package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.UserRegisterRecord;
import org.apache.ibatis.annotations.Mapper;

/** 注册账号记录 Mapper（MyBatis-Plus BaseMapper） */
@Mapper
public interface UserRegisterRecordMapper extends BaseMapper<UserRegisterRecord> {
}
