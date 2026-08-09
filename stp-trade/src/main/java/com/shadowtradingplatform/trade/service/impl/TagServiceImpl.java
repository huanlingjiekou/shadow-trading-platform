package com.shadowtradingplatform.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shadowtradingplatform.trade.domain.Tag;
import com.shadowtradingplatform.trade.service.TagService;
import com.shadowtradingplatform.trade.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author huanlingjiekou
* @description 针对表【tag(标签字典表)】的数据库操作Service实现
* @createDate 2026-08-09 16:08:29
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




