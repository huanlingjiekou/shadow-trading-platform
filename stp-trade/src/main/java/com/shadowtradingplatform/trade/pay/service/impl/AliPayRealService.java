package com.shadowtradingplatform.trade.pay.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.shadowtradingplatform.trade.config.AliPayProperties;
import com.shadowtradingplatform.trade.pay.domain.enums.PayChannelEnum;
import com.shadowtradingplatform.trade.pay.domain.enums.TradeStatusEnum;
import com.shadowtradingplatform.trade.pay.domain.enums.TradeTypeEnum;
import com.shadowtradingplatform.trade.pay.domain.req.AliPayTradeReq;
import com.shadowtradingplatform.trade.pay.domain.vo.PayNotifyResultVO;
import com.shadowtradingplatform.trade.pay.domain.vo.TradeResultVO;
import com.shadowtradingplatform.trade.pay.service.AliPayService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝真实实现.
 *
 * <p>当 {@code alipay.mock-enabled=false} (环境变量 ALIPAY_MOCK_ENABLED=false) 时激活。</p>
 *
 * <p>使用 IJPay 内置的支付宝 SDK (alipay-sdk-java) 调用真实支付宝网关，
 * 支持 PC 网站支付 / 手机网站支付 / 当面付扫码 / APP 支付。</p>
 *
 * <p>签名方式: RSA2 (普通公钥模式)。如需证书模式，可改用
 * {@code CertAlipayRequest} 与 {@code DefaultCertAlipayClient}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alipay.mock-enabled", havingValue = "false", matchIfMissing = true)
public class AliPayRealService implements AliPayService {

    private final AliPayProperties properties;

    private AlipayClient alipayClient;

    @PostConstruct
    public void init() {
        log.info("===== 支付宝真实模式已启用 =====");
        log.info("  AppId: {}", properties.getAppId());
        log.info("  网关: {}", properties.getGatewayUrl());
        log.info("  签名类型: {}", properties.getSignType());

        // 初始化支付宝客户端 (普通公钥模式)
        this.alipayClient = new DefaultAlipayClient(
                properties.getGatewayUrl(),
                properties.getAppId(),
                properties.getPrivateKey(),
                properties.getFormat(),
                properties.getCharset(),
                properties.getAlipayPublicKey(),
                properties.getSignType());
        log.info("=====================================");
    }

