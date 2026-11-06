package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.HomeSection;
import org.apache.ibatis.annotations.Mapper;

/** 首页核心界域展示框 Mapper */
@Mapper
public interface HomeSectionMapper extends BaseMapper<HomeSection> {
}
