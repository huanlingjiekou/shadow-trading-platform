package com.shadowtradingplatform.trade.pay.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijpay.core.IJPayHttpResponse;
import com.ijpay.core.enums.RequestMethodEnum;
import com.ijpay.core.kit.AesUtil;
import com.ijpay.core.kit.WxPayKit;
import com.ijpay.wxpay.WxPayApi;
import com.ijpay.wxpay.enums.WxDomainEnum;
import com.ijpay.wxpay.enums.v3.BasePayApiEnum;
import com.shadowtradingplatform.trade.config.WxPayProperties;
import com.shadowtradingplatform.trade.pay.domain.enums.PayChannelEnum;
import com.shadowtradingplatform.trade.pay.domain.enums.TradeStatusEnum;
import com.shadowtradingplatform.trade.pay.domain.pojo.WxPayAmount;
import com.shadowtradingplatform.trade.pay.domain.pojo.WxPayH5Info;
import com.shadowtradingplatform.trade.pay.domain.pojo.WxPayPayer;
import com.shadowtradingplatform.trade.pay.domain.pojo.WxPaySceneInfo;
import com.shadowtradingplatform.trade.pay.domain.pojo.WxPayUnifiedOrderBizContent;
import com.shadowtradingplatform.trade.pay.domain.req.WxPayTradeReq;
import com.shadowtradingplatform.trade.pay.domain.vo.PayNotifyResultVO;
import com.shadowtradingplatform.trade.pay.domain.vo.TradeResultVO;
import com.shadowtradingplatform.trade.pay.service.WxPayService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 微信支付真实实现.
 *
 * <p>当 {@code wechat.pay.mock-enabled=false} (环境变量 WECHAT_PAY_MOCK_ENABLED=false) 时激活。</p>
 *
 * <p>使用 IJPay 的 WxPayApi.v3() 方法调用真实微信支付 Api-v3 接口，
 * 支持 JSAPI / Native / APP / H5 支付。</p>
 *
 * <p>签名方式: SHA256-RSA2048；回调验签 + AES-GCM 解密均由 IJPay 内部完成。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wechat.pay.mock-enabled", havingValue = "false", matchIfMissing = true)
public class WxPayRealService implements WxPayService {

