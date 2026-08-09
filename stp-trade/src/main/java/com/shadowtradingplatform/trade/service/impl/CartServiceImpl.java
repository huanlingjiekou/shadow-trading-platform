package com.shadowtradingplatform.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shadowtradingplatform.trade.domain.Cart;
import com.shadowtradingplatform.trade.service.CartService;
import com.shadowtradingplatform.trade.mapper.CartMapper;
import org.springframework.stereotype.Service;

/**
* @author huanlingjiekou
* @description 针对表【cart(购物车表)】的数据库操作Service实现
* @createDate 2026-08-09 16:08:29
*/
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart>
    implements CartService{

}




