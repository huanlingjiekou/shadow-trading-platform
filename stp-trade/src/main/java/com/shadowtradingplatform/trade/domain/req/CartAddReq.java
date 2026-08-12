package com.shadowtradingplatform.trade.domain.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 添加购物车请求参数.
 */
@Data
@Schema(description = "添加购物车请求")
public class CartAddReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long productId;

    @Min(value = 1, message = "数量至少为1")
    @Schema(description = "购买数量，默认1", example = "1")
    private Integer quantity = 1;
}
