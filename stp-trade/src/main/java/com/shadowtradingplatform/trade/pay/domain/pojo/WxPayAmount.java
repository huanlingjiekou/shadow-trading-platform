package com.shadowtradingplatform.trade.pay.domain.pojo;

import lombok.Data;

/**
 * 微信支付订单金额信息.
 *
 * <p>Api-v3 规范: 金额单位为分，字符串格式。</p>
 */
@Data
public class WxPayAmount {

    /**
     * 金额 (分).
     */
    private Integer total;

    /**
     * 货币类型.
     */
    private String currency;
}
