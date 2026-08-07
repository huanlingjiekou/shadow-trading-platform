package com.shadowtradingplatform.trade.pay.service;

import com.shadowtradingplatform.trade.pay.domain.req.AliPayTradeReq;
import com.shadowtradingplatform.trade.pay.domain.vo.PayNotifyResultVO;
import com.shadowtradingplatform.trade.pay.domain.vo.TradeResultVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 支付宝支付服务接口.
 *
 * <p>定义支付宝下单与回调处理核心能力。
 * 真实实现 {@code AliPayRealService} 与 Mock 实现 {@code AliPayMockService} 均实现此接口。</p>
 */
public interface AliPayService {

    /**
     * 创建支付宝订单.
     *
     * @param req 下单请求
     * @return 下单结果 (含支付链接/表单/二维码内容)
     */
    TradeResultVO createOrder(AliPayTradeReq req);

    /**
     * 解析支付宝异步回调通知.
     *
     * @param request HttpServletRequest (含支付宝 POST 参数)
     * @return 回调处理结果
     */
    PayNotifyResultVO parseNotify(HttpServletRequest request);
}
