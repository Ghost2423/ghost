package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.ForumReply;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ForumReplyMapper extends BaseMapper<ForumReply> {
}
