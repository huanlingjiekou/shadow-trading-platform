package com.shadowtradingplatform.trade.wechat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信支付统一下单请求 (JSAPI / Native).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrepayRequest {

    /** 应用ID */
    private String appid;

    /** 商户号 */
    private String mchid;

    /** 商品描述 */
    private String description;

    /** 商户订单号 */
    private String outTradeNo;

    /** 支付通知地址 */
    private String notifyUrl;

    /** 订单金额 */
    private Amount amount;

    /** 支付者 openid (JSAPI 下单必填) */
    private String openid;
}
