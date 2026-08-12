package com.shadowtradingplatform.trade.domain.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新购物车数量请求参数.
 */
@Data
@Schema(description = "更新购物车数量请求")
public class CartUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "购物车项ID不能为空")
    @Schema(description = "购物车项ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Schema(description = "新的购买数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer quantity;
}
