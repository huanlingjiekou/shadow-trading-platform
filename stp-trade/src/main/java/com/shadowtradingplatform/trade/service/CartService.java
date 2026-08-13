package com.shadowtradingplatform.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shadowtradingplatform.trade.domain.po.Cart;
import com.shadowtradingplatform.trade.domain.vo.CartItemVO;

import java.util.List;

/**
* @author huanlingjiekou
* @description 针对表【cart(购物车表)】的数据库操作Service
* @createDate 2026-08-09 16:08:29
*/
public interface CartService extends IService<Cart> {

    /**
     * 查询当前用户购物车列表（含商品信息 + 选中状态）.
     */
    List<CartItemVO> loadCartList();

    /**
     * 添加商品到购物车，返回刷新后的列表.
     */
    List<CartItemVO> addToCart(Long productId, Integer quantity);

    /**
     * 更新购物车项数量，返回更新后的列表.
     */
    List<CartItemVO> updateCart(Long id, Integer quantity);

    /**
     * 移除购物车项，返回更新后的列表.
     */
    List<CartItemVO> removeFromCart(Long id);

    /**
     * 切换购物车项选中状态（用于结算），返回更新后的列表.
     */
    List<CartItemVO> toggleSelect(Long id);

    /**
     * 全选 / 全不选购物车项，返回更新后的列表.
     */
    List<CartItemVO> selectAll();
}
