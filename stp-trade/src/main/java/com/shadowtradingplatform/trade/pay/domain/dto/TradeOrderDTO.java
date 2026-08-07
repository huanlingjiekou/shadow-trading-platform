package com.shadowtradingplatform.trade.pay.domain.dto;

import com.shadowtradingplatform.trade.pay.domain.enums.PayChannelEnum;
import com.shadowtradingplatform.trade.pay.domain.enums.TradeStatusEnum;
import com.shadowtradingplatform.trade.pay.domain.enums.TradeTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易订单数据传输对象.
 *
 * <p>用于 Service 层与持久层之间的订单数据传递。</p>
 */
@Data
@Builder
public class TradeOrderDTO {

    /** 主键 */
    private Long id;

    /** 商户订单号 */
    private String outTradeNo;

    /** 支付渠道 */
    private PayChannelEnum payChannel;

    /** 交易类型 */
    private TradeTypeEnum tradeType;

    /** 订单标题/商品描述 */
    private String subject;

    /** 订单金额 (元, 支付宝用 BigDecimal, 微信用 totalFee 分) */
    private BigDecimal totalAmount;

    /** 订单金额 (分, 微信用) */
    private Integer totalFee;

    /** 第三方平台交易号 (微信 transaction_id / 支付宝 trade_no) */
    private String tradeNo;

    /** 买家标识 (支付宝 buyer_id / 微信 openid) */
    private String buyerId;

    /** 交易状态 */
    private TradeStatusEnum tradeStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 支付完成时间 */
    private LocalDateTime successTime;
}
