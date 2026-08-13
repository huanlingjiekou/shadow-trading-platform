package com.shadowtradingplatform.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shadowtradingplatform.trade.domain.po.Coupon;
import com.shadowtradingplatform.trade.domain.req.CouponClaimReq;
import com.shadowtradingplatform.trade.domain.vo.CouponClaimResp;
import com.shadowtradingplatform.trade.domain.vo.CouponVO;

import java.util.List;

/**
* @author huanlingjiekou
* @description 针对表【coupon(优惠券表)】的数据库操作Service
* @createDate 2026-08-09 16:08:29
*/
public interface CouponService extends IService<Coupon> {

    /**
     * 查询优惠券列表（含当前用户领取状态）.
     */
    List<CouponVO> loadCouponList();

    /**
     * 领取优惠券（高并发方案：Redis 预扣 + 布隆过滤器 + 分布式锁 + Lua 脚本）.
     */
    CouponClaimResp claimCoupon(CouponClaimReq req);

    /**
     * 将指定优惠券预加载到 Redis（高并发场景使用）.
     *
     * @param couponIds 优惠券 ID 集合
     */
    void preloadToRedis(List<Long> couponIds);

    /**
     * 从 Redis 中移除优惠券缓存.
     */
    void evictFromRedis(Long couponId);
}
