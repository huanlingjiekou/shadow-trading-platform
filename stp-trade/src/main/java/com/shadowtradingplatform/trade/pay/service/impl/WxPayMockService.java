package com.shadowtradingplatform.trade.pay.service.impl;

import com.shadowtradingplatform.trade.config.WxPayProperties;
import com.shadowtradingplatform.trade.pay.domain.enums.PayChannelEnum;
import com.shadowtradingplatform.trade.pay.domain.enums.TradeStatusEnum;
import com.shadowtradingplatform.trade.pay.domain.req.WxPayTradeReq;
import com.shadowtradingplatform.trade.pay.domain.vo.PayNotifyResultVO;
import com.shadowtradingplatform.trade.pay.domain.vo.TradeResultVO;
import com.shadowtradingplatform.trade.pay.service.WxPayService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 微信支付 Mock 实现.
 *
 * <p>当 {@code wechat.pay.mock-enabled=true} (环境变量 WECHAT_PAY_MOCK_ENABLED=true) 时激活。</p>
 *
 * <p>核心特点：不向微信支付服务器发送真实 HTTP 请求，构造模拟下单结果返回。
 * 适用于未申请商户号时的本地开发与联调。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wechat.pay.mock-enabled", havingValue = "true")
public class WxPayMockService implements WxPayService {

    private final WxPayProperties properties;

    @PostConstruct
    public void init() {
        log.warn("===== 微信支付 Mock 模式已启用 =====");
        log.warn("  网络请求: 已禁用 (返回模拟下单结果)");
        log.warn("  商户号: {}", hasValue(properties.getMchId()) ? properties.getMchId() : "(未配置,使用模拟值)");
        log.warn("  AppId: {}", hasValue(properties.getAppId()) ? properties.getAppId() : "(未配置,使用模拟值)");
        log.warn("=====================================");
    }

    @Override
    public TradeResultVO createOrder(WxPayTradeReq req) {
        log.info("[MOCK-WXPAY] 收到下单请求: outTradeNo={}, description={}, totalFee={}, tradeType={}",
                req.getOutTradeNo(), req.getDescription(), req.getTotalFee(), req.getTradeType());

        String mockPrepayId = "mock_wx_prepay_id_" + System.currentTimeMillis();
        String mockCodeUrl = null;
        String mockMwebUrl = null;
        String mockJsapiParams = null;

        switch (req.getTradeType()) {
            case NATIVE:
                // Native 扫码支付: 返回二维码内容
                mockCodeUrl = "weixin://wxpay/bizpayurl?pr=mock_" + UUID.randomUUID().toString().replace("-", "");
                log.info("[MOCK-WXPAY] 模拟 Native 下单成功: codeUrl={}", mockCodeUrl);
                break;
            case H5:
                // H5 支付: 返回 mweb_url
                mockMwebUrl = "https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=" + mockPrepayId;
                log.info("[MOCK-WXPAY] 模拟 H5 下单成功: mwebUrl={}", mockMwebUrl);
                break;
            case JSAPI:
                // JSAPI 支付: 返回调起支付的签名参数
                mockJsapiParams = buildMockJsapiPayParams(mockPrepayId);
                log.info("[MOCK-WXPAY] 模拟 JSAPI 下单成功: prepayId={}, jsapiParams={}", mockPrepayId, mockJsapiParams);
                break;
            case APP:
                // APP 支付: 返回调起参数
                mockJsapiParams = buildMockAppPayParams(mockPrepayId);
                log.info("[MOCK-WXPAY] 模拟 APP 下单成功: prepayId={}, appParams={}", mockPrepayId, mockJsapiParams);
                break;
            default:
                log.warn("[MOCK-WXPAY] 未知的交易类型: {}", req.getTradeType());
        }

        return TradeResultVO.builder()
                .outTradeNo(req.getOutTradeNo())
                .payChannel(PayChannelEnum.WXPAY)
                .tradeType(req.getTradeType())
                .prepayId(mockPrepayId)
                .payUrl(mockMwebUrl)
                .qrCodeContent(mockCodeUrl)
                .jsapiPayParams(mockJsapiParams)
                .mock(true)
                .build();
    }

    @Override
    public PayNotifyResultVO parseNotify(String body, String timestamp, String nonce, String serial, String signature) {
        log.info("[MOCK-WXPAY] 收到模拟微信支付回调通知");
        log.info("[MOCK-WXPAY] body={}", body);
        log.info("[MOCK-WXPAY] timestamp={}, nonce={}, serial={}, signature={}",
                timestamp, nonce, serial, signature);

        // 构造模拟回调结果
        String mockOutTradeNo = "MOCK_OUT_TRADE_NO_" + System.currentTimeMillis();
        String mockTransactionId = "mock_transaction_id_" + System.currentTimeMillis();

        log.info("[MOCK-WXPAY] 模拟回调处理成功: outTradeNo={}, transactionId={}", mockOutTradeNo, mockTransactionId);

        return PayNotifyResultVO.builder()
                .responseCode("SUCCESS")
                .responseMessage("Mock 微信支付回调处理成功")
                .payChannel(PayChannelEnum.WXPAY)
                .outTradeNo(mockOutTradeNo)
                .tradeNo(mockTransactionId)
                .totalAmount(new java.math.BigDecimal("0.01"))
                .tradeStatus(TradeStatusEnum.SUCCESS)
                .successTime(LocalDateTime.now())
                .build();
    }

    /**
     * 构造模拟的 JSAPI 调起支付参数 (与真实微信支付返回的 5 个字段一致).
     */
    private String buildMockJsapiPayParams(String prepayId) {
        Map<String, String> params = new HashMap<>();
        params.put("appId", hasValue(properties.getAppId()) ? properties.getAppId() : "wx1234567890abcdef");
        params.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("nonceStr", UUID.randomUUID().toString().replace("-", ""));
        params.put("package", "prepay_id=" + prepayId);
        params.put("signType", "RSA");
        params.put("paySign", "mock_sign_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        return params.toString();
    }

    /**
     * 构造模拟的 APP 调起支付参数.
     */
    private String buildMockAppPayParams(String prepayId) {
        Map<String, String> params = new HashMap<>();
        params.put("appid", hasValue(properties.getAppId()) ? properties.getAppId() : "wx1234567890abcdef");
        params.put("partnerid", hasValue(properties.getMchId()) ? properties.getMchId() : "1234567890");
        params.put("prepayid", prepayId);
        params.put("package", "Sign=WXPay");
        params.put("noncestr", UUID.randomUUID().toString().replace("-", ""));
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("sign", "mock_sign_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        return params.toString();
    }

    private boolean hasValue(String value) {
        return value != null && !value.isEmpty();
    }
}
