package com.shadowtradingplatform.trade.controller;

import com.shadowtradingplatform.trade.common.R;
import com.shadowtradingplatform.trade.context.UserContext;
import com.shadowtradingplatform.trade.domain.req.CouponClaimReq;
import com.shadowtradingplatform.trade.domain.vo.CouponClaimResp;
import com.shadowtradingplatform.trade.domain.vo.CouponVO;
import com.shadowtradingplatform.trade.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优惠券控制器.
 *
 * <p>高并发领取方案：
 * Redis 预扣减 + 布隆过滤器防穿透 + Redisson 分布式锁 + 定时预热</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券模块", description = "优惠券列表查询、领取（高并发）")
public class CouponController {

    private final CouponService couponService;

    /**
     * 查询优惠券列表（含当前用户领取状态）.
     */
    @GetMapping("/coupon/list")
    @Operation(summary = "查询优惠券列表")
    public R<List<CouponVO>> list() {
        return R.success(couponService.loadCouponList());
    }

    /**
     * 领取优惠券（高并发）.
     */
    @PostMapping("/coupon/claim")
    @Operation(summary = "领取优惠券（高并发）")
    public R<CouponClaimResp> claim(@Valid @RequestBody CouponClaimReq req) {
        if (!UserContext.isLogin()) {
            CouponClaimResp fail = CouponClaimResp.fail(req.getCouponId(), "未登录");
            return R.success(fail);
        }
        CouponClaimResp resp = couponService.claimCoupon(req);
        if (resp.getSuccess() != null && resp.getSuccess()) {
            return R.success("领取成功", resp);
        }
        return R.fail(resp.getMessage());
    }
}
