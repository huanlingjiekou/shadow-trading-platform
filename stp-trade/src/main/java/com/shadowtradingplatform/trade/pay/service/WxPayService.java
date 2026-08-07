package com.shadowtradingplatform.trade.pay.service;

import com.shadowtradingplatform.trade.pay.domain.req.WxPayTradeReq;
import com.shadowtradingplatform.trade.pay.domain.vo.PayNotifyResultVO;
import com.shadowtradingplatform.trade.pay.domain.vo.TradeResultVO;

/**
 * 微信支付服务接口.
 *
 * <p>定义微信下单与回调处理核心能力。
 * 真实实现 {@code WxPayRealService} 与 Mock 实现 {@code WxPayMockService} 均实现此接口。</p>
 */
public interface WxPayService {

    /**
     * 创建微信支付订单.
     *
     * @param req 下单请求
     * @return 下单结果 (含 prepay_id / code_url / mweb_url / jsapi 参数)
     */
    TradeResultVO createOrder(WxPayTradeReq req);

    /**
     * 解析微信支付回调通知.
     *
     * @param body      回调报文主体 (JSON)
     * @param timestamp Wechatpay-Timestamp 请求头
     * @param nonce     Wechatpay-Nonce 请求头
     * @param serial    Wechatpay-Serial 请求头
     * @param signature Wechatpay-Signature 请求头
     * @return 回调处理结果
     */
    PayNotifyResultVO parseNotify(String body, String timestamp, String nonce, String serial, String signature);
}
