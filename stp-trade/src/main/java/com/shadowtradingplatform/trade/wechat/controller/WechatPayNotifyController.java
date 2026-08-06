package com.shadowtradingplatform.trade.wechat.controller;

import com.shadowtradingplatform.trade.wechat.dto.NotifyDecryptResult;
import com.shadowtradingplatform.trade.wechat.dto.PrepayRequest;
import com.shadowtradingplatform.trade.wechat.dto.PrepayResponse;
import com.shadowtradingplatform.trade.wechat.service.WechatPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付回调通知与测试接口.
 */
@Slf4j
@RestController
@RequestMapping("/wechat/pay")
@RequiredArgsConstructor
@Tag(name = "微信支付", description = "微信支付统一下单与回调通知")
public class WechatPayNotifyController {

    private final WechatPayService wechatPayService;

    /**
     * 微信支付回调通知接收.
     */
    @PostMapping("/notify")
    @Operation(summary = "接收微信支付回调通知")
    public Map<String, Object> notify(
            @RequestBody String body,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Serial") String serial,
            @RequestHeader("Wechatpay-Signature") String signature) {

        log.info("收到微信支付回调: serial={}", serial);
        NotifyDecryptResult result = wechatPayService.parseNotify(body, timestamp, nonce, serial, signature);

        Map<String, Object> response = new HashMap<>();
        response.put("code", "SUCCESS");
        response.put("message", "成功");
        response.put("data", result);
        return response;
    }

    /**
     * 统一下单 (生成 prepay_id).
     */
    @PostMapping("/order")
    @Operation(summary = "统一下单")
    public PrepayResponse createOrder(@RequestBody PrepayRequest request) {
        log.info("收到下单请求: outTradeNo={}", request.getOutTradeNo());
        return wechatPayService.createOrder(request);
    }

    /**
     * Mock 模式回调自测接口.
     *
     * <p>当 MOCK_ENABLED=true 时，调用此接口会构造模拟加密回调报文，
     * 执行完整的签名 → 验签 → 加密 → 解密链路测试。</p>
     */
    @GetMapping("/mock/notify-test")
    @Operation(summary = "Mock模式回调自测")
    public Map<String, Object> mockNotifyTest() {
        log.info("触发 Mock 回调自测");
        NotifyDecryptResult result = wechatPayService.parseNotify(null, null, null, null, null);

        Map<String, Object> response = new HashMap<>();
        response.put("code", "SUCCESS");
        response.put("message", "Mock 回调自测通过，签名/验签/加解密链路正常");
        response.put("data", result);
        return response;
    }
}
