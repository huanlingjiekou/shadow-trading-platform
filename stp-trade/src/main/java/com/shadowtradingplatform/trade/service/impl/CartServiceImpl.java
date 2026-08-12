package com.shadowtradingplatform.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shadowtradingplatform.trade.context.UserContext;
import com.shadowtradingplatform.trade.domain.po.Cart;
import com.shadowtradingplatform.trade.domain.po.Product;
import com.shadowtradingplatform.trade.domain.vo.CartItemVO;
import com.shadowtradingplatform.trade.mapper.CartMapper;
import com.shadowtradingplatform.trade.service.CartService;
import com.shadowtradingplatform.trade.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 购物车服务实现.
 *
 * <p>选中状态采用 <b>DB 持久化 + Redis Set 加速读取</b> 的双重方案：</p>
 * <ul>
 *   <li>DB {@code cart.selected} 字段做持久化，保证重启不丢失</li>
 *   <li>Redis Set {@code stp:cart:selected:{userId}} 缓存选中购物车项 ID，
 *       结算时可直接读取 Set 而无需扫描 DB</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart>
    implements CartService {

    /** Redis 中购物车选中集合键前缀，拼接 userId */
    private static final String REDIS_CART_SELECTED_PREFIX = "stp:cart:selected:";

    private final ProductService productService;
    private final RedissonClient redissonClient;

    // ==================== 业务方法 ====================

    @Override
    public List<CartItemVO> loadCartList() {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }

        List<Cart> carts = list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime));

        if (carts.isEmpty()) {
            return Collections.emptyList();
        }

        return buildCartVOs(carts, userId);
    }

    @Override
    public List<CartItemVO> addToCart(Long productId, Integer quantity) {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }

        // 1. 校验商品是否存在且上架
        Product product = productService.getById(productId);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            log.warn("加入购物车失败：商品不存在或已下架，productId={}", productId);
            return loadCartList();
        }

        // 2. 查询购物车是否已有该商品
        Cart existing = getOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId), false);

        if (existing != null) {
            // 已存在 -> 累加数量
            existing.setQuantity(existing.getQuantity() + quantity);
            existing.setUpdateTime(LocalDateTime.now());
            updateById(existing);
        } else {
            // 不存在 -> 新建购物车项，默认未选中
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            cart.setSelected(0);
            cart.setCreateTime(LocalDateTime.now());
            cart.setUpdateTime(LocalDateTime.now());
            save(cart);
        }

        return loadCartList();
    }

    @Override
    public List<CartItemVO> updateCart(Long id, Integer quantity) {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        if (quantity == null || quantity < 1) {
            log.warn("更新购物车数量无效：id={}, quantity={}", id, quantity);
            return loadCartList();
        }

        Cart cart = getById(id);
        if (cart == null || !userId.equals(cart.getUserId())) {
            log.warn("更新购物车失败：项不存在或不属于当前用户，id={}, userId={}", id, userId);
            return loadCartList();
        }

        cart.setQuantity(quantity);
        cart.setUpdateTime(LocalDateTime.now());
        updateById(cart);

        return loadCartList();
    }

    @Override
    public List<CartItemVO> removeFromCart(Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }

        Cart cart = getById(id);
        if (cart == null || !userId.equals(cart.getUserId())) {
            log.warn("移除购物车失败：项不存在或不属于当前用户，id={}, userId={}", id, userId);
            return loadCartList();
        }

        removeById(id);
        // 同步清理 Redis 选中集合
        getSelectedSet(userId).remove(id);

        return loadCartList();
    }

    @Override
    public List<CartItemVO> toggleSelect(Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }

        Cart cart = getById(id);
        if (cart == null || !userId.equals(cart.getUserId())) {
            log.warn("切换选中失败：项不存在或不属于当前用户，id={}, userId={}", id, userId);
            return loadCartList();
        }

        // 切换选中状态
        int newSelected = (cart.getSelected() != null && cart.getSelected() == 1) ? 0 : 1;
        cart.setSelected(newSelected);
        cart.setUpdateTime(LocalDateTime.now());
        updateById(cart);

        // 同步 Redis 选中集合
        RSet<Long> selectedSet = getSelectedSet(userId);
        if (newSelected == 1) {
            selectedSet.add(id);
        } else {
            selectedSet.remove(id);
        }

        return loadCartList();
    }

    @Override
    public List<CartItemVO> selectAll() {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }

        List<Cart> carts = list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId));
        if (carts.isEmpty()) {
            return Collections.emptyList();
        }

        // 判断当前是否已全部选中 -> 全选时切换为全不选，否则全选
        boolean allSelected = carts.stream()
                .allMatch(c -> c.getSelected() != null && c.getSelected() == 1);
        int target = allSelected ? 0 : 1;

        // 批量更新 DB
        RSet<Long> selectedSet = getSelectedSet(userId);
        List<Long> allIds = new ArrayList<>();
        for (Cart c : carts) {
            c.setSelected(target);
            c.setUpdateTime(LocalDateTime.now());
            allIds.add(c.getId());
        }
        updateBatchById(carts);

        // 同步 Redis 选中集合
        if (target == 1) {
            selectedSet.addAll(allIds);
        } else {
            selectedSet.clear();
        }

        return loadCartList();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 批量组装购物车 VO（含商品信息 + Redis 选中状态）.
     */
    private List<CartItemVO> buildCartVOs(List<Cart> carts, Long userId) {
        if (carts.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 批量查询商品信息
        Set<Long> productIds = carts.stream()
                .map(Cart::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<Product> products = productService.listByIds(productIds);
            for (Product p : products) {
                if (p.getId() != null) {
                    productMap.put(p.getId(), p);
                }
            }
        }

        // 2. 从 Redis 读取选中集合（O(1) 判断选中状态）
        Set<Long> selectedIds = getSelectedSet(userId).readAll();

        // 3. 组装 VO
        List<CartItemVO> voList = new ArrayList<>(carts.size());
        for (Cart c : carts) {
            Product p = productMap.get(c.getProductId());
            if (p == null) {
                // 商品已删除，跳过
                continue;
            }
            CartItemVO vo = new CartItemVO();
            vo.setId(c.getId());
            vo.setProductId(c.getProductId());
            vo.setName(p.getName());
            vo.setImage(p.getImage());
            vo.setPrice(p.getPrice());
            vo.setQuantity(c.getQuantity());
            vo.setStock(p.getStock());
            // 优先从 Redis 选中集合判断，兜底用 DB 字段
            vo.setSelected(selectedIds.contains(c.getId())
                    || (c.getSelected() != null && c.getSelected() == 1));
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 获取当前用户在 Redis 中的选中购物车项集合.
     */
    private RSet<Long> getSelectedSet(Long userId) {
        return redissonClient.getSet(REDIS_CART_SELECTED_PREFIX + userId);
    }

    /**
     * 获取当前登录用户 ID.
     */
    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            log.warn("当前用户未登录，无法操作购物车");
        }
        return userId;
    }
}
