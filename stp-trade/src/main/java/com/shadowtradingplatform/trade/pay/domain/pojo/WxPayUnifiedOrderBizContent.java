package com.shadowtradingplatform.trade.pay.domain.pojo;

import lombok.Data;

/**
 * 微信支付统一下单请求体 (Api-v3).
 *
 * <p>字段命名严格遵循微信支付 APIv3 规范（snake_case），
 * 由全局 Jackson {@code PropertyNamingStrategy.SNAKE_CASE} 策略自动处理序列化。</p>
 */
@Data
public class WxPayUnifiedOrderBizContent {

    /**
     * 公众号/小程序/APPID.
     */
    private String appid;

    /**
     * 商户号.
     */
    private String mchid;

    /**
     * 商品描述.
     */
    private String description;

    /**
     * 商户订单号.
     */
    private String outTradeNo;

    /**
     * 支付通知地址.
     */
    private String notifyUrl;

    /**
     * 订单金额信息.
     */
    private WxPayAmount amount;

    /**
     * JSAPI 场景: 买家 openid.
     */
    private WxPayPayer payer;

    /**
     * H5 场景: 场景信息.
     */
    private WxPaySceneInfo sceneInfo;
}
