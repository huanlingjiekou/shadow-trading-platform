package com.shadowtradingplatform.trade.pay.controller;

import com.shadowtradingplatform.trade.pay.domain.req.AliPayTradeReq;
import com.shadowtradingplatform.trade.pay.domain.vo.PayNotifyResultVO;
import com.shadowtradingplatform.trade.pay.domain.vo.TradeResultVO;
import com.shadowtradingplatform.trade.pay.service.AliPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付宝支付控制器.
 *
 * <p>提供支付宝下单与回调通知接收接口。
 * 根据 {@code alipay.mock-enabled} 配置自动切换 Mock/真实实现。</p>
 */
@Slf4j
@RestController
@RequestMapping("/alipay")
@RequiredArgsConstructor
@Tag(name = "支付宝支付", description = "支付宝下单与回调通知")
public class AliPayController {

    private final AliPayService aliPayService;

    /**
     * 创建支付宝订单.
     */
    @PostMapping("/order")
    @Operation(summary = "支付宝下单 (PC/WAP/SCAN/APP)")
    public TradeResultVO createOrder(@Valid @RequestBody AliPayTradeReq req) {
        log.info("收到支付宝下单请求: outTradeNo={}, tradeType={}", req.getOutTradeNo(), req.getTradeType());
        return aliPayService.createOrder(req);
    }

    /**
     * 支付宝异步回调通知.
     *
     * <p>支付宝会以 POST 表单方式回调此接口，返回 success 表示处理成功。</p>
     */
    @PostMapping("/notify")
    @Operation(summary = "支付宝异步回调通知")
    public String notify(HttpServletRequest request) {
        log.info("收到支付宝回调通知");
        PayNotifyResultVO result = aliPayService.parseNotify(request);
        log.info("支付宝回调处理结果: {}", result.getResponseCode());
        return result.getResponseCode();
    }

    /**
     * Mock 模式回调自测接口.
     *
     * <p>无需真实支付宝回调，直接返回模拟处理结果。</p>
     */
    @GetMapping("/mock/notify-test")
    @Operation(summary = "Mock模式支付宝回调自测")
    public PayNotifyResultVO mockNotifyTest() {
        log.info("触发支付宝 Mock 回调自测");
        return aliPayService.parseNotify(null);
    }
}
