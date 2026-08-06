package com.shadowtradingplatform.trade.wechat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回调通知解密后的交易结果.
 *
 * <p>由 AES-256-GCM 解密 {@link NotifyResource} 后得到。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyDecryptResult {

    /** 商户号 */
    private String mchid;

    /** 应用ID */
    private String appid;

    /** 商户订单号 */
    @JsonProperty("out_trade_no")
    private String outTradeNo;

    /** 微信支付订单号 */
    @JsonProperty("transaction_id")
    private String transactionId;

    /** 交易类型 */
    @JsonProperty("trade_type")
    private String tradeType;

    /** 交易状态 */
    @JsonProperty("trade_state")
    private String tradeState;

    /** 交易状态描述 */
    @JsonProperty("trade_state_desc")
    private String tradeStateDesc;

    /** 银行类型 */
    @JsonProperty("bank_type")
    private String bankType;

    /** 附加数据 */
    private String attach;

    /** 支付完成时间 */
    @JsonProperty("success_time")
    private String successTime;

    /** 支付者信息 */
    private Payer payer;

    /** 订单金额信息 */
    private Amount amount;

    /**
     * 支付者信息.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payer {
        /** 用户标识 */
        private String openid;

        /** 用户ID (服务商模式) */
        @JsonProperty("user_id")
        private String userId;
    }
}
