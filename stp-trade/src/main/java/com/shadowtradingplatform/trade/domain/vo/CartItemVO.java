package com.shadowtradingplatform.trade.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车项 VO.
 */
@Data
@Schema(description = "购物车项")
public class CartItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车项ID", example = "1")
    private Long id;

    @Schema(description = "商品ID", example = "10")
    private Long productId;

    @Schema(description = "商品名称", example = "智能手机")
    private String name;

    @Schema(description = "商品主图URL")
    private String image;

    @Schema(description = "商品单价", example = "2999.00")
    private BigDecimal price;

    @Schema(description = "购买数量", example = "2")
    private Integer quantity;

    @Schema(description = "商品库存", example = "100")
    private Integer stock;

    @Schema(description = "是否选中（用于结算）", example = "true")
    private Boolean selected;
}
