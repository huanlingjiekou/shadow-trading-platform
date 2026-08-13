package com.shadowtradingplatform.trade.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券列表项 VO.
 */
@Data
@Schema(description = "优惠券列表项")
public class CouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "优惠券ID", example = "1")
    private Long id;

    @Schema(description = "券标题", example = "满100减10")
    private String title;

    @Schema(description = "满减金额", example = "10.00")
    private BigDecimal amount;

    @Schema(description = "门槛金额", example = "100.00")
    private BigDecimal minSpend;

    @Schema(description = "适用范围描述", example = "全场通用")
    private String scope;

    @Schema(description = "有效期开始", example = "2026-08-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validStart;

    @Schema(description = "有效期结束", example = "2026-08-31 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validEnd;

    @Schema(description = "总发行量", example = "1000")
    private Integer total;

    @Schema(description = "已领取数量", example = "520")
    private Integer claimed;

    @Schema(description = "当前用户领取状态: 0=未领取, 1=已领取, 2=已抢光", example = "0")
    private Integer status;
}
