package com.shadowtradingplatform.trade.pay.domain.vo;

import com.shadowtradingplatform.trade.pay.domain.enums.PayChannelEnum;
import com.shadowtradingplatform.trade.pay.domain.enums.TradeStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回调处理结果视图对象.
 *
 * <p>返回给支付平台作为响应（微信要求返回 SUCCESS/FAIL，支付宝要求返回 success/fail）。</p>
 */
@Data
@Builder
@Schema(description = "支付回调处理结果")
public class PayNotifyResultVO {

    @Schema(description = "返回给支付平台的状态字符串 (微信: SUCCESS/FAIL, 支付宝: success/fail)")
    private String responseCode;

    @Schema(description = "返回给支付平台的消息")
    private String responseMessage;

    @Schema(description = "支付渠道")
    private PayChannelEnum payChannel;

    @Schema(description = "商户订单号")
    private String outTradeNo;

    @Schema(description = "第三方交易号")
    private String tradeNo;

    @Schema(description = "订单金额 (元)")
    private BigDecimal totalAmount;

    @Schema(description = "交易状态")
    private TradeStatusEnum tradeStatus;

    @Schema(description = "支付完成时间")
    private LocalDateTime successTime;
}
