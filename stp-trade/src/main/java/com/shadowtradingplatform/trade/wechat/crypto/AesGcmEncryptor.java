package com.shadowtradingplatform.trade.wechat.crypto;

import com.shadowtradingplatform.trade.wechat.WechatPayConstants;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 微信支付 AES-256-GCM 加密器.
 *
 * <p>实现真实的 AES-GCM 加密逻辑。主要用于 Mock 模式下构造
 * 模拟的加密回调报文，配合 {@link AesGcmDecryptor} 进行解密测试。</p>
 */
@Slf4j
public class AesGcmEncryptor {

    private final SecretKeySpec secretKey;

    public AesGcmEncryptor(String apiV3Key) {
        byte[] keyBytes = apiV3Key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "APIv3 密钥长度必须为 32 字节 (AES-256), 当前: " + keyBytes.length + " 字节");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密数据 (用于 Mock 模式构造假回调报文).
     *
     * @param plaintext      明文 JSON
     * @param associatedData 附加数据
     * @param nonce          12 字节随机串
     * @return Base64 编码的密文 (含认证标签)
     */
    public String encrypt(String plaintext, String associatedData, byte[] nonce) {
        try {
            byte[] aad = (associatedData == null) ? null : associatedData.getBytes(StandardCharsets.UTF_8);

            Cipher cipher = Cipher.getInstance(WechatPayConstants.AES_GCM_NO_PADDING);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(
                    WechatPayConstants.GCM_TAG_LENGTH_BITS, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            if (aad != null) {
                cipher.updateAAD(aad);
            }
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            String ciphertext = Base64.getEncoder().encodeToString(encrypted);
            log.debug("AES-GCM 加密成功, 明文长度={}, 密文长度={}", plaintext.length(), ciphertext.length());
            return ciphertext;
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM 加密失败", e);
        }
    }
}
