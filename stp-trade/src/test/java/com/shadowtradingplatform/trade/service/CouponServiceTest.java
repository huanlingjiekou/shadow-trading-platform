package com.shadowtradingplatform.trade.service;

import com.shadowtradingplatform.trade.StpTradeApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 优惠券预热测试.
 *
 * <p>验证 {@link CouponService#preloadAllToRedis()} 直接预热方法是否正常工作。</p>
 */
@Slf4j
@SpringBootTest(classes = StpTradeApplication.class)
class CouponServiceTest {

    @Autowired
    private CouponService couponService;

    /**
     * 测试直接预热所有有效优惠券到 Redis.
     */
    @Test
    void testPreloadAllToRedis() {
        int count = couponService.preloadAllToRedis();
        log.info("预热优惠券数量：{}", count);
        assertTrue(count >= 0, "预热数量应大于等于 0");
    }
}
