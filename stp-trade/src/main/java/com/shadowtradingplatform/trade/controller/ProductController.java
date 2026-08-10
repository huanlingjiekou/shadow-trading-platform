package com.shadowtradingplatform.trade.controller;

import com.shadowtradingplatform.trade.common.PageResult;
import com.shadowtradingplatform.trade.common.R;
import com.shadowtradingplatform.trade.domain.dto.ProductQueryDTO;
import com.shadowtradingplatform.trade.domain.vo.ProductDetailVO;
import com.shadowtradingplatform.trade.domain.vo.ProductItemVO;
import com.shadowtradingplatform.trade.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品控制器.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "商品模块", description = "商品列表/详情查询")
public class ProductController {

    private final ProductService productService;

    /**
     * 分页查询商品列表.
     */
    @GetMapping("/products")
    @Operation(summary = "分页查询商品列表")
    public R<PageResult<ProductItemVO>> list(ProductQueryDTO query) {
        return R.success(productService.loadProductPage(query));
    }

    /**
     * 查询商品详情.
     */
    @GetMapping("/product")
    @Operation(summary = "查询商品详情")
    public R<ProductDetailVO> detail(@RequestParam Long id) {
        ProductDetailVO detail = productService.loadProductDetail(id);
        if (detail == null) {
            return R.fail("商品不存在");
        }
        return R.success(detail);
    }
}
