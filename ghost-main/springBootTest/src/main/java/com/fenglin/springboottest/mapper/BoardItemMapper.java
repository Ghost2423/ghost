package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.BoardItem;
import org.apache.ibatis.annotations.Mapper;

/** 太虚弈局道具背包 Mapper */
@Mapper
public interface BoardItemMapper extends BaseMapper<BoardItem> {
}
