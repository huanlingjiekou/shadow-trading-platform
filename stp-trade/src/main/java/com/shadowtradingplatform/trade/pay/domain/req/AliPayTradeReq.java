package com.shadowtradingplatform.trade.pay.domain.req;

import com.shadowtradingplatform.trade.pay.domain.enums.TradeTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付宝下单请求对象.
 *
 * <p>接收前端传入的下单参数，用于支付宝 Web/WAP/Scan/APP 支付。</p>
 */
@Data
@Schema(description = "支付宝下单请求")
public class AliPayTradeReq {

    @Schema(description = "商户订单号 (商户自定义，唯一)", example = "ALI202608050001")
    @NotBlank(message = "订单号不能为空")
    private String outTradeNo;

    @Schema(description = "订单标题", example = "Shadow 平台会员服务")
    @NotBlank(message = "订单标题不能为空")
    private String subject;

    @Schema(description = "订单金额 (元)", example = "0.01")
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额最小 0.01 元")
    private BigDecimal totalAmount;

    @Schema(description = "商品描述", example = "会员服务 1 个月")
    private String body;

    @Schema(description = "交易类型", example = "WEB", allowableValues = {"WEB", "WAP", "SCAN", "APP"})
    @NotNull(message = "交易类型不能为空")
    private TradeTypeEnum tradeType;

    @Schema(description = "买家支付宝用户ID (当面付预下单时可空)", example = "2088102146225135")
    private String buyerId;
}
