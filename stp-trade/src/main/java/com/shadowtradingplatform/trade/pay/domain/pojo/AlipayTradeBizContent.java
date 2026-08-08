package com.shadowtradingplatform.trade.pay.domain.pojo;

import lombok.Data;

/**
 * 支付宝统一下单 bizContent 请求体.
 *
 * <p>字段命名严格遵循支付宝 API 规范（snake_case），
 * 由全局 Jackson {@code PropertyNamingStrategy.SNAKE_CASE} 策略自动处理序列化。</p>
 *
 * <p>不同交易类型需要的字段:</p>
 * <ul>
 *   <li>WEB (PC网站支付): product_code=FAST_INSTANT_TRADE_PAY</li>
 *   <li>WAP (手机网站支付): product_code=QUICK_WAP_WAY</li>
 *   <li>SCAN (当面付扫码): 无 product_code，可选 buyer_id</li>
 *   <li>APP (APP支付): 无 product_code</li>
 * </ul>
 */
@Data
public class AlipayTradeBizContent {

    /**
     * 商户订单号.
     */
    private String outTradeNo;

    /**
     * 订单标题.
     */
    private String subject;

    /**
     * 订单金额 (元，字符串格式，保留两位小数).
     */
    private String totalAmount;

    /**
     * 商品描述 (可选).
     */
    private String body;

    /**
     * 买家支付宝用户ID (仅当面付 SCAN 场景使用).
     */
    private String buyerId;

    /**
     * 超时时间 (如 30m).
     */
    private String timeoutExpress;

    /**
     * 产品编码 (WEB/WAP 场景必填).
     * FAST_INSTANT_TRADE_PAY - PC网站支付
     * QUICK_WAP_WAY - 手机网站支付
     */
    private String productCode;
}
