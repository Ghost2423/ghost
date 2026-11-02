package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.SparLog;
import org.apache.ibatis.annotations.Mapper;

/** 切磋记录 Mapper */
@Mapper
public interface SparLogMapper extends BaseMapper<SparLog> {
}
