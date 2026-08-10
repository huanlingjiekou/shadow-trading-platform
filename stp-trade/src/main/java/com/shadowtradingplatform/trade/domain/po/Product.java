package com.shadowtradingplatform.trade.domain.po;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class Product {
    @Schema(description="商品ID")
    private Long id;
    @Schema(description="商品名称")
    private String name;
    @Schema(description="副标题")
    private String subtitle;
    @Schema(description="售价")
    private BigDecimal price;
    @Schema(description="原价")
    private BigDecimal original_price;
    @Schema(description="主图URL")
    private String image;
    @Schema(description="分类：digital/accessory/audio")
    private String category;
    @Schema(description="销量")
    private Integer sales;
    @Schema(description="库存")
    private Integer stock;
    @Schema(description="简述")
    private String description;
    @Schema(description="富文本详情")
    private String detail;
    @Schema(description="上架状态：0=下架 1=上架")
    private Integer status;
    @Schema(description="创建时间")
    private LocalDateTime create_time;
    @Schema(description="更新时间")
    private LocalDateTime update_time;
}
