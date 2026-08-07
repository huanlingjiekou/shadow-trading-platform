package com.shadowtradingplatform.trade.pay.domain.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易订单持久化对象.
 *
 * <p>对应数据库表 {@code t_trade_order}，记录每笔下单请求与支付状态。
 * 供 MyBatis-Plus 自动映射使用。</p>
 */
@Data
@TableName("t_trade_order")
public class TradeOrderPO {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 商户订单号 (唯一) */
    @TableField("out_trade_no")
    private String outTradeNo;

    /** 支付渠道 (ALIPAY/WXPAY) */
    @TableField("pay_channel")
    private String payChannel;

    /** 交易类型 (WEB/WAP/SCAN/APP/JSAPI/NATIVE/H5) */
    @TableField("trade_type")
    private String tradeType;

    /** 订单标题/商品描述 */
    @TableField("subject")
    private String subject;

    /** 订单金额 (元) */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /** 订单金额 (分, 微信专用) */
    @TableField("total_fee")
    private Integer totalFee;

    /** 第三方平台交易号 */
    @TableField("trade_no")
    private String tradeNo;

    /** 买家标识 (支付宝 buyer_id / 微信 openid) */
    @TableField("buyer_id")
    private String buyerId;

    /** 交易状态 (NOTPAY/SUCCESS/CLOSED/REFUND/PAYERROR) */
    @TableField("trade_status")
    private String tradeStatus;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 支付完成时间 */
    @TableField("success_time")
    private LocalDateTime successTime;

    /** 逻辑删除标记 */
    @TableField("deleted")
    private Integer deleted;
}
