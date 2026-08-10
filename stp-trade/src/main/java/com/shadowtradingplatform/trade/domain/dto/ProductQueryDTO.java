package com.shadowtradingplatform.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品列表分页查询参数.
 */
@Data
@Schema(description = "商品列表查询参数")
public class ProductQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码，从 1 开始", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "20")
    private Integer pageSize = 20;

    @Schema(description = "分类（可空，为空则查全部）", example = "digital")
    private String category;
}
