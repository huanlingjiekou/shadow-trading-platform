package com.shadowtradingplatform.trade.domain;


import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserCoupon {
    @Schema(description="主键")
    private Long id;
    @Schema(description="用户ID")
    private Long user_id;
    @Schema(description="优惠券ID")
    private Long coupon_id;
    @Schema(description="状态：0=未使用 1=已使用 2=已过期")
    private Integer status;
    @Schema(description="领取时间")
    private LocalDateTime claim_time;
    @Schema(description="使用时间")
    private LocalDateTime use_time;
}
