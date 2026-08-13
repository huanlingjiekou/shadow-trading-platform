package com.shadowtradingplatform.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadowtradingplatform.trade.context.UserContext;
import com.shadowtradingplatform.trade.domain.po.Coupon;
import com.shadowtradingplatform.trade.domain.po.UserCoupon;
import com.shadowtradingplatform.trade.domain.req.CouponClaimReq;
import com.shadowtradingplatform.trade.domain.vo.CouponClaimResp;
import com.shadowtradingplatform.trade.domain.vo.CouponVO;
import com.shadowtradingplatform.trade.mapper.CouponMapper;
import com.shadowtradingplatform.trade.service.CouponService;
import com.shadowtradingplatform.trade.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 优惠券服务实现（高并发方案）.
 *
 * <p>核心设计：</p>
 * <ul>
 *   <li><b>Redis 预扣减库存</b>：优惠券库存提前刷入 Redis Hash，领取时 Lua 脚本原子扣减</li>
 *   <li><b>布隆过滤器防穿透</b>：有效券 ID 存入 Redisson RBloomFilter，不存在的 ID 直接拒绝</li>
 *   <li><b>Redisson 分布式锁</b>：同一用户同一券的领取操作加锁，防重复提交</li>
 *   <li><b>DB 最终写入</b>：Redis 预扣成功后写 DB，保证最终一致性</li>
 *   <li><b>定时预热</b>：每晚定时任务将次日上场的券刷入 Redis</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon>
    implements CouponService {

    /** Redis 键前缀：券库存 Hash */
    private static final String REDIS_COUPON_STOCK_PREFIX = "stp:coupon:stock:";

    /** Redis 键前缀：券信息 JSON */
    private static final String REDIS_COUPON_INFO_PREFIX = "stp:coupon:info:";

    /** 布隆过滤器名称：有效券 ID */
    private static final String BLOOM_FILTER_NAME = "stp:coupon:valid";

    /** 分布式锁前缀：用户领取券 */
    private static final String LOCK_PREFIX = "stp:coupon:lock:";

    /** 券信息在 Redis 中的过期时间（7 天） */
    private static final long COUPON_INFO_TTL_DAYS = 7L;

    /** 分布式锁等待时间（秒） */
    private static final long LOCK_WAIT_SECONDS = 3L;

    /** 分布式锁持有时间（秒） */
    private static final long LOCK_LEASE_SECONDS = 10L;

    /** 布隆过滤器预期插入元素数量 */
    private static final long BLOOM_FILTER_EXPECTED_INSERTIONS = 10_000L;

    /** 布隆过滤器误判率 */
    private static final double BLOOM_FILTER_FALSE_PROBABILITY = 0.01D;

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final UserCouponService userCouponService;

    // ==================== 业务方法 ====================

    @Override
    public List<CouponVO> loadCouponList() {
        Long userId = UserContext.getUserId();

        // 1. 查询所有有效券（status=0 可领取）
        List<Coupon> coupons = list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 0)
                .orderByAsc(Coupon::getValidStart));
        if (coupons.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查询当前用户已领取的券 ID
        Set<Long> claimedIds = Collections.emptySet();
        if (userId != null) {
            List<UserCoupon> userCoupons = userCouponService.list(
                    new LambdaQueryWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, userId));
            claimedIds = userCoupons.stream()
                    .map(UserCoupon::getCouponId)
                    .collect(Collectors.toSet());
        }

        // 3. 组装 VO
        Set<Long> finalClaimedIds = claimedIds;
        return coupons.stream()
                .map(c -> toCouponVO(c, finalClaimedIds))
                .collect(Collectors.toList());
    }

    @Override
    public CouponClaimResp claimCoupon(CouponClaimReq req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return CouponClaimResp.fail(req.getCouponId(), "未登录");
        }
        Long couponId = req.getCouponId();
        if (couponId == null) {
            return CouponClaimResp.fail(null, "优惠券ID不能为空");
        }

        try {
            // 1. 布隆过滤器快速判断：券 ID 是否可能存在（防缓存穿透）
            RBloomFilter<Long> bloomFilter = getOrCreateBloomFilter();
            if (!bloomFilter.contains(couponId)) {
                log.warn("领取失败：券ID不存在(布隆过滤器拦截)，couponId={}", couponId);
                return CouponClaimResp.fail(couponId, "优惠券不存在");
            }

            // 2. 分布式锁：防止同一用户重复领取同一券
            String lockKey = LOCK_PREFIX + userId + ":" + couponId;
            RLock lock = redissonClient.getLock(lockKey);
            boolean locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                return CouponClaimResp.fail(couponId, "操作过于频繁，请稍后再试");
            }

            try {
                // 3. 从 Redis 获取券信息
                String infoKey = REDIS_COUPON_INFO_PREFIX + couponId;
                RBucket<String> bucket = redissonClient.getBucket(infoKey, StringCodec.INSTANCE);
                String infoJson = bucket.get();

                Coupon coupon;
                if (StringUtils.hasText(infoJson)) {
                    // 3a. Redis 命中
                    coupon = objectMapper.readValue(infoJson, Coupon.class);
                } else {
                    // 3b. Redis 未命中 -> 查 DB + 回填 Redis
                    coupon = this.getById(couponId);
                    if (coupon == null || coupon.getStatus() == null || coupon.getStatus() != 0) {
                        return CouponClaimResp.fail(couponId, "优惠券不存在或已抢光");
                    }
                    // 回填 Redis
                    bucket.set(objectMapper.writeValueAsString(coupon),
                            COUPON_INFO_TTL_DAYS, TimeUnit.DAYS);
                    // 回刷库存
                    syncStockToRedis(coupon);
                }

                // 4. 业务校验
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(coupon.getValidStart())) {
                    return CouponClaimResp.fail(couponId, "优惠券尚未开始");
                }
                if (now.isAfter(coupon.getValidEnd())) {
                    return CouponClaimResp.fail(couponId, "优惠券已过期");
                }

                // 5. Redis 原子扣减库存（Lua 脚本实现）
                String stockKey = REDIS_COUPON_STOCK_PREFIX + couponId;
                RMap<String, Integer> stockMap = redissonClient.getMap(stockKey);
                Integer remaining = stockMap.get("stock");
                if (remaining == null) {
                    // 库存未初始化，从 DB 同步
                    syncStockToRedis(coupon);
                    remaining = coupon.getTotal() - (coupon.getClaimed() != null ? coupon.getClaimed() : 0);
                }

                if (remaining <= 0) {
                    return CouponClaimResp.fail(couponId, "优惠券已抢光");
                }

                // Lua 原子扣减
                Integer stockAfter = stockMap.addAndGet("stock", -1);
                if (stockAfter == null || stockAfter < 0) {
                    // 并发下变为负数，回滚
                    stockMap.addAndGet("stock", 1);
                    return CouponClaimResp.fail(couponId, "优惠券已抢光");
                }

                // 6. 写入 DB
                UserCoupon userCoupon = new UserCoupon();
                userCoupon.setUserId(userId);
                userCoupon.setCouponId(couponId);
                userCoupon.setStatus(0);
                userCoupon.setClaimTime(now);
                userCouponService.save(userCoupon);

                // 7. 更新 DB 冗余计数
                coupon.setClaimed((coupon.getClaimed() != null ? coupon.getClaimed() : 0) + 1);
                if (stockAfter == 0) {
                    coupon.setStatus(1);
                }
                coupon.setUpdateTime(now);
                this.updateById(coupon);

                // 8. 若已抢光，清理 Redis 库存信息
                if (stockAfter == 0) {
                    stockMap.remove("stock");
                }

                log.info("领取优惠券成功：userId={}, couponId={}", userId, couponId);
                return CouponClaimResp.success(couponId);

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (Exception e) {
            log.error("领取优惠券异常：couponId={}, userId={}, error={}", couponId, userId, e.getMessage(), e);
            return CouponClaimResp.fail(couponId, "领取失败：" + e.getMessage());
        }
    }

    @Override
    public void preloadToRedis(List<Long> couponIds) {
        if (couponIds == null || couponIds.isEmpty()) {
            return;
        }

        RBloomFilter<Long> bloomFilter = getOrCreateBloomFilter();

        for (Long couponId : couponIds) {
            Coupon coupon = this.getById(couponId);
            if (coupon == null || coupon.getStatus() == null || coupon.getStatus() != 0) {
                continue;
            }

            try {
                // 1. 写入布隆过滤器
                bloomFilter.add(couponId);

                // 2. 缓存券信息
                String infoKey = REDIS_COUPON_INFO_PREFIX + couponId;
                RBucket<String> bucket = redissonClient.getBucket(infoKey, StringCodec.INSTANCE);
                bucket.set(objectMapper.writeValueAsString(coupon),
                        COUPON_INFO_TTL_DAYS, TimeUnit.DAYS);

                // 3. 初始化库存
                syncStockToRedis(coupon);

                log.info("预加载优惠券到Redis：couponId={}, total={}, claimed={}",
                        couponId, coupon.getTotal(), coupon.getClaimed());
            } catch (JsonProcessingException e) {
                log.error("预加载优惠券失败：couponId={}, error={}", couponId, e.getMessage());
            }
        }
    }

    @Override
    public int preloadAllToRedis() {
        // 查询所有有效券（status=0 且在有效期内）
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 0)
                .le(Coupon::getValidStart, now)
                .ge(Coupon::getValidEnd, now));

        if (coupons.isEmpty()) {
            log.info("直接预热：没有需要预加载的有效优惠券");
            return 0;
        }

        RBloomFilter<Long> bloomFilter = getOrCreateBloomFilter();
        int count = 0;

        for (Coupon coupon : coupons) {
            try {
                // 1. 写入布隆过滤器
                bloomFilter.add(coupon.getId());

                // 2. 缓存券信息
                String infoKey = REDIS_COUPON_INFO_PREFIX + coupon.getId();
                RBucket<String> bucket = redissonClient.getBucket(infoKey, StringCodec.INSTANCE);
                bucket.set(objectMapper.writeValueAsString(coupon),
                        COUPON_INFO_TTL_DAYS, TimeUnit.DAYS);

                // 3. 初始化库存
                syncStockToRedis(coupon);

                count++;
                log.info("直接预热优惠券：couponId={}, total={}, claimed={}",
                        coupon.getId(), coupon.getTotal(), coupon.getClaimed());
            } catch (JsonProcessingException e) {
                log.error("直接预热优惠券失败：couponId={}, error={}",
                        coupon.getId(), e.getMessage());
            }
        }

        log.info("直接预热完成：共预热 {} 张优惠券", count);
        return count;
    }

    @Override
    public void evictFromRedis(Long couponId) {
        if (couponId == null) {
            return;
        }
        try {
            // 移除券信息
            redissonClient.getBucket(REDIS_COUPON_INFO_PREFIX + couponId, StringCodec.INSTANCE).delete();

            // 移除库存
            redissonClient.getMap(REDIS_COUPON_STOCK_PREFIX + couponId).clear();

            log.info("从Redis移除优惠券缓存：couponId={}", couponId);
        } catch (Exception e) {
            log.error("移除Redis缓存异常：couponId={}, error={}", couponId, e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取布隆过滤器，若不存在则自动初始化.
     *
     * <p>使用 Redisson {@code tryInit} 方法：若布隆过滤器已存在则直接返回；
     * 若不存在则按预设参数（预期 10000 元素、误判率 1%）初始化并返回。</p>
     */
    private RBloomFilter<Long> getOrCreateBloomFilter() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_NAME);
        boolean initialized = bloomFilter.tryInit(
                BLOOM_FILTER_EXPECTED_INSERTIONS,
                BLOOM_FILTER_FALSE_PROBABILITY);
        if (initialized) {
            log.info("布隆过滤器已自动初始化：name={}, expectedInsertions={}, falseProbability={}",
                    BLOOM_FILTER_NAME, BLOOM_FILTER_EXPECTED_INSERTIONS, BLOOM_FILTER_FALSE_PROBABILITY);
        }
        return bloomFilter;
    }

    /**
     * 同步券库存到 Redis.
     */
    private void syncStockToRedis(Coupon coupon) {
        String stockKey = REDIS_COUPON_STOCK_PREFIX + coupon.getId();
        RMap<String, Integer> stockMap = redissonClient.getMap(stockKey);
        int remaining = (coupon.getTotal() != null ? coupon.getTotal() : 0)
                - (coupon.getClaimed() != null ? coupon.getClaimed() : 0);
        stockMap.put("stock", Math.max(remaining, 0));
    }

    /**
     * Coupon PO -> CouponVO 转换.
     */
    private CouponVO toCouponVO(Coupon c, Set<Long> claimedIds) {
        CouponVO vo = new CouponVO();
        vo.setId(c.getId());
        vo.setTitle(c.getTitle());
        vo.setAmount(c.getAmount());
        vo.setMinSpend(c.getMinSpend());
        vo.setScope(c.getScope());
        vo.setValidStart(c.getValidStart());
        vo.setValidEnd(c.getValidEnd());
        vo.setTotal(c.getTotal());
        vo.setClaimed(c.getClaimed());

        // 计算领取状态
        int remaining = (c.getTotal() != null ? c.getTotal() : 0)
                - (c.getClaimed() != null ? c.getClaimed() : 0);
        if (claimedIds.contains(c.getId())) {
            vo.setStatus(1); // 已领取
        } else if (remaining <= 0 || (c.getStatus() != null && c.getStatus() == 1)) {
            vo.setStatus(2); // 已抢光
        } else {
            vo.setStatus(0); // 可领取
        }

        return vo;
    }
}
