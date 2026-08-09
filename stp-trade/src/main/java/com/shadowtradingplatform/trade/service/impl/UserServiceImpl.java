package com.shadowtradingplatform.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shadowtradingplatform.trade.domain.User;
import com.shadowtradingplatform.trade.service.UserService;
import com.shadowtradingplatform.trade.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author huanlingjiekou
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2026-08-09 16:08:29
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




