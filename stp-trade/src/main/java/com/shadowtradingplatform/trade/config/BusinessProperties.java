package com.shadowtradingplatform.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务级配置属性.
 *
 * <p>绑定 application.yml 中 {@code business.*} 配置项。
 * 敏感字段（RSA 私钥）通过环境变量注入。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "business")
public class BusinessProperties {

    /**
     * 业务 RSA 私钥 (PKCS8 格式，Base64 字符串，含头尾或纯 Base64 均可).
     * <p>用于解密登录请求中的 encryptedKey 字段。</p>
     * <p>来源：环境变量 {@code BUSINESS_PRIVATE_KEY}</p>
     */
    private String privateKey;
}
