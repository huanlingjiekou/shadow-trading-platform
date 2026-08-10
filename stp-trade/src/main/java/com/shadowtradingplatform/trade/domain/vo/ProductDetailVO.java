package com.shadowtradingplatform.trade.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品详情 VO.
 *
 * <p>继承自 {@link ProductItemVO}，额外增加详情页专属字段：</p>
 * <ul>
 *   <li>{@code images} - 商品图片列表</li>
 *   <li>{@code detail} - 富文本详情</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品详情")
public class ProductDetailVO extends ProductItemVO {

    @Schema(description = "商品图片URL列表", example = "[\"https://example.com/img1.jpg\", \"https://example.com/img2.jpg\"]")
    private List<String> images;

    @Schema(description = "富文本详情")
    private String detail;
}
