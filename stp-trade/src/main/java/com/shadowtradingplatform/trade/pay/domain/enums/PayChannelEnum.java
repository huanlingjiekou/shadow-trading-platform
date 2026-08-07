package com.shadowtradingplatform.trade.pay.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付渠道枚举.
 */
@Getter
@AllArgsConstructor
public enum PayChannelEnum {

    /** 支付宝 */
    ALIPAY("ALIPAY", "支付宝"),

    /** 微信支付 */
    WXPAY("WXPAY", "微信支付");

    private final String code;

    private final String desc;

    public static PayChannelEnum fromCode(String code) {
        for (PayChannelEnum channel : values()) {
            if (channel.code.equalsIgnoreCase(code)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("未知支付渠道: " + code);
    }
}
