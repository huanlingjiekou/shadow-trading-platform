package com.shadowtradingplatform.trade.pay.domain.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付回调通知日志持久化对象.
 *
 * <p>对应数据库表 {@code t_pay_notify_log}，记录每次支付平台回调的原始报文与处理结果，
 * 用于审计、对账与问题排查。</p>
 */
@Data
@TableName("t_pay_notify_log")
public class PayNotifyLogPO {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 商户订单号 */
    @TableField("out_trade_no")
    private String outTradeNo;

    /** 第三方平台交易号 */
    @TableField("trade_no")
    private String tradeNo;

    /** 支付渠道 (ALIPAY/WXPAY) */
    @TableField("pay_channel")
    private String payChannel;

    /** 验签结果 (0=失败, 1=成功) */
    @TableField("verify_result")
    private Integer verifyResult;

    /** 处理结果 (0=失败, 1=成功) */
    @TableField("process_result")
    private Integer processResult;

    /** 失败原因 */
    @TableField("fail_reason")
    private String failReason;

    /** 原始回调报文 */
    @TableField("raw_notify_data")
    private String rawNotifyData;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
