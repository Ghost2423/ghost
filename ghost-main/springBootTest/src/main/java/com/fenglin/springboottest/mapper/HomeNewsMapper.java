package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.HomeNews;
import org.apache.ibatis.annotations.Mapper;

/** 首页元界动态 Mapper */
@Mapper
public interface HomeNewsMapper extends BaseMapper<HomeNews> {
}
