package com.shadowtradingplatform.trade.pay.controller;

import com.shadowtradingplatform.trade.pay.domain.req.WxPayTradeReq;
import com.shadowtradingplatform.trade.pay.domain.vo.PayNotifyResultVO;
import com.shadowtradingplatform.trade.pay.domain.vo.TradeResultVO;
import com.shadowtradingplatform.trade.pay.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信支付控制器.
 *
 * <p>提供微信支付下单与回调通知接收接口。
 * 根据 {@code wechat.pay.mock-enabled} 配置自动切换 Mock/真实实现。</p>
 */
@Slf4j
@RestController
@RequestMapping("/wxpay")
@RequiredArgsConstructor
@Tag(name = "微信支付", description = "微信支付下单与回调通知")
public class WxPayController {

    private final WxPayService wxPayService;

    /**
     * 创建微信支付订单.
     */
    @PostMapping("/order")
    @Operation(summary = "微信支付下单 (JSAPI/NATIVE/APP/H5)")
    public TradeResultVO createOrder(@Valid @RequestBody WxPayTradeReq req) {
        log.info("收到微信支付下单请求: outTradeNo={}, tradeType={}", req.getOutTradeNo(), req.getTradeType());

        return wxPayService.createOrder(req);
    }

    /**
     * 微信支付回调通知.
     *
     * <p>微信会以 POST JSON 方式回调此接口，需返回 SUCCESS/FAIL。</p>
     */
    @PostMapping("/notify")
    @Operation(summary = "微信支付回调通知")
    public String notify(
            @RequestBody String body,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Serial") String serial,
            @RequestHeader("Wechatpay-Signature") String signature) {

        log.info("收到微信支付回调通知: serial={}", serial);
        PayNotifyResultVO result = wxPayService.parseNotify(body, timestamp, nonce, serial, signature);
        log.info("微信支付回调处理结果: {}", result.getResponseCode());
        return result.getResponseCode();
    }

    /**
     * Mock 模式回调自测接口.
     *
     * <p>无需真实微信回调，直接返回模拟处理结果。</p>
     */
    @GetMapping("/mock/notify-test")
    @Operation(summary = "Mock模式微信支付回调自测")
    public PayNotifyResultVO mockNotifyTest() {
        log.info("触发微信支付 Mock 回调自测");
        return wxPayService.parseNotify(null, null, null, null, null);
    }
}
