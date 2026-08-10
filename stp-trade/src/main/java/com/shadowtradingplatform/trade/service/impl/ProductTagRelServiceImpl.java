package com.shadowtradingplatform.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shadowtradingplatform.trade.domain.po.ProductTagRel;
import com.shadowtradingplatform.trade.service.ProductTagRelService;
import com.shadowtradingplatform.trade.mapper.ProductTagRelMapper;
import org.springframework.stereotype.Service;

/**
* @author huanlingjiekou
* @description 针对表【product_tag_rel(商品-标签关系表)】的数据库操作Service实现
* @createDate 2026-08-09 16:08:29
*/
@Service
public class ProductTagRelServiceImpl extends ServiceImpl<ProductTagRelMapper, ProductTagRel>
    implements ProductTagRelService{

}




