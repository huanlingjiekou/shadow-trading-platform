package com.shadowtradingplatform.trade.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 领取优惠券响应 VO.
 */
@Data
@Schema(description = "领取优惠券响应")
public class CouponClaimResp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "优惠券ID", example = "1")
    private Long couponId;

    @Schema(description = "是否领取成功", example = "true")
    private Boolean success;

    @Schema(description = "结果消息", example = "领取成功")
    private String message;

    public static CouponClaimResp success(Long couponId) {
        CouponClaimResp resp = new CouponClaimResp();
        resp.setCouponId(couponId);
        resp.setSuccess(true);
        resp.setMessage("领取成功");
        return resp;
    }

    public static CouponClaimResp fail(Long couponId, String message) {
        CouponClaimResp resp = new CouponClaimResp();
        resp.setCouponId(couponId);
        resp.setSuccess(false);
        resp.setMessage(message);
        return resp;
    }
}
