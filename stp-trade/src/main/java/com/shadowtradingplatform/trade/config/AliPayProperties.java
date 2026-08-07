package com.shadowtradingplatform.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝配置属性.
 *
 * <p>绑定 application.yml 中 {@code alipay.*} 配置项。
 * 敏感字段（私钥、公钥）通过环境变量注入。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AliPayProperties {

    /** 应用ID */
    private String appId;

    /** 应用私钥 (PKCS8 字符串) */
    private String privateKey;

    /** 支付宝公钥 (用于验签) */
    private String alipayPublicKey;

    /** 网关地址 */
    private String gatewayUrl;

    /** 编码 */
    private String charset;

    /** 数据格式 */
    private String format;

    /** 签名类型 */
    private String signType;

    /** 异步通知回调地址 */
    private String notifyUrl;

    /** 同步返回地址 */
    private String returnUrl;

    /**
     * Mock 模式开关.
     * <p>true: 不发送真实网络请求，返回模拟支付链接/二维码</p>
     * <p>false: 走真实支付宝接口</p>
     */
    private boolean mockEnabled = true;
}
