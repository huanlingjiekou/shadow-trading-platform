package com.shadowtradingplatform.trade.pay.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易状态枚举.
 */
@Getter
@AllArgsConstructor
public enum TradeStatusEnum {

    /** 待支付 */
    NOTPAY("NOTPAY", "待支付"),

    /** 已支付 */
    SUCCESS("SUCCESS", "支付成功"),

    /** 已关闭 */
    CLOSED("CLOSED", "已关闭"),

    /** 已退款 */
    REFUND("REFUND", "已退款"),

    /** 支付失败 */
    PAYERROR("PAYERROR", "支付失败");

    private final String code;

    private final String desc;
}
