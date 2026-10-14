package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.CodexEntry;
import org.apache.ibatis.annotations.Mapper;

/**
 * 元界图鉴 Mapper（MyBatis-Plus BaseMapper，单表 CRUD 零 SQL）
 */
@Mapper
public interface CodexEntryMapper extends BaseMapper<CodexEntry> {
}
