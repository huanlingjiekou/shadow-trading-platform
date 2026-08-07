package com.shadowtradingplatform.trade.pay.domain.dto;

import com.shadowtradingplatform.trade.pay.domain.enums.PayChannelEnum;
import com.shadowtradingplatform.trade.pay.domain.enums.TradeStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回调通知数据传输对象.
 *
 * <p>用于 Controller 接收回调报文后传递给 Service 层处理。
 * 统一支付宝与微信的回调字段，屏蔽渠道差异。</p>
 */
@Data
@Builder
public class PayNotifyDTO {

    /** 支付渠道 */
    private PayChannelEnum payChannel;

    /** 商户订单号 */
    private String outTradeNo;

    /** 第三方平台交易号 */
    private String tradeNo;

    /** 订单金额 (元) */
    private BigDecimal totalAmount;

    /** 订单金额 (分) */
    private Integer totalFee;

    /** 买家标识 */
    private String buyerId;

    /** 交易状态 */
    private TradeStatusEnum tradeStatus;

    /** 支付完成时间 */
    private LocalDateTime successTime;

    /** 原始回调报文 (用于日志审计) */
    private String rawNotifyData;
}
