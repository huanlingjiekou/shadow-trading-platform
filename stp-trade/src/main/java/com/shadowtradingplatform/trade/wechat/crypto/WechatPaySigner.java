package com.shadowtradingplatform.trade.wechat.crypto;

import com.shadowtradingplatform.trade.wechat.WechatPayConstants;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

/**
 * 微信支付 APIv3 签名器.
 *
 * <p>实现真实的 SHA256-RSA2048 签名逻辑：
 * 使用商户私钥对请求报文进行 {@code SHA256withRSA} 签名，输出 Base64 编码结果。</p>
 *
 * <p>签名串格式（5行，每行末尾换行符）：</p>
 * <pre>
 * HTTP请求方法\n
 * 请求URL（含查询参数）\n
 * 时间戳\n
 * 随机串\n
 * 请求报文主体\n
 * </pre>
 */
@Slf4j
public class WechatPaySigner {

    private final PrivateKey privateKey;

    public WechatPaySigner(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * 生成 APIv3 请求签名.
     *
     * @param method    HTTP 方法 (GET/POST/PUT/DELETE)
     * @param url       请求路径（含查询参数，不含域名）
     * @param timestamp 时间戳（秒级）
     * @param nonce     随机串
     * @param body      请求报文主体（GET 请求为空串）
     * @return Base64 编码的签名值
     */
    public String sign(String method, String url, String timestamp, String nonce, String body) {
        String message = buildSignatureMessage(method, url, timestamp, nonce, body);
        try {
            Signature signature = Signature.getInstance(WechatPayConstants.SIGN_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            byte[] signed = signature.sign();
            String signatureBase64 = Base64.getEncoder().encodeToString(signed);
            log.debug("APIv3 签名完成, 待签名串=\n{}", message);
            return signatureBase64;
        } catch (Exception e) {
            throw new RuntimeException("生成 APIv3 签名失败", e);
        }
    }

    /**
     * 构造待签名串.
     */
    private String buildSignatureMessage(String method, String url, String timestamp, String nonce, String body) {
        String bodyStr = body == null ? "" : body;
        return method + "\n"
                + url + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + bodyStr + "\n";
    }

    /**
     * 生成回调通知签名 (3行格式).
     *
     * <p>微信回调/响应的签名串格式（与请求签名不同，无 method 和 url）：</p>
     * <pre>
     * 时间戳\n
     * 随机串\n
     * 报文主体\n
     * </pre>
     *
     * @param timestamp 时间戳（秒级）
     * @param nonce     随机串
     * @param body      报文主体
     * @return Base64 编码的签名值
     */
    public String signNotify(String timestamp, String nonce, String body) {
        String bodyStr = body == null ? "" : body;
        String message = timestamp + "\n" + nonce + "\n" + bodyStr + "\n";
        try {
            Signature signature = Signature.getInstance(WechatPayConstants.SIGN_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            byte[] signed = signature.sign();
            String signatureBase64 = Base64.getEncoder().encodeToString(signed);
            log.debug("APIv3 回调签名完成, 待签名串=\n{}", message);
            return signatureBase64;
        } catch (Exception e) {
            throw new RuntimeException("生成 APIv3 回调签名失败", e);
        }
    }

    /**
     * 构建 Authorization 头值.
     *
     * <p>格式：WECHATPAY2-SHA256-RSA2048 mchid="...",serial_no="...",timestamp="...",nonce_str="...",signature="..."</p>
     *
     * @param merchantId         商户号
     * @param merchantSerialNo   商户证书序列号
     * @param signature          签名值
     * @param timestamp          时间戳
     * @param nonce              随机串
     * @return Authorization 头完整值
     */
    public String buildAuthorization(String merchantId, String merchantSerialNo,
                                     String timestamp, String nonce, String signature) {
        return WechatPayConstants.SIGN_TYPE
                + " mchid=\"" + merchantId + "\""
                + ",serial_no=\"" + merchantSerialNo + "\""
                + ",timestamp=\"" + timestamp + "\""
                + ",nonce_str=\"" + nonce + "\""
                + ",signature=\"" + signature + "\"";
    }
}
