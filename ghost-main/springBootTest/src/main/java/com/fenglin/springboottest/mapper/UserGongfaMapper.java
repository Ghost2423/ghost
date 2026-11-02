package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.UserGongfa;
import org.apache.ibatis.annotations.Mapper;

/** 用户已购功法 Mapper */
@Mapper
public interface UserGongfaMapper extends BaseMapper<UserGongfa> {
}
