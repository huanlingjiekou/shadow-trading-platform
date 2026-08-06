package com.shadowtradingplatform.trade.wechat.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.shadowtradingplatform.trade.config.WechatPayProperties;
import com.shadowtradingplatform.trade.wechat.WechatPayConstants;
import com.shadowtradingplatform.trade.wechat.crypto.AesGcmDecryptor;
import com.shadowtradingplatform.trade.wechat.crypto.AesGcmEncryptor;
import com.shadowtradingplatform.trade.wechat.crypto.WechatPayKeyLoader;
import com.shadowtradingplatform.trade.wechat.crypto.WechatPaySigner;
import com.shadowtradingplatform.trade.wechat.crypto.WechatPayVerifier;
import com.shadowtradingplatform.trade.wechat.dto.Amount;
import com.shadowtradingplatform.trade.wechat.dto.NotifyDecryptResult;
import com.shadowtradingplatform.trade.wechat.dto.NotifyParams;
import com.shadowtradingplatform.trade.wechat.dto.NotifyResource;
import com.shadowtradingplatform.trade.wechat.dto.PrepayRequest;
import com.shadowtradingplatform.trade.wechat.dto.PrepayResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 微信支付 Mock 实现.
 *
 * <p>当 {@code wechat.pay.mock-enabled=true} (环境变量 MOCK_ENABLED=true) 时激活。</p>
 *
 * <p>核心特点：依然执行真实的 APIv3 签名 (SHA256-RSA2048) 与 AES-256-GCM 加解密逻辑，
 * 但不向微信服务器发送网络请求，而是返回模拟数据。</p>
 *
 * <p>密钥策略：</p>
 * <ul>
 *   <li>若配置了商户私钥 → 加载真实私钥；否则生成临时 RSA-2048 密钥对</li>
 *   <li>若配置了平台证书 → 加载真实公钥验签；否则从私钥推导公钥</li>
 *   <li>若配置了 APIv3 密钥 → 使用真实密钥；否则使用 Mock 默认密钥</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wechat.pay.mock-enabled", havingValue = "true")
public class WechatPayMockService implements WechatPayService {

    private final WechatPayProperties properties;

    /** Mock 模式默认 APIv3 密钥 (必须 32 字符，AES-256)，仅用于本地加解密测试 */
    private static final String MOCK_API_V3_KEY = "MOCK_API_V3_KEY_FOR_TESTING_32CH";

    private WechatPaySigner signer;
    private WechatPayVerifier verifier;
    private AesGcmEncryptor encryptor;
    private AesGcmDecryptor decryptor;
    private final ObjectMapper wechatMapper = buildWechatMapper();

    @PostConstruct
    public void init() {
        PrivateKey privateKey;
        PublicKey publicKey = null;

        // 商户私钥：加载真实私钥或生成临时密钥对
        if (hasValue(properties.getPrivateKeyPath())) {
            privateKey = WechatPayKeyLoader.loadPrivateKey(properties.getPrivateKeyPath());
        } else {
            KeyPair keyPair = WechatPayKeyLoader.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }

        // 平台公钥：加载真实证书或从私钥推导
        if (hasValue(properties.getCertPath())) {
            publicKey = WechatPayKeyLoader.loadPlatformPublicKey(properties.getCertPath());
        } else if (publicKey == null) {
            publicKey = WechatPayKeyLoader.derivePublicKey(privateKey);
        }

        // APIv3 密钥：使用真实密钥或 Mock 默认密钥
        String apiV3Key = hasValue(properties.getApiV3Key()) ? properties.getApiV3Key() : MOCK_API_V3_KEY;

        this.signer = new WechatPaySigner(privateKey);
        this.verifier = new WechatPayVerifier(publicKey);
        this.encryptor = new AesGcmEncryptor(apiV3Key);
        this.decryptor = new AesGcmDecryptor(apiV3Key);

        log.warn("===== 微信支付 Mock 模式已启用 =====");
        log.warn("  签名算法: {} (真实执行)", WechatPayConstants.SIGN_ALGORITHM);
        log.warn("  加解密算法: {} (真实执行)", WechatPayConstants.AES_GCM_NO_PADDING);
        log.warn("  网络请求: 已禁用 (返回模拟数据)");
        log.warn("  APIv3 密钥: {}", hasValue(properties.getApiV3Key()) ? "使用配置密钥" : "使用 Mock 默认密钥");
        log.warn("  商户私钥: {}", hasValue(properties.getPrivateKeyPath()) ? "已加载真实私钥" : "已生成临时密钥对");
        log.warn("=====================================");
    }

    @Override
    public PrepayResponse createOrder(PrepayRequest request) {
        try {
            // 补全商户信息
            request.setMchid(hasValue(properties.getMerchantId()) ? properties.getMerchantId() : "MOCK_MERCHANT_ID");
            if (request.getNotifyUrl() == null) {
                request.setNotifyUrl(hasValue(properties.getNotifyUrl())
                        ? properties.getNotifyUrl() : "https://mock.example.com/notify");
            }

            String body = wechatMapper.writeValueAsString(request);
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonce = UUID.randomUUID().toString().replace("-", "");

            // === 真实签名逻辑执行 (SHA256-RSA2048) ===
            String signature = signer.sign("POST", WechatPayConstants.JSAPI_PREPAY_PATH, timestamp, nonce, body);
            String authorization = signer.buildAuthorization(
                    request.getMchid(),
                    hasValue(properties.getMerchantSerialNumber()) ? properties.getMerchantSerialNumber() : "MOCK_SERIAL",
                    timestamp, nonce, signature);

            log.info("[MOCK] 统一下单 - 签名已执行 (未发送网络请求)");
            log.info("[MOCK] 请求体: {}", body);
            log.info("[MOCK] 签名值: {}", signature);
            log.info("[MOCK] Authorization: {}", authorization);

            // 返回模拟 prepay_id
            String mockPrepayId = "mock_prepay_id_" + System.currentTimeMillis();
            log.info("[MOCK] 返回模拟 prepay_id: {}", mockPrepayId);

            return PrepayResponse.builder()
                    .prepayId(mockPrepayId)
                    .mock(true)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("[MOCK] 统一下单失败", e);
        }
    }

