package com.shadowtradingplatform.trade.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shadowtradingplatform.trade.domain.po.Coupon;
import com.shadowtradingplatform.trade.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠券定时任务.
 *
 * <p>职责：</p>
 * <ul>
 *   <li>每天凌晨 2:00 将次日上场的优惠券预加载到 Redis（库存 + 布隆过滤器）</li>
 *   <li>启动时立即预热一次，保障券上线即可高并发领取</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponScheduleTask {

    private static final String DISTRIBUTED_LOCK_KEY = "stp:coupon:schedule:lock";

    private final CouponService couponService;
    private final RedissonClient redissonClient;

    /**
     * 每天凌晨 2:00 预加载次日上场的优惠券到 Redis.
     *
     * <p>使用 Redisson 分布式锁避免多实例重复执行。</p>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void preloadTomorrowCoupons() {
        RLock lock = redissonClient.getLock(DISTRIBUTED_LOCK_KEY);
        boolean locked = false;
        try {
            locked = lock.tryLock();
            if (!locked) {
                log.debug("优惠券预加载：另一个实例正在执行，跳过");
                return;
            }
            log.info("========== 开始预加载次日优惠券到 Redis ==========");
            doPreload();
            log.info("========== 优惠券预加载完成 ==========");
        } catch (Exception e) {
            log.error("优惠券预加载异常：{}", e.getMessage(), e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 启动时立即预热一次，保障券上线即可领取.
     */
    @jakarta.annotation.PostConstruct
    public void onStartup() {
        log.info("========== 启动预热：加载当前有效优惠券到 Redis ==========");
        RLock lock = redissonClient.getLock(DISTRIBUTED_LOCK_KEY);
        boolean locked = false;
        try {
            locked = lock.tryLock();
            if (!locked) {
                log.warn("启动预热：获取分布式锁失败，跳过");
                return;
            }
            doPreload();
            log.info("========== 启动预热完成 ==========");
        } catch (Exception e) {
            log.error("启动预热异常：{}", e.getMessage(), e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行预加载逻辑：查询当前有效的券（含当日和次日上场的）.
     */
    private void doPreload() {
        LocalDateTime now = LocalDateTime.now();

        // 查询：当前有效券（status=0 且 valid_start <= now <= valid_end）
        // 以及次日即将上场的券（status=0 且 valid_start 在未来 24 小时内）
        List<Coupon> coupons = couponService.list(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getStatus, 0)
                        .le(Coupon::getValidStart, now.plusDays(1))
                        .ge(Coupon::getValidEnd, now.minusDays(1)));

        if (coupons.isEmpty()) {
            log.info("没有需要预加载的优惠券");
            return;
        }

        List<Long> couponIds = coupons.stream()
                .map(Coupon::getId)
                .collect(Collectors.toList());

        log.info("预加载优惠券数量：{}", couponIds.size());
        couponService.preloadToRedis(couponIds);
    }
}
