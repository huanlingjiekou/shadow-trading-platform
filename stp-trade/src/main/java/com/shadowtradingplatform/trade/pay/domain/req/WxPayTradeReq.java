package com.shadowtradingplatform.trade.pay.domain.req;

import com.shadowtradingplatform.trade.pay.domain.enums.TradeTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 微信支付下单请求对象.
 *
 * <p>接收前端传入的下单参数，用于微信 JSAPI/NATIVE/APP/H5 支付。
 * 金额单位为分，与微信支付接口一致。</p>
 */
@Data
@Schema(description = "微信支付下单请求")
public class WxPayTradeReq {

    @Schema(description = "商户订单号 (商户自定义，唯一)", example = "WX202608050001")
    @NotBlank(message = "订单号不能为空")
    private String outTradeNo;

    @Schema(description = "商品描述", example = "Shadow 平台会员服务")
    @NotBlank(message = "商品描述不能为空")
    private String description;

    @Schema(description = "订单金额 (分)", example = "1")
    @NotNull(message = "订单金额不能为空")
    @Min(value = 1, message = "订单金额最小 1 分")
    private Integer totalFee;

    @Schema(description = "交易类型", example = "NATIVE", allowableValues = {"JSAPI", "NATIVE", "APP", "H5"})
    @NotNull(message = "交易类型不能为空")
    private TradeTypeEnum tradeType;

    @Schema(description = "买家openid (JSAPI 必填)", example = "oUpF8uMuAJO_M2pxb1Q9fNjWeS6o")
    private String openid;

    @Schema(description = "终端IP", example = "127.0.0.1")
    private String clientIp;
}
