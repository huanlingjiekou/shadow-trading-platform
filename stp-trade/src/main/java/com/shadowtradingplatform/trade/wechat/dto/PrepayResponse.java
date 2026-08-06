package com.shadowtradingplatform.trade.wechat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一下单响应.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrepayResponse {

    /** 预支付交易会话标识 (prepay_id) */
    private String prepayId;

    /** 支付二维码链接 (Native 下单返回) */
    private String codeUrl;

    /** 是否为 Mock 模式生成 */
    private boolean mock;
}
