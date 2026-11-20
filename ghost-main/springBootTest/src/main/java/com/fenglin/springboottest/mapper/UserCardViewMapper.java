package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.UserCardView;
import org.apache.ibatis.annotations.Mapper;

/** 首页卡片首阅记录 Mapper */
@Mapper
public interface UserCardViewMapper extends BaseMapper<UserCardView> {
}
