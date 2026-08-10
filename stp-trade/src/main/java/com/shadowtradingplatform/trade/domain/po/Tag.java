package com.shadowtradingplatform.trade.domain.po;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class Tag {
    @Schema(description="标签ID")
    private Long id;
    @Schema(description="标签名")
    private String name;
}
