package com.shadowtradingplatform.trade.wechat.crypto;

import com.shadowtradingplatform.trade.wechat.WechatPayConstants;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 微信支付 AES-256-GCM 解密器.
 *
 * <p>实现真实的 AES-GCM 解密逻辑，用于解密回调通知中
 * {@code resource.ciphertext} 字段的加密数据。</p>
 *
 * <p>解密参数：</p>
 * <ul>
 *   <li>Key: APIv3 密钥 (32 字节)</li>
 *   <li>Nonce: 12 字节随机串</li>
 *   <li>AAD: associated_data 附加数据</li>
 *   <li>Tag: 128 位认证标签</li>
 * </ul>
 */
@Slf4j
public class AesGcmDecryptor {

    private final SecretKeySpec secretKey;

    public AesGcmDecryptor(String apiV3Key) {
        byte[] keyBytes = apiV3Key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "APIv3 密钥长度必须为 32 字节 (AES-256), 当前: " + keyBytes.length + " 字节");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 解密回调报文中的加密资源.
     *
     * @param associatedData 附加数据 (resource.associated_data)
     * @param nonce          随机串 (resource.nonce)
     * @param ciphertext     Base64 编码的密文 (resource.ciphertext)
     * @return 解密后的明文 JSON
     */
    public String decrypt(String associatedData, String nonce, String ciphertext) {
        try {
            byte[] nonceBytes = nonce.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);
            byte[] aad = (associatedData == null) ? null : associatedData.getBytes(StandardCharsets.UTF_8);

            Cipher cipher = Cipher.getInstance(WechatPayConstants.AES_GCM_NO_PADDING);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(
                    WechatPayConstants.GCM_TAG_LENGTH_BITS, nonceBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            if (aad != null) {
                cipher.updateAAD(aad);
            }
            byte[] decrypted = cipher.doFinal(ciphertextBytes);
            String plaintext = new String(decrypted, StandardCharsets.UTF_8);
            log.debug("AES-GCM 解密成功, 明文长度={}", plaintext.length());
            return plaintext;
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM 解密失败", e);
        }
    }
}
