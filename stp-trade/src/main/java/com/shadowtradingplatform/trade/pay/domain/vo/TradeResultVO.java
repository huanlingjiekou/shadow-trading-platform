package com.shadowtradingplatform.trade.pay.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.shadowtradingplatform.trade.config.CamelCaseNamingStrategy;
import com.shadowtradingplatform.trade.pay.domain.enums.PayChannelEnum;
import com.shadowtradingplatform.trade.pay.domain.enums.TradeTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 下单结果视图对象.
 *
 * <p>返回给前端调起支付所需的关键参数。</p>
 *
 * <p>使用 {@link JsonNaming} 覆盖全局 SNAKE_CASE 策略为 CamelCaseNamingStrategy，
 * 确保返回前端的 JSON 字段为驼峰命名 (如 outTradeNo 而非 out_trade_no)。</p>
 */
@Data
@Builder
@Schema(description = "下单结果")
@JsonNaming(CamelCaseNamingStrategy.class)
public class TradeResultVO {

    @Schema(description = "商户订单号")
    private String outTradeNo;

    @Schema(description = "支付渠道")
    private PayChannelEnum payChannel;

    @Schema(description = "交易类型")
    private TradeTypeEnum tradeType;

    @Schema(description = "预支付ID (微信 prepay_id, 支付宝为空)")
    private String prepayId;

    @Schema(description = "支付链接/表单 (支付宝 PC 网站支付 form、微信 H5 mweb_url、扫码 code_url)")
    private String payUrl;

    @Schema(description = "JSAPI 调起支付所需的签名参数 (仅微信 JSAPI)", example = "{\"appId\":\"\",\"timeStamp\":\"\",\"nonceStr\":\"\",\"package\":\"\",\"signType\":\"\",\"paySign\":\"\"}")
    private String jsapiPayParams;

    @Schema(description = "二维码内容 (扫码支付用, 前端可基于此生成二维码图片)")
    private String qrCodeContent;

    @Schema(description = "是否为 Mock 模拟数据")
    private boolean mock;
}
