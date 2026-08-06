package com.shadowtradingplatform.trade.wechat.service;

import com.shadowtradingplatform.trade.wechat.dto.NotifyDecryptResult;
import com.shadowtradingplatform.trade.wechat.dto.PrepayRequest;
import com.shadowtradingplatform.trade.wechat.dto.PrepayResponse;

/**
 * 微信支付核心服务接口.
 *
 * <p>定义统一下单与回调通知处理两大核心能力。
 * 真实实现与 Mock 实现均实现此接口，Mock 模式下仍执行底层签名与加解密逻辑。</p>
 */
public interface WechatPayService {

    /**
     * 统一下单 - 生成 prepay_id.
     *
     * @param request 下单请求参数
     * @return 下单响应（含 prepay_id）
     */
    PrepayResponse createOrder(PrepayRequest request);

    /**
     * 解析支付回调通知 - 验签 + AES-GCM 解密.
     *
     * @param body      回调报文主体 (JSON)
     * @param timestamp Wechatpay-Timestamp 请求头
     * @param nonce     Wechatpay-Nonce 请求头
     * @param serial    Wechatpay-Serial 请求头
     * @param signature Wechatpay-Signature 请求头
     * @return 解密后的交易结果
     */
    NotifyDecryptResult parseNotify(String body, String timestamp, String nonce, String serial, String signature);
}
