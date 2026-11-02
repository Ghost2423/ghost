package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.UserCultivation;
import org.apache.ibatis.annotations.Mapper;

/** 用户修炼状态 Mapper */
@Mapper
public interface UserCultivationMapper extends BaseMapper<UserCultivation> {
}
