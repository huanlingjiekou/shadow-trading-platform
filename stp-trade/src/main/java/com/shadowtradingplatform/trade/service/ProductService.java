package com.shadowtradingplatform.trade.service;

import com.shadowtradingplatform.trade.common.PageResult;
import com.shadowtradingplatform.trade.domain.dto.ProductQueryDTO;
import com.shadowtradingplatform.trade.domain.po.Product;
import com.shadowtradingplatform.trade.domain.vo.ProductDetailVO;
import com.shadowtradingplatform.trade.domain.vo.ProductItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author huanlingjiekou
* @description 针对表【product(商品表)】的数据库操作Service
* @createDate 2026-08-09 16:08:29
*/
public interface ProductService extends IService<Product> {

    /**
     * 分页查询上架商品列表（含标签）.
     *
     * @param query 分页 + 分类过滤参数
     * @return 商品列表分页结果
     */
    PageResult<ProductItemVO> loadProductPage(ProductQueryDTO query);

    /**
     * 查询商品详情（含图片列表、标签、富文本详情）.
     *
     * @param id 商品ID
     * @return 商品详情，不存在返回 null
     */
    ProductDetailVO loadProductDetail(Long id);
}
