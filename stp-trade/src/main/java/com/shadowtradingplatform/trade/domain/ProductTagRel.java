package com.shadowtradingplatform.trade.domain;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProductTagRel {
    @Schema(description="商品ID")
    private Long product_id;
    @Schema(description="标签ID")
    private Long tag_id;
}
