package com.shadowtradingplatform.trade.wechat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回调通知中的加密资源容器.
 *
 * <p>对应微信回调报文 {@code resource} 字段，
 * 需使用 AES-256-GCM 解密后得到 {@link NotifyDecryptResult}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyResource {

    /** 加密算法类型 (AES_256_GCM) */
    private String algorithm;

    /** Base64 编码的密文 */
    private String ciphertext;

    /** 附加数据 */
    @JsonProperty("associated_data")
    private String associatedData;

    /** 随机串 (12字节) */
    private String nonce;
}