    @Override
    public TradeResultVO createOrder(AliPayTradeReq req) {
        log.info("[ALIPAY] 收到下单请求: outTradeNo={}, tradeType={}", req.getOutTradeNo(), req.getTradeType());

        try {
            // 构造业务参数 (buyer_id 仅在当面付 SCAN 场景下传，WEB/WAP/APP 不支持该参数，
            // 否则支付宝会返回 INVALID_PARAMETER 错误)
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", req.getOutTradeNo());
            bizContent.put("subject", req.getSubject());
            bizContent.put("total_amount", req.getTotalAmount().toPlainString());
            if (req.getBody() != null) {
                bizContent.put("body", req.getBody());
            }
            if (req.getBuyerId() != null && req.getTradeType() == TradeTypeEnum.SCAN) {
                bizContent.put("buyer_id", req.getBuyerId());
            }
            // 超时时间 30 分钟
            bizContent.put("timeout_express", "30m");

            // 打印实际请求体，便于排查 INVALID_PARAMETER 错误
            log.info("[ALIPAY] bizContent={}", bizContent.toJSONString());

            String payUrl = null;
            String qrCodeContent = null;
            String prepayId = null;

            switch (req.getTradeType()) {
                case WEB: {
                    // PC 网站支付
                    // 关键: product_code 必须显式设置为 FAST_INSTANT_TRADE_PAY，
                    // 否则支付宝网关返回 INVALID_PARAMETER
                    bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
                    AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
                    request.setNotifyUrl(properties.getNotifyUrl());
                    request.setReturnUrl(properties.getReturnUrl());
                    request.setBizContent(bizContent.toJSONString());
                    AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
                    payUrl = response.getBody();
                    log.info("[ALIPAY] PC 网站支付下单成功: subCode={}, subMsg={}", response.getSubCode(), response.getSubMsg());
                    break;
                }
                case WAP: {
                    // 手机网站支付
                    // 关键: product_code 必须显式设置为 QUICK_WAP_WAY
                    bizContent.put("product_code", "QUICK_WAP_WAY");
                    AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
                    request.setNotifyUrl(properties.getNotifyUrl());
                    request.setReturnUrl(properties.getReturnUrl());
                    request.setBizContent(bizContent.toJSONString());
                    AlipayTradeWapPayResponse response = alipayClient.pageExecute(request);
                    payUrl = response.getBody();
                    log.info("[ALIPAY] 手机网站支付下单成功: subCode={}, subMsg={}", response.getSubCode(), response.getSubMsg());
                    break;
                }
                case SCAN: {
                    // 当面付扫码 (预下单: 生成二维码链接)
                    AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
                    request.setNotifyUrl(properties.getNotifyUrl());
                    request.setBizContent(bizContent.toJSONString());
                    AlipayTradePrecreateResponse response = alipayClient.execute(request);
                    if (response.isSuccess()) {
                        qrCodeContent = response.getQrCode();
                        log.info("[ALIPAY] 扫码支付下单成功: qrCode={}", qrCodeContent);
                    } else {
                        throw new RuntimeException("[ALIPAY] 扫码下单失败: " + response.getSubMsg());
                    }
                    break;
                }
                case APP: {
                    // APP 支付
                    AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
                    request.setNotifyUrl(properties.getNotifyUrl());
                    request.setBizContent(bizContent.toJSONString());
                    AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);
                    prepayId = response.getBody();
                    payUrl = response.getBody();
                    log.info("[ALIPAY] APP 支付下单成功");
                    break;
                }
                default:
                    throw new IllegalArgumentException("[ALIPAY] 不支持的交易类型: " + req.getTradeType());
            }

            return TradeResultVO.builder()
                    .outTradeNo(req.getOutTradeNo())
                    .payChannel(PayChannelEnum.ALIPAY)
                    .tradeType(req.getTradeType())
                    .prepayId(prepayId)
                    .payUrl(payUrl)
                    .qrCodeContent(qrCodeContent)
                    .mock(false)
                    .build();
        } catch (Exception e) {
            log.error("[ALIPAY] 下单失败", e);
            throw new RuntimeException("[ALIPAY] 下单失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PayNotifyResultVO parseNotify(HttpServletRequest request) {
        log.info("[ALIPAY] 收到支付宝异步回调通知");

        try {
            // 获取支付宝 POST 过来的反馈信息
            Map<String, String> params = convertRequestToMap(request);

            // 真实验签 (RSA2)
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    properties.getAlipayPublicKey(),
                    properties.getCharset(),
                    properties.getSignType());

            if (!signVerified) {
                log.error("[ALIPAY] 验签失败");
                return PayNotifyResultVO.builder()
                        .responseCode("fail")
                        .responseMessage("验签失败")
                        .payChannel(PayChannelEnum.ALIPAY)
                        .build();
            }

            // 验签通过后处理业务
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String totalAmountStr = params.get("total_amount");
            BigDecimal totalAmount = totalAmountStr != null ? new BigDecimal(totalAmountStr) : null;
            String gmtPayment = params.get("gmt_payment");

            LocalDateTime successTime = null;
            if (gmtPayment != null) {
                successTime = LocalDateTime.parse(gmtPayment, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            // 仅交易成功时更新订单状态
            TradeStatusEnum statusEnum = "TRADE_SUCCESS".equals(tradeStatus)
                    || "TRADE_FINISHED".equals(tradeStatus)
                    ? TradeStatusEnum.SUCCESS
                    : TradeStatusEnum.NOTPAY;

            log.info("[ALIPAY] 回调处理成功: outTradeNo={}, tradeNo={}, tradeStatus={}",
                    outTradeNo, tradeNo, tradeStatus);

            return PayNotifyResultVO.builder()
                    .responseCode("success")
                    .responseMessage("处理成功")
                    .payChannel(PayChannelEnum.ALIPAY)
                    .outTradeNo(outTradeNo)
                    .tradeNo(tradeNo)
                    .totalAmount(totalAmount)
                    .tradeStatus(statusEnum)
                    .successTime(successTime)
                    .build();
        } catch (Exception e) {
            log.error("[ALIPAY] 回调处理失败", e);
            return PayNotifyResultVO.builder()
                    .responseCode("fail")
                    .responseMessage("处理失败: " + e.getMessage())
                    .payChannel(PayChannelEnum.ALIPAY)
                    .build();
        }
    }

    /**
     * 将 HttpServletRequest 中的所有参数转为 Map<String, String>.
     */
    private Map<String, String> convertRequestToMap(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String[] values = entry.getValue();
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    valueStr.append(",");
                }
                valueStr.append(values[i]);
            }
            params.put(entry.getKey(), valueStr.toString());
        }
        return params;
    }
}
