package com.shadowtradingplatform.trade.pay.domain.pojo;

import lombok.Data;

/**
 * 微信支付 H5 场景信息.
 */
@Data
public class WxPaySceneInfo {

    /**
     * 客户端 IP.
     */
    private String payerClientIp;

    /**
     * H5 支付类型信息.
     */
    private WxPayH5Info h5Info;
}
