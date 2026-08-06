package com.shadowtradingplatform.trade.wechat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单金额信息.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Amount {

    /** 总金额 (单位: 分) */
    private Integer total;

    /** 货币类型 (CNY) */
    private String currency;
}
