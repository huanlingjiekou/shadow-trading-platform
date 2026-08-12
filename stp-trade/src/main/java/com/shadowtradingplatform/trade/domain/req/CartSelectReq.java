package com.shadowtradingplatform.trade.domain.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 切换购物车选中状态请求参数.
 */
@Data
@Schema(description = "切换购物车选中状态请求")
public class CartSelectReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "购物车项ID不能为空")
    @Schema(description = "购物车项ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;
}
