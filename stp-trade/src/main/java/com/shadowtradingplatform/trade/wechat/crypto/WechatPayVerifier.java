package com.shadowtradingplatform.trade.wechat.crypto;

import com.shadowtradingplatform.trade.wechat.WechatPayConstants;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * 微信支付 APIv3 验签器.
 *
 * <p>实现真实的签名验证逻辑：使用微信支付平台证书公钥，
 * 对回调/响应报文进行 {@code SHA256withRSA} 验签。</p>
 *
 * <p>回调验签串格式：</p>
 * <pre>
 * 时间戳\n
 * 随机串\n
 * 报文主体\n
 * </pre>
 */
@Slf4j
public class WechatPayVerifier {

    private final PublicKey platformPublicKey;

    public WechatPayVerifier(PublicKey platformPublicKey) {
        this.platformPublicKey = platformPublicKey;
    }

    /**
     * 验证回调/响应签名.
     *
     * @param timestamp      时间戳（Wechatpay-Timestamp 头）
     * @param nonce          随机串（Wechatpay-Nonce 头）
     * @param body           报文主体
     * @param signatureBase64 Base64 编码的签名值（Wechatpay-Signature 头）
     * @return true 验签通过，false 验签失败
     */
    public boolean verify(String timestamp, String nonce, String body, String signatureBase64) {
        String message = buildVerifyMessage(timestamp, nonce, body);
        try {
            Signature signature = Signature.getInstance(WechatPayConstants.SIGN_ALGORITHM);
            signature.initVerify(platformPublicKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            boolean result = signature.verify(signatureBytes);
            if (result) {
                log.debug("APIv3 验签通过, 待验签串=\n{}", message);
            } else {
                log.warn("APIv3 验签失败, 待验签串=\n{}", message);
            }
            return result;
        } catch (Exception e) {
            log.error("APIv3 验签异常", e);
            return false;
        }
    }

    /**
     * 构造验签串.
     */
    private String buildVerifyMessage(String timestamp, String nonce, String body) {
        String bodyStr = body == null ? "" : body;
        return timestamp + "\n"
                + nonce + "\n"
                + bodyStr + "\n";
    }
}
