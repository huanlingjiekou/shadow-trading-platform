package com.shadowtradingplatform.trade.domain;


import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class Cart {
    @Schema(description="购物车项ID")
    private Long id;
    @Schema(description="用户ID")
    private Long user_id;
    @Schema(description="商品ID")
    private Long product_id;
    @Schema(description="数量")
    private Integer quantity;
    @Schema(description="是否选中：0=否 1=是")
    private Integer selected;
    @Schema(description="创建时间")
    private LocalDateTime create_time;
    @Schema(description="更新时间")
    private LocalDateTime update_time;
}
