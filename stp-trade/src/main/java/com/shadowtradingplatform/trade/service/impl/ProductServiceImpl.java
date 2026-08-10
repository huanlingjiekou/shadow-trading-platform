package com.shadowtradingplatform.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shadowtradingplatform.trade.common.PageResult;
import com.shadowtradingplatform.trade.domain.dto.ProductQueryDTO;
import com.shadowtradingplatform.trade.domain.po.Product;
import com.shadowtradingplatform.trade.domain.po.ProductImage;
import com.shadowtradingplatform.trade.domain.po.ProductTagRel;
import com.shadowtradingplatform.trade.domain.po.Tag;
import com.shadowtradingplatform.trade.domain.vo.ProductDetailVO;
import com.shadowtradingplatform.trade.domain.vo.ProductItemVO;
import com.shadowtradingplatform.trade.mapper.ProductMapper;
import com.shadowtradingplatform.trade.mapper.TagMapper;
import com.shadowtradingplatform.trade.service.ProductImageService;
import com.shadowtradingplatform.trade.service.ProductService;
import com.shadowtradingplatform.trade.service.ProductTagRelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
* @author huanlingjiekou
* @description 针对表【product(商品表)】的数据库操作Service实现
* @createDate 2026-08-09 16:08:29
*/
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
    implements ProductService {

    private final ProductTagRelService productTagRelService;
    private final ProductImageService productImageService;
    private final TagMapper tagMapper;

    @Override
    public PageResult<ProductItemVO> loadProductPage(ProductQueryDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? 1 : query.getPage();
        int pageSize = (query.getPageSize() == null || query.getPageSize() < 1) ? 20 : query.getPageSize();

        // 1. 分页查询上架商品
        Page<Product> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(Product::getCategory, query.getCategory());
        }
        wrapper.orderByDesc(Product::getSales);

        Page<Product> result = this.page(pageParam, wrapper);

        // 2. 批量查询当前页商品的标签
        List<Long> productIds = result.getRecords().stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        Map<Long, List<String>> tagMap = loadTagsByProductIds(productIds);

        // 3. 组装 VO
        List<ProductItemVO> list = result.getRecords().stream()
                .map(p -> toProductItemVO(p, tagMap.getOrDefault(p.getId(), Collections.emptyList())))
                .collect(Collectors.toList());

        return new PageResult<>(list, result.getTotal(), page, pageSize);
    }

    /**
     * 批量查询多个商品的标签名列表.
     *
     * @param productIds 商品 ID 集合
     * @return productId -> 标签名列表
     */
    private Map<Long, List<String>> loadTagsByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 查询商品-标签关联
        List<ProductTagRel> rels = productTagRelService.list(
                new LambdaQueryWrapper<ProductTagRel>()
                        .in(ProductTagRel::getProduct_id, productIds));
        if (rels == null || rels.isEmpty()) {
            return Collections.emptyMap();
        }

        // 查询标签名称：过滤掉 rel 为 null 或 tag_id 为 null 的记录
        Set<Long> tagIds = rels.stream()
                .filter(Objects::nonNull)
                .map(ProductTagRel::getTag_id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Tag> tags=new ArrayList<>();
        if(!tagIds.isEmpty()){
            tags = tagMapper.selectBatchIds(tagIds);
        }
        Map<Long, String> tagNameMap = new HashMap<>();
        for (Tag t : tags) {
            if (t.getId() != null && t.getName() != null) {
                tagNameMap.put(t.getId(), t.getName());
            }
        }

        // 按 productId 分组（跳过 rel/tag_id/product_id 为 null 的记录）
        Map<Long, List<String>> result = new HashMap<>();
        for (ProductTagRel rel : rels) {
            if (rel == null || rel.getProduct_id() == null || rel.getTag_id() == null) {
                continue;
            }
            String tagName = tagNameMap.get(rel.getTag_id());
            if (tagName != null) {
                result.computeIfAbsent(rel.getProduct_id(), k -> new ArrayList<>()).add(tagName);
            }
        }
        return result;
    }

    @Override
    public ProductDetailVO loadProductDetail(Long id) {
        if (id == null) {
            return null;
        }

        // 1. 查询商品
        Product product = this.getById(id);
        if (product == null) {
            return null;
        }

        // 2. 查询标签
        Map<Long, List<String>> tagMap = loadTagsByProductIds(Collections.singletonList(id));

        // 3. 查询图片列表（按 sort_order 升序）
        List<String> images = loadProductImages(id);

        // 4. 组装详情 VO
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setSubtitle(product.getSubtitle());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getOriginal_price());
        vo.setImage(product.getImage());
        vo.setCategory(product.getCategory());
        vo.setSales(product.getSales());
        vo.setStock(product.getStock());
        vo.setDescription(product.getDescription());
        vo.setTags(tagMap.getOrDefault(id, Collections.emptyList()));
        vo.setImages(images);
        vo.setDetail(product.getDetail());
        return vo;
    }

    /**
     * 查询商品图片列表（按 sort_order 升序）.
     */
    private List<String> loadProductImages(Long productId) {
        List<ProductImage> images = productImageService.list(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProduct_id, productId)
                        .orderByAsc(ProductImage::getSort_order));
        return images.stream()
                .map(ProductImage::getImage_url)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    /**
     * Product PO -> ProductItemVO 转换.
     */
    private ProductItemVO toProductItemVO(Product p, List<String> tags) {
        ProductItemVO vo = new ProductItemVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setSubtitle(p.getSubtitle());
        vo.setPrice(p.getPrice());
        vo.setOriginalPrice(p.getOriginal_price());
        vo.setImage(p.getImage());
        vo.setCategory(p.getCategory());
        vo.setSales(p.getSales());
        vo.setStock(p.getStock());
        vo.setDescription(p.getDescription());
        vo.setTags(tags);
        return vo;
    }
}
