package com.shadowtradingplatform.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置属性.
 *
 * <p>绑定 application.yml 中 {@code wechat.pay.*} 配置项。
 * 敏感字段（私钥、证书、APIv3密钥）通过环境变量注入。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.pay")
public class WxPayProperties {

    /** 应用ID (公众号/小程序/APP) */
    private String appId;

    /** 商户号 */
    private String mchId;

    /** 商户API证书序列号 */
    private String mchSerialNo;

    /** 商户API V3密钥 (32位字符，用于AES-GCM加解密) */
    private String apiV3Key;

    /** 商户私钥文件路径 (apiclient_key.pem) */
    private String privateKeyPath;

    /** 商户证书文件路径 (apiclient_cert.pem) */
    private String certPath;

    /** 微信支付平台证书路径 (wxp_platform_cert.pem, 用于验签) */
    private String platformCertPath;

    /** 支付通知地址 */
    private String notifyUrl;

    /**
     * Mock 模式开关.
     * <p>true: 不发送真实网络请求，返回模拟 prepay_id</p>
     * <p>false: 走真实微信支付接口</p>
     */
    private boolean mockEnabled = true;
}
