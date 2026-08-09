package com.shadowtradingplatform.trade.domain;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProductImage {
    @Schema(description="主键")
    private Long id;
    @Schema(description="商品ID")
    private Long product_id;
    @Schema(description="图片URL")
    private String image_url;
    @Schema(description="排序（升序）")
    private Integer sort_order;
}
