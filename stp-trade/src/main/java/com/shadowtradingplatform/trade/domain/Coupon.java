package com.shadowtradingplatform.trade.domain;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class Coupon {
    @Schema(description="优惠券ID")
    private Long id;
    @Schema(description="券名称")
    private String title;
    @Schema(description="满减金额")
    private BigDecimal amount;
    @Schema(description="使用门槛（满 min_spend 可用）")
    private BigDecimal min_spend;
    @Schema(description="适用范围描述")
    private String scope;
    @Schema(description="有效期开始")
    private LocalDate valid_start;
    @Schema(description="有效期结束")
    private LocalDate valid_end;
    @Schema(description="发行总量")
    private Integer total;
    @Schema(description="已领取数（冗余计数）")
    private Integer claimed;
    @Schema(description="状态：0=可领取 1=已抢光")
    private Integer status;
    @Schema(description="创建时间")
    private LocalDateTime create_time;
    @Schema(description="更新时间")
    private LocalDateTime update_time;
}
