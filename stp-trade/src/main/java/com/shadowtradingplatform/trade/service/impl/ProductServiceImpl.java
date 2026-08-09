package com.shadowtradingplatform.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shadowtradingplatform.trade.domain.Product;
import com.shadowtradingplatform.trade.service.ProductService;
import com.shadowtradingplatform.trade.mapper.ProductMapper;
import org.springframework.stereotype.Service;

/**
* @author huanlingjiekou
* @description 针对表【product(商品表)】的数据库操作Service实现
* @createDate 2026-08-09 16:08:29
*/
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
    implements ProductService{

}




