package com.shadowtradingplatform.trade.wechat.crypto;

import com.shadowtradingplatform.trade.wechat.WechatPayConstants;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 微信支付密钥/证书加载工具.
 *
 * <p>支持从 PEM 文件加载商户私钥和平台证书，
 * 并可在未配置证书时生成临时 RSA 密钥对（用于 Mock 模式）。</p>
 */
@Slf4j
public final class WechatPayKeyLoader {

    private WechatPayKeyLoader() {
    }

    /**
     * 从 PEM 文件加载商户私钥 (PKCS#8 格式).
     *
     * @param pemPath apiclient_key.pem 文件路径
     * @return RSA 私钥
     */
    public static PrivateKey loadPrivateKey(String pemPath) {
        try {
            String pemContent = readFile(pemPath);
            String base64Content = pemContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(base64Content);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(spec);
            log.info("商户私钥加载成功: {}", pemPath);
            return privateKey;
        } catch (Exception e) {
            throw new RuntimeException("加载商户私钥失败: " + pemPath, e);
        }
    }

    /**
     * 从 PEM 文件加载微信支付平台证书，提取公钥.
     *
     * @param certPath 证书文件路径
     * @return 平台证书公钥
     */
    public static PublicKey loadPlatformPublicKey(String certPath) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(
                    Files.newInputStream(Paths.get(certPath)));
            PublicKey publicKey = certificate.getPublicKey();
            log.info("平台证书加载成功: {}, 序列号={}", certPath, certificate.getSerialNumber());
            return publicKey;
        } catch (Exception e) {
            throw new RuntimeException("加载平台证书失败: " + certPath, e);
        }
    }

    /**
     * 生成临时 RSA-2048 密钥对 (用于 Mock 模式).
     *
     * @return 密钥对
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(WechatPayConstants.RSA_KEY_SIZE);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            log.info("Mock 模式: 已生成临时 RSA-2048 密钥对");
            return keyPair;
        } catch (Exception e) {
            throw new RuntimeException("生成 RSA 密钥对失败", e);
        }
    }

    /**
     * 从 RSA 私钥推导公钥 (用于 Mock 模式无证书时验签).
     *
     * @param privateKey RSA 私钥
     * @return 对应的公钥
     */
    public static PublicKey derivePublicKey(PrivateKey privateKey) {
        try {
            if (privateKey instanceof java.security.interfaces.RSAPrivateCrtKey rsaPrivateKey) {
                java.security.spec.RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(
                        rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
                return KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
            }
            throw new RuntimeException("私钥类型不支持推导公钥 (需要 RSAPrivateCrtKey): " + privateKey.getClass().getName());
        } catch (Exception e) {
            throw new RuntimeException("从私钥推导公钥失败", e);
        }
    }

    /**
     * 读取文件内容.
     */
    private static String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
