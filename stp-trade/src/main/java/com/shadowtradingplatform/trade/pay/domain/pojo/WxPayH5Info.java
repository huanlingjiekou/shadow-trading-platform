package com.shadowtradingplatform.trade.pay.domain.pojo;

import lombok.Data;

/**
 * 微信支付 H5 类型信息.
 */
@Data
public class WxPayH5Info {

    /**
     * 场景类型 (Wap - 手机网站).
     */
    private String type;
}
