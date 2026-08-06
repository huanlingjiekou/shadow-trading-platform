package com.shadowtradingplatform.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置属性.
 *
 * <p>绑定 application.yml 中 {@code wechat.pay.*} 配置项，
 * 其中 {@code mockEnabled} 通过环境变量 {@code MOCK_ENABLED} 控制。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayProperties {

    /** 商户号 */
    private String merchantId;

    /** 商户API证书序列号 */
    private String merchantSerialNumber;

    /** 商户API V3密钥 (32位字符，用于AES-GCM加解密) */
    private String apiV3Key;

    /** 商户私钥文件路径 (apiclient_key.pem) */
    private String privateKeyPath;

    /** 微信支付平台证书路径 (用于验签) */
    private String certPath;

    /** 支付通知地址 */
    private String notifyUrl;

    /**
     * Mock 模式开关.
     * <p>true: 执行真实签名与加解密逻辑，但不发送网络请求，返回模拟数据</p>
     * <p>false: 走真实微信支付接口</p>
     */
    private boolean mockEnabled = false;
}
