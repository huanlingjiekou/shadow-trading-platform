package com.shadowtradingplatform.trade.wechat.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.shadowtradingplatform.trade.config.WechatPayProperties;
import com.shadowtradingplatform.trade.wechat.WechatPayConstants;
import com.shadowtradingplatform.trade.wechat.crypto.AesGcmDecryptor;
import com.shadowtradingplatform.trade.wechat.crypto.WechatPayKeyLoader;
import com.shadowtradingplatform.trade.wechat.crypto.WechatPaySigner;
import com.shadowtradingplatform.trade.wechat.crypto.WechatPayVerifier;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

/**
 * 微信支付真实实现.
 *
 * <p>当 {@code wechat.pay.mock-enabled=false} 时激活。
 * 使用真实网络请求调用微信支付 APIv3 接口。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wechat.pay.mock-enabled", havingValue = "false", matchIfMissing = true)
public class WechatPayRealService implements WechatPayService {

    private final WechatPayProperties properties;

    private WechatPaySigner signer;
    private WechatPayVerifier verifier;
    private AesGcmDecryptor decryptor;
    private HttpClient httpClient;
    private final ObjectMapper wechatMapper = buildWechatMapper();

    @PostConstruct
    public void init() {
        PrivateKey privateKey = WechatPayKeyLoader.loadPrivateKey(properties.getPrivateKeyPath());
        PublicKey publicKey = WechatPayKeyLoader.loadPlatformPublicKey(properties.getCertPath());
        this.signer = new WechatPaySigner(privateKey);
        this.verifier = new WechatPayVerifier(publicKey);
        this.decryptor = new AesGcmDecryptor(properties.getApiV3Key());
        this.httpClient = HttpClient.newBuilder().build();
        log.info("微信支付真实服务初始化完成 (商户号={})", properties.getMerchantId());
    }

    @Override
    public PrepayResponse createOrder(PrepayRequest request) {
        try {
            // 补全商户信息
            request.setMchid(properties.getMerchantId());
            if (request.getNotifyUrl() == null) {
                request.setNotifyUrl(properties.getNotifyUrl());
            }

            String body = wechatMapper.writeValueAsString(request);
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonce = UUID.randomUUID().toString().replace("-", "");

            // === 真实签名 ===
            String signature = signer.sign("POST", WechatPayConstants.JSAPI_PREPAY_PATH, timestamp, nonce, body);
            String authorization = signer.buildAuthorization(
                    properties.getMerchantId(), properties.getMerchantSerialNumber(),
                    timestamp, nonce, signature);

            // === 真实 HTTP 请求 ===
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(WechatPayConstants.API_BASE_URL + WechatPayConstants.JSAPI_PREPAY_PATH))
                    .header(WechatPayConstants.HEADER_ACCEPT, WechatPayConstants.ACCEPT_JSON)
                    .header(WechatPayConstants.HEADER_CONTENT_TYPE, WechatPayConstants.CONTENT_TYPE_JSON)
                    .header(WechatPayConstants.HEADER_USER_AGENT, WechatPayConstants.USER_AGENT)
                    .header(WechatPayConstants.HEADER_AUTHORIZATION, authorization)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info("统一下单响应: status={}", response.statusCode());

            if (response.statusCode() != 200) {
                throw new RuntimeException("统一下单失败: HTTP " + response.statusCode() + ", body=" + response.body());
            }

            // === 响应验签 ===
            String respTimestamp = response.headers().firstValue(WechatPayConstants.HEADER_WECHATPAY_TIMESTAMP).orElse("");
            String respNonce = response.headers().firstValue(WechatPayConstants.HEADER_WECHATPAY_NONCE).orElse("");
            String respSignature = response.headers().firstValue(WechatPayConstants.HEADER_WECHATPAY_SIGNATURE).orElse("");
            if (!respSignature.isEmpty()) {
                boolean verified = verifier.verify(respTimestamp, respNonce, response.body(), respSignature);
                log.info("响应验签结果: {}", verified);
            }

            String prepayId = wechatMapper.readTree(response.body()).get("prepay_id").asText();
            return PrepayResponse.builder().prepayId(prepayId).mock(false).build();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("统一下单失败", e);
        }
    }

    @Override
    public NotifyDecryptResult parseNotify(String body, String timestamp, String nonce, String serial, String signature) {
        try {
            // === 真实验签 ===
            boolean verified = verifier.verify(timestamp, nonce, body, signature);
            if (!verified) {
                throw new RuntimeException("回调验签失败: 签名不匹配");
            }
            log.info("回调验签通过, serial={}", serial);

            // === 解析报文 ===
            NotifyParams params = wechatMapper.readValue(body, NotifyParams.class);

            // === 真实 AES-GCM 解密 ===
            NotifyResource resource = params.getResource();
            String decryptedJson = decryptor.decrypt(
                    resource.getAssociatedData(),
                    resource.getNonce(),
                    resource.getCiphertext());

            NotifyDecryptResult result = wechatMapper.readValue(decryptedJson, NotifyDecryptResult.class);
            log.info("回调解析成功: outTradeNo={}, tradeState={}", result.getOutTradeNo(), result.getTradeState());
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("回调解析失败", e);
        }
    }

    private ObjectMapper buildWechatMapper() {
        return new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
