package com.shadowtradingplatform.trade.controller;

import com.shadowtradingplatform.trade.common.R;
import com.shadowtradingplatform.trade.context.UserContext;
import com.shadowtradingplatform.trade.domain.req.CartAddReq;
import com.shadowtradingplatform.trade.domain.req.CartRemoveReq;
import com.shadowtradingplatform.trade.domain.req.CartSelectReq;
import com.shadowtradingplatform.trade.domain.req.CartUpdateReq;
import com.shadowtradingplatform.trade.domain.vo.CartItemVO;
import com.shadowtradingplatform.trade.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车控制器.
 *
 * <p>所有操作均依赖 {@link UserContext} 中的当前登录用户，
 * 由 {@code AuthInterceptor} 前置写入。</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "购物车模块", description = "购物车增删改查、选中管理")
public class CartController {

    private final CartService cartService;

    /**
     * 查询购物车列表.
     */
    @GetMapping("/cart/list")
    @Operation(summary = "查询购物车列表")
    public R<List<CartItemVO>> list() {
        if (!UserContext.isLogin()) {
            return R.fail("未登录");
        }
        return R.success(cartService.loadCartList());
    }

    /**
     * 添加商品到购物车.
     */
    @PostMapping("/cart/add")
    @Operation(summary = "添加商品到购物车")
    public R<List<CartItemVO>> add(@Valid @RequestBody CartAddReq req) {
        if (!UserContext.isLogin()) {
            return R.fail("未登录");
        }
        return R.success(cartService.addToCart(req.getProductId(), req.getQuantity()));
    }

    /**
     * 更新购物车项数量.
     */
    @PostMapping("/cart/update")
    @Operation(summary = "更新购物车项数量")
    public R<List<CartItemVO>> update(@Valid @RequestBody CartUpdateReq req) {
        if (!UserContext.isLogin()) {
            return R.fail("未登录");
        }
        return R.success(cartService.updateCart(req.getId(), req.getQuantity()));
    }

    /**
     * 移除购物车项.
     */
    @PostMapping("/cart/remove")
    @Operation(summary = "移除购物车项")
    public R<List<CartItemVO>> remove(@Valid @RequestBody CartRemoveReq req) {
        if (!UserContext.isLogin()) {
            return R.fail("未登录");
        }
        return R.success(cartService.removeFromCart(req.getId()));
    }

    /**
     * 切换单个购物车项选中状态（用于结算）.
     */
    @PostMapping("/cart/select")
    @Operation(summary = "切换购物车项选中状态")
    public R<List<CartItemVO>> select(@Valid @RequestBody CartSelectReq req) {
        if (!UserContext.isLogin()) {
            return R.fail("未登录");
        }
        return R.success(cartService.toggleSelect(req.getId()));
    }

    /**
     * 全选 / 全不选购物车项.
     */
    @PostMapping("/cart/selectAll")
    @Operation(summary = "全选/全不选购物车项")
    public R<List<CartItemVO>> selectAll() {
        if (!UserContext.isLogin()) {
            return R.fail("未登录");
        }
        return R.success(cartService.selectAll());
    }
}