    @Override
    public NotifyDecryptResult parseNotify(String body, String timestamp, String nonce, String serial, String signature) {
        try {
            if (body == null || body.isEmpty()) {
                // === 自测试模式：构造模拟加密回调报文 ===
                log.info("[MOCK] 回调自测试模式 - 构造模拟加密报文");
                return simulateAndDecryptNotify();
            }

            // === 正常回调处理模式 (验签 + 解密均真实执行) ===
            log.info("[MOCK] 回调正常处理模式 - 验签 + 解密");
            boolean verified = verifier.verify(timestamp, nonce, body, signature);
            if (!verified) {
                throw new RuntimeException("[MOCK] 回调验签失败: 签名不匹配");
            }
            log.info("[MOCK] 回调验签通过");

            NotifyParams params = wechatMapper.readValue(body, NotifyParams.class);
            NotifyResource resource = params.getResource();
            String decryptedJson = decryptor.decrypt(
                    resource.getAssociatedData(),
                    resource.getNonce(),
                    resource.getCiphertext());

            NotifyDecryptResult result = wechatMapper.readValue(decryptedJson, NotifyDecryptResult.class);
            log.info("[MOCK] 回调解密成功: outTradeNo={}, tradeState={}", result.getOutTradeNo(), result.getTradeState());
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("[MOCK] 回调解析失败", e);
        }
    }

    /**
     * 构造模拟加密回调报文并执行完整的加解密测试链路.
     *
     * <p>流程：构造明文 → 真实AES-GCM加密 → 真实签名 → 真实验签 → 真实AES-GCM解密</p>
     */
    private NotifyDecryptResult simulateAndDecryptNotify() throws Exception {
        // 1. 构造模拟交易结果明文
        NotifyDecryptResult mockResult = NotifyDecryptResult.builder()
                .mchid(hasValue(properties.getMerchantId()) ? properties.getMerchantId() : "MOCK_MERCHANT_ID")
                .appid("MOCK_APPID_" + System.currentTimeMillis())
                .outTradeNo("MOCK_OUT_TRADE_NO_" + System.currentTimeMillis())
                .transactionId("MOCK_TRANSACTION_ID_" + System.currentTimeMillis())
                .tradeType("JSAPI")
                .tradeState(WechatPayConstants.TRADE_STATE_SUCCESS)
                .tradeStateDesc("支付成功")
                .bankType("OTHERS")
                .successTime(OffsetDateTime.now().toString())
                .payer(NotifyDecryptResult.Payer.builder()
                        .openid("MOCK_OPENID")
                        .build())
                .amount(Amount.builder()
                        .total(100)
                        .currency("CNY")
                        .build())
                .build();

        String plaintext = wechatMapper.writeValueAsString(mockResult);
        log.info("[MOCK] (1/5) 构造模拟交易明文: {}", plaintext);

        // 2. 真实 AES-GCM 加密
        String nonceStr = UUID.randomUUID().toString().replace("-", "").substring(0, WechatPayConstants.GCM_NONCE_LENGTH);
        String associatedData = "transaction";
        String ciphertext = encryptor.encrypt(plaintext, associatedData, nonceStr.getBytes(StandardCharsets.UTF_8));
        log.info("[MOCK] (2/5) AES-GCM 加密完成, nonce={}, 密文长度={}", nonceStr, ciphertext.length());

        // 3. 构造完整回调报文并真实签名
        NotifyParams mockParams = NotifyParams.builder()
                .transactionId(mockResult.getTransactionId())
                .outTradeNo(mockResult.getOutTradeNo())
                .eventType("TRANSACTION.SUCCESS")
                .summary("支付成功")
                .resource(NotifyResource.builder()
                        .algorithm("AES_256_GCM")
                        .ciphertext(ciphertext)
                        .associatedData(associatedData)
                        .nonce(nonceStr)
                        .build())
                .build();

        String notifyBody = wechatMapper.writeValueAsString(mockParams);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = signer.signNotify(timestamp, nonceStr, notifyBody);
        log.info("[MOCK] (3/5) 回调签名完成, signature={}", signature);

        // 4. 真实验签
        boolean verified = verifier.verify(timestamp, nonceStr, notifyBody, signature);
        log.info("[MOCK] (4/5) 验签结果: {}", verified);
        if (!verified) {
            throw new RuntimeException("[MOCK] 自测验签失败");
        }

        // 5. 真实 AES-GCM 解密
        String decryptedJson = decryptor.decrypt(associatedData, nonceStr, ciphertext);
        log.info("[MOCK] (5/5) AES-GCM 解密完成, 明文: {}", decryptedJson);

        NotifyDecryptResult result = wechatMapper.readValue(decryptedJson, NotifyDecryptResult.class);
        log.info("[MOCK] 回调自测全链路通过: outTradeNo={}, tradeState={}", result.getOutTradeNo(), result.getTradeState());
        return result;
    }

    private boolean hasValue(String value) {
        return value != null && !value.isEmpty();
    }

    private ObjectMapper buildWechatMapper() {
        return new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
