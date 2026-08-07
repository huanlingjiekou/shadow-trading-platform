package com.shadowtradingplatform.trade.pay.service.impl;

import com.shadowtradingplatform.trade.config.AliPayProperties;
import com.shadowtradingplatform.trade.pay.domain.enums.PayChannelEnum;
import com.shadowtradingplatform.trade.pay.domain.enums.TradeStatusEnum;
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
import java.util.UUID;

/**
 * 支付宝 Mock 实现.
 *
 * <p>当 {@code alipay.mock-enabled=true} (环境变量 ALIPAY_MOCK_ENABLED=true) 时激活。</p>
 *
 * <p>核心特点：不向支付宝网关发送真实 HTTP 请求，构造模拟下单结果返回。
 * 适用于未申请商户号时的本地开发与联调。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alipay.mock-enabled", havingValue = "true")
public class AliPayMockService implements AliPayService {

    private final AliPayProperties properties;

    @PostConstruct
    public void init() {
        log.warn("===== 支付宝 Mock 模式已启用 =====");
        log.warn("  网络请求: 已禁用 (返回模拟下单结果)");
        log.warn("  商户 AppId: {}", hasValue(properties.getAppId()) ? properties.getAppId() : "(未配置,使用模拟值)");
        log.warn("  网关: {}", hasValue(properties.getGatewayUrl()) ? properties.getGatewayUrl() : "(未配置)");
        log.warn("====================================");
    }

    @Override
    public TradeResultVO createOrder(AliPayTradeReq req) {
        log.info("[MOCK-ALIPAY] 收到下单请求: outTradeNo={}, subject={}, amount={}, tradeType={}",
                req.getOutTradeNo(), req.getSubject(), req.getTotalAmount(), req.getTradeType());

        // 构造模拟下单结果
        String mockPrepayId = "mock_alipay_" + System.currentTimeMillis();
        String mockTradeNo = "mock_trade_no_" + System.currentTimeMillis();

        // 按交易类型生成不同的模拟支付参数
        String payUrl;
        String qrCodeContent = null;

        switch (req.getTradeType()) {
            case WEB:
                // PC 网站支付: 返回模拟的 form HTML
                payUrl = buildMockFormHtml(req, mockTradeNo);
                break;
            case WAP:
                // 手机网站支付: 返回模拟支付链接
                payUrl = "https://mock.alipay.com/wap/pay?out_trade_no=" + req.getOutTradeNo()
                        + "&total_amount=" + req.getTotalAmount();
                break;
            case SCAN:
                // 扫码支付: 返回二维码内容 (前端用此内容生成二维码图片)
                qrCodeContent = "https://mock.alipay.com/scan/pay?out_trade_no=" + req.getOutTradeNo()
                        + "&trade_no=" + mockTradeNo;
                payUrl = null;
                break;
            case APP:
                // APP 支付: 返回模拟的调起参数字符串
                payUrl = buildMockAppPayParams(req, mockTradeNo);
                break;
            default:
                payUrl = "https://mock.alipay.com/pay?out_trade_no=" + req.getOutTradeNo();
        }

        log.info("[MOCK-ALIPAY] 模拟下单成功: tradeNo={}, prepayId={}", mockTradeNo, mockPrepayId);
        log.info("[MOCK-ALIPAY] 支付链接/参数: {}", payUrl != null ? payUrl : qrCodeContent);

        return TradeResultVO.builder()
                .outTradeNo(req.getOutTradeNo())
                .payChannel(PayChannelEnum.ALIPAY)
                .tradeType(req.getTradeType())
                .prepayId(mockPrepayId)
                .payUrl(payUrl)
                .qrCodeContent(qrCodeContent)
                .mock(true)
                .build();
    }

    @Override
    public PayNotifyResultVO parseNotify(HttpServletRequest request) {
        log.info("[MOCK-ALIPAY] 收到模拟支付宝回调通知");

        // 构造模拟回调结果
        String mockOutTradeNo = "MOCK_OUT_TRADE_NO_" + System.currentTimeMillis();
        String mockTradeNo = "mock_trade_no_" + System.currentTimeMillis();

        log.info("[MOCK-ALIPAY] 模拟回调处理成功: outTradeNo={}, tradeNo={}", mockOutTradeNo, mockTradeNo);

        return PayNotifyResultVO.builder()
                .responseCode("success")
                .responseMessage("Mock 支付宝回调处理成功")
                .payChannel(PayChannelEnum.ALIPAY)
                .outTradeNo(mockOutTradeNo)
                .tradeNo(mockTradeNo)
                .totalAmount(new BigDecimal("0.01"))
                .tradeStatus(TradeStatusEnum.SUCCESS)
                .successTime(LocalDateTime.now())
                .build();
    }

    /**
     * 构造模拟的支付宝 PC 网站支付 form HTML.
     */
    private String buildMockFormHtml(AliPayTradeReq req, String tradeNo) {
        return "<form name='mock_alipay_form' action='https://mock.alipay.com/gateway.do' method='POST'>"
                + "<input type='hidden' name='out_trade_no' value='" + req.getOutTradeNo() + "'/>"
                + "<input type='hidden' name='trade_no' value='" + tradeNo + "'/>"
                + "<input type='hidden' name='total_amount' value='" + req.getTotalAmount() + "'/>"
                + "<input type='hidden' name='subject' value='" + req.getSubject() + "'/>"
                + "<input type='submit' value='前往 Mock 支付宝支付'/>"
                + "</form>";
    }

    /**
     * 构造模拟的 APP 支付调起参数.
     */
    private String buildMockAppPayParams(AliPayTradeReq req, String tradeNo) {
        Map<String, String> params = new HashMap<>();
        params.put("app_id", hasValue(properties.getAppId()) ? properties.getAppId() : "2021000MOCK0000000");
        params.put("out_trade_no", req.getOutTradeNo());
        params.put("trade_no", tradeNo);
        params.put("total_amount", req.getTotalAmount().toPlainString());
        params.put("subject", req.getSubject());
        params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("mock_nonce", UUID.randomUUID().toString().replace("-", ""));
        return params.toString();
    }

    private boolean hasValue(String value) {
        return value != null && !value.isEmpty();
    }
}
