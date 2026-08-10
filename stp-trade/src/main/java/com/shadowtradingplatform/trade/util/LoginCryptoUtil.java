package com.shadowtradingplatform.trade.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/**
 * 登录加解密工具类.
 *
 * <p>支持 RSA+AES 混合加密方案：</p>
 * <ul>
 *   <li>RSA/ECB/PKCS1Padding 解密 AES 密钥（业务私钥 PKCS8）</li>
 *   <li>AES/CBC/PKCS7Padding 解密业务数据（IV 为十六进制字符串）</li>
 *   <li>解密后的 JSON 字符串解析为 {@code Map<String, Object>}</li>
 * </ul>
 *
 * <p><b>关于 PKCS7 与 Java 默认 PKCS5 的说明：</b>
 * PKCS5 是 PKCS7 的 8 字节块大小子集。由于 AES 块长 16 字节，
 * JDK 的 {@code PKCS5Padding} 对 AES 实际等价于 PKCS7。</p>
 */
@Component
public class LoginCryptoUtil {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private final ObjectMapper objectMapper;

    public LoginCryptoUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 登录数据整体解密流程.
     *
     * <ol>
     *   <li>使用 RSA 业务私钥解密 encryptedKey → AES 原始密钥字节</li>
     *   <li>将十六进制 iv 字符串解码为 16 字节 IV</li>
     *   <li>使用 AES/CBC/PKCS7 + AES key + IV 解密 encryptedData → JSON 明文</li>
     *   <li>JSON 明文解析为 Map 返回</li>
     * </ol>
     *
     * @param encryptedData Base64 编码的 AES 密文
     * @param encryptedKey  Base64 编码的 RSA 加密后的 AES key
     * @param ivHex         十六进制字符串形式的 IV（32 字符 = 16 字节）
     * @param rsaPrivateKey PKCS8 格式的 RSA 私钥（Base64，可带/不带 PEM 头尾）
     * @return 解密并解析后的业务 Map
     */
    public Map<String, Object> decryptLoginPayload(String encryptedData,
                                                   String encryptedKey,
                                                   String ivHex,
                                                   String rsaPrivateKey) {
        try {
            byte[] rawKeyBytes = rsaDecrypt(encryptedKey, rsaPrivateKey);
            // 前端可能将 AES key 转为 hex 字符串后再用 RSA 加密，
            // 导致 RSA 解密后得到的是 32/48/64 字节的 ASCII（对应 16/24/32 字节原始 key）。
            // 此处做归一化：若长度为合法 AES key 的 2 倍且可 hex decode，则还原为原始字节。
            byte[] aesKeyBytes = normalizeAesKey(rawKeyBytes);
            byte[] ivBytes = hexDecode(ivHex);
            byte[] plainBytes = aesCbcDecrypt(encryptedData, aesKeyBytes, ivBytes);
            String json = new String(plainBytes, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("登录数据解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * RSA 私钥解密（RSA/ECB/PKCS1Padding）.
     */
    public byte[] rsaDecrypt(String base64CipherText, String rsaPrivateKey) {
        try {
            PrivateKey privateKey = parsePkcs8PrivateKey(rsaPrivateKey);
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] cipherBytes = Base64.getDecoder().decode(base64CipherText);
            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            throw new IllegalStateException("RSA 解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * AES/CBC/PKCS7 解密（JDK 中 PKCS5Padding 对 AES 等价于 PKCS7）.
     */
    public byte[] aesCbcDecrypt(String base64CipherText, byte[] aesKeyBytes, byte[] ivBytes) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] cipherBytes = Base64.getDecoder().decode(base64CipherText);
            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            throw new IllegalStateException("AES-CBC 解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 归一化 AES 密钥字节.
     *
     * <p>兼容前端两种传 key 方式：</p>
     * <ul>
     *   <li>原始字节（16/24/32 字节）→ 直接返回</li>
     *   <li>hex 字符串的 ASCII 字节（32/48/64 字节，对应 16/24/32 字节原始 key）→ hex decode 后返回</li>
     * </ul>
     *
     * @param rawKeyBytes RSA 解密得到的 AES key 原始内容
     * @return 合法的 AES 密钥字节（16/24/32 字节）
     */
    private byte[] normalizeAesKey(byte[] rawKeyBytes) {
        if (rawKeyBytes == null) {
            throw new IllegalStateException("AES 密钥为空");
        }
        // 合法长度直接返回
        if (rawKeyBytes.length == 16 || rawKeyBytes.length == 24 || rawKeyBytes.length == 32) {
            return rawKeyBytes;
        }
        // 长度为合法 AES key 的 2 倍，尝试当作 hex 字符串解码
        if (rawKeyBytes.length == 32 || rawKeyBytes.length == 48 || rawKeyBytes.length == 64) {
            try {
                String hexStr = new String(rawKeyBytes, StandardCharsets.US_ASCII).trim();
                byte[] decoded = HexFormat.of().parseHex(hexStr);
                if (decoded.length == 16 || decoded.length == 24 || decoded.length == 32) {
                    return decoded;
                }
            } catch (Exception ignored) {
                // 不是 hex 字符串，继续按原内容报错
            }
        }
        throw new IllegalStateException(
                "无效的 AES 密钥长度: " + rawKeyBytes.length
                        + " 字节（合法长度: 16/24/32，或其 hex 字符串 32/48/64）");
    }

    /**
     * 将十六进制字符串解码为字节数组（支持 Java 17+ HexFormat + 手动兜底）.
     */
    public byte[] hexDecode(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("iv 不能为空");
        }
        try {
            return HexFormat.of().parseHex(hex.trim());
        } catch (Exception e) {
            String s = hex.trim();
            if ((s.length() & 1) != 0) {
                throw new IllegalArgumentException("iv 十六进制长度必须为偶数", e);
            }
            byte[] data = new byte[s.length() / 2];
            for (int i = 0; i < data.length; i++) {
                int high = Character.digit(s.charAt(i * 2), 16);
                int low = Character.digit(s.charAt(i * 2 + 1), 16);
                if (high < 0 || low < 0) {
                    throw new IllegalArgumentException("iv 含非法十六进制字符", e);
                }
                data[i] = (byte) ((high << 4) | low);
            }
            return data;
        }
    }

    /**
     * 解析 PKCS8 格式 RSA 私钥.
     *
     * <p>自动兼容：纯 Base64、带 {@code -----BEGIN PRIVATE KEY-----} PEM 头、
     * 带 {@code -----BEGIN RSA PRIVATE KEY-----} 头（会提示转换为 PKCS8）。</p>
     */
    private PrivateKey parsePkcs8PrivateKey(String keyBase64) {
        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new IllegalArgumentException("业务私钥未配置 (BUSINESS_PRIVATE_KEY)");
        }
        try {
            String cleaned = keyBase64
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(cleaned);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("私钥 Base64 解码失败", e);
            }

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance(RSA_ALGORITHM);
            return kf.generatePrivate(spec);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            String msg = "解析业务私钥失败";
            // PKCS1 格式时给出友好提示
            if (keyBase64 != null && keyBase64.contains("BEGIN RSA PRIVATE KEY")) {
                msg += "；当前私钥是 PKCS1(RSA PRIVATE KEY) 格式，请转换为 PKCS8(PRIVATE KEY)。"
                        + "转换命令: openssl pkcs8 -topk8 -nocrypt -in pkcs1.pem -out pkcs8.pem";
            }
            throw new IllegalStateException(msg + ": " + e.getMessage(), e);
        }
    }
}
