package com.shadowtradingplatform.trade.pay.domain.pojo;

import lombok.Data;

/**
 * 微信支付 JSAPI 买家信息.
 */
@Data
public class WxPayPayer {

    /**
     * 买家 openid (JSAPI 场景必填).
     */
    private String openid;
}
