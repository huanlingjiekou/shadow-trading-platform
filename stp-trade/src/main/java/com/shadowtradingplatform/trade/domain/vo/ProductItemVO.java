package com.shadowtradingplatform.trade.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品列表项 VO.
 */
@Data
@Schema(description = "商品列表项")
public class ProductItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID", example = "1")
    private Long id;

    @Schema(description = "商品名称", example = "智能手机")
    private String name;

    @Schema(description = "副标题", example = "旗舰配置，超长续航")
    private String subtitle;

    @Schema(description = "售价", example = "2999.00")
    private BigDecimal price;

    @Schema(description = "原价", example = "3999.00")
    private BigDecimal originalPrice;

    @Schema(description = "主图URL")
    private String image;

    @Schema(description = "分类：digital/accessory/audio", example = "digital")
    private String category;

    @Schema(description = "销量", example = "520")
    private Integer sales;

    @Schema(description = "库存", example = "100")
    private Integer stock;

    @Schema(description = "简述")
    private String description;

    @Schema(description = "标签列表")
    private List<String> tags;
}
