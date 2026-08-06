package com.shadowtradingplatform.trade.wechat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信支付回调通知原始参数.
 *
 * <p>对应微信推送的报文结构（已加密），需经过验签 + AES-GCM 解密后
 * 得到 {@link NotifyResource}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyParams {

    /** 微信支付订单号 */
    @JsonProperty("transaction_id")
    private String transactionId;

    /** 商户订单号 */
    @JsonProperty("out_trade_no")
    private String outTradeNo;

    /** 通知数据类型 */
    @JsonProperty("event_type")
    private String eventType;

    /** 回调摘要 */
    @JsonProperty("summary")
    private String summary;

    /** 资源 (含密文) */
    private NotifyResource resource;
}