    private final WxPayProperties properties;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info("===== 微信支付真实模式已启用 =====");
        log.info("  AppId: {}", properties.getAppId());
        log.info("  商户号: {}", properties.getMchId());
        log.info("  证书序列号: {}", properties.getMchSerialNo());
        log.info("  私钥路径: {}", properties.getPrivateKeyPath());
        log.info("=====================================");
    }

    @Override
    public TradeResultVO createOrder(WxPayTradeReq req) {
        log.info("[WXPAY] 收到下单请求: outTradeNo={}, tradeType={}", req.getOutTradeNo(), req.getTradeType());

        try {
            // 使用 POJO 构造 v3 统一下单参数 (@JsonNaming(SnakeCaseStrategy) 自动序列化为 snake_case JSON)
            WxPayAmount amount = new WxPayAmount();
            amount.setTotal(req.getTotalFee());
            amount.setCurrency("CNY");

            WxPayUnifiedOrderBizContent order = new WxPayUnifiedOrderBizContent();
            order.setAppid(properties.getAppId());
            order.setMchid(properties.getMchId());
            order.setDescription(req.getDescription());
            order.setOutTradeNo(req.getOutTradeNo());
            order.setAmount(amount);
            order.setNotifyUrl(properties.getNotifyUrl());

            // 按交易类型设置不同字段
            String apiPath;
            switch (req.getTradeType()) {
                case JSAPI:
                    WxPayPayer payer = new WxPayPayer();
                    payer.setOpenid(req.getOpenid());
                    order.setPayer(payer);
                    apiPath = BasePayApiEnum.JS_API_PAY.toString();
                    break;
                case NATIVE:
                    apiPath = BasePayApiEnum.NATIVE_PAY.toString();
                    break;
                case APP:
                    apiPath = BasePayApiEnum.APP_PAY.toString();
                    break;
                case H5:
                    WxPaySceneInfo sceneInfo = new WxPaySceneInfo();
                    sceneInfo.setPayerClientIp(req.getClientIp() != null ? req.getClientIp() : "127.0.0.1");
                    WxPayH5Info h5Info = new WxPayH5Info();
                    h5Info.setType("Wap");
                    sceneInfo.setH5Info(h5Info);
                    order.setSceneInfo(sceneInfo);
                    apiPath = BasePayApiEnum.H5_PAY.toString();
                    break;
                default:
                    throw new IllegalArgumentException("[WXPAY] 不支持的交易类型: " + req.getTradeType());
            }

            String body;
            try {
                body = objectMapper.writeValueAsString(order);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("[WXPAY] 统一下单请求体序列化失败", e);
            }
            log.info("[WXPAY] 统一下单请求体: {}", body);

            // 调用 IJPay v3 接口
            IJPayHttpResponse response = WxPayApi.v3(
                    RequestMethodEnum.POST,
                    WxDomainEnum.CHINA.toString(),
                    apiPath,
                    properties.getMchId(),
                    properties.getMchSerialNo(),
                    null,
                    properties.getPrivateKeyPath(),
                    body);

            log.info("[WXPAY] 统一下单响应: status={}, body={}", response.getStatus(), response.getBody());

            if (response.getStatus() != 200 && response.getStatus() != 204) {
                throw new RuntimeException("[WXPAY] 下单失败: " + response.getBody());
            }

            JSONObject result = JSONUtil.parseObj(response.getBody());
            String prepayId = result.getStr("prepay_id");

            // 不同交易类型提取不同的支付参数
            String payUrl = null;
            String qrCodeContent = null;
            String jsapiPayParams = null;

            switch (req.getTradeType()) {
                case NATIVE:
                    qrCodeContent = result.getStr("code_url");
                    break;
                case H5:
                    JSONObject h5UrlObj = result.getJSONObject("h5_url");
                    if (h5UrlObj != null) {
                        payUrl = h5UrlObj.getStr("h5_url");
                    }
                    break;
                case JSAPI:
                    jsapiPayParams = buildJsapiPaySign(prepayId);
                    break;
                case APP:
                    jsapiPayParams = buildAppPaySign(prepayId);
                    break;
                default:
                    break;
            }

            log.info("[WXPAY] 下单成功: prepayId={}", prepayId);

            return TradeResultVO.builder()
                    .outTradeNo(req.getOutTradeNo())
                    .payChannel(PayChannelEnum.WXPAY)
                    .tradeType(req.getTradeType())
                    .prepayId(prepayId)
                    .payUrl(payUrl)
                    .qrCodeContent(qrCodeContent)
                    .jsapiPayParams(jsapiPayParams)
                    .mock(false)
                    .build();
        } catch (Exception e) {
            log.error("[WXPAY] 下单失败", e);
            throw new RuntimeException("[WXPAY] 下单失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PayNotifyResultVO parseNotify(String body, String timestamp, String nonce, String serial, String signature) {
        log.info("[WXPAY] 收到微信支付回调通知: serial={}", serial);

        try {
            // 加载平台证书的 PublicKey 用于验签 (使用 Java 安全 API)
            PublicKey publicKey;
            try (InputStream certStream = new FileInputStream(properties.getPlatformCertPath())) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(certStream);
                publicKey = cert.getPublicKey();
            }

            // 使用 IJPay 验签 (5参数 PublicKey 形式)
            boolean verifyResult = WxPayKit.verifySignature(
                    timestamp,
                    nonce,
                    body,
                    serial,
                    publicKey);

            if (!verifyResult) {
                log.error("[WXPAY] 验签失败");
                return PayNotifyResultVO.builder()
                        .responseCode("FAIL")
                        .responseMessage("验签失败")
                        .payChannel(PayChannelEnum.WXPAY)
                        .build();
            }

            // 验签通过, 解密回调报文
            JSONObject notifyObj = JSONUtil.parseObj(body);
            JSONObject resource = notifyObj.getJSONObject("resource");
            String associatedData = resource.getStr("associated_data");
            String ciphertext = resource.getStr("ciphertext");
            String nonceStr = resource.getStr("nonce");

            // AES-GCM 解密 (实例化 AesUtil)
            AesUtil aesUtil = new AesUtil(properties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
            String decryptedJson = aesUtil.decryptToString(
                    associatedData != null ? associatedData.getBytes(StandardCharsets.UTF_8) : null,
                    nonceStr.getBytes(StandardCharsets.UTF_8),
                    ciphertext);

            JSONObject decrypted = JSONUtil.parseObj(decryptedJson);

            String outTradeNo = decrypted.getStr("out_trade_no");
            String transactionId = decrypted.getStr("transaction_id");
            String tradeState = decrypted.getStr("trade_state");
            Integer totalFee = decrypted.getJSONObject("amount") != null
                    ? decrypted.getJSONObject("amount").getInt("total") : null;
            String successTimeStr = decrypted.getStr("success_time");

            LocalDateTime successTime = null;
            if (successTimeStr != null) {
                successTime = OffsetDateTime.parse(successTimeStr).toLocalDateTime();
            }

            TradeStatusEnum statusEnum = "SUCCESS".equals(tradeState)
                    ? TradeStatusEnum.SUCCESS
                    : TradeStatusEnum.NOTPAY;

            BigDecimal totalAmount = totalFee != null
                    ? new BigDecimal(totalFee).movePointLeft(2)
                    : null;

            log.info("[WXPAY] 回调处理成功: outTradeNo={}, transactionId={}, tradeState={}",
                    outTradeNo, transactionId, tradeState);

            return PayNotifyResultVO.builder()
                    .responseCode("SUCCESS")
                    .responseMessage("处理成功")
                    .payChannel(PayChannelEnum.WXPAY)
                    .outTradeNo(outTradeNo)
                    .tradeNo(transactionId)
                    .totalAmount(totalAmount)
                    .tradeStatus(statusEnum)
                    .successTime(successTime)
                    .build();
        } catch (Exception e) {
            log.error("[WXPAY] 回调处理失败", e);
            return PayNotifyResultVO.builder()
                    .responseCode("FAIL")
                    .responseMessage("处理失败: " + e.getMessage())
                    .payChannel(PayChannelEnum.WXPAY)
                    .build();
        }
    }

    /**
     * 生成 JSAPI 调起支付所需的 5 个签名参数.
     */
    private String buildJsapiPaySign(String prepayId) {
        try {
            Map<String, String> params = WxPayKit.jsApiCreateSign(
                    properties.getAppId(),
                    prepayId,
                    properties.getPrivateKeyPath());
            return JSONUtil.toJsonStr(params);
        } catch (Exception e) {
            log.error("[WXPAY] 生成 JSAPI 签名失败", e);
            return "{}";
        }
    }

    /**
     * 生成 APP 调起支付所需的签名参数.
     */
    private String buildAppPaySign(String prepayId) {
        try {
            Map<String, String> params = WxPayKit.appCreateSign(
                    properties.getAppId(),
                    properties.getMchId(),
                    prepayId,
                    properties.getPrivateKeyPath());
            return JSONUtil.toJsonStr(params);
        } catch (Exception e) {
            log.error("[WXPAY] 生成 APP 签名失败", e);
            return "{}";
        }
    }
}
