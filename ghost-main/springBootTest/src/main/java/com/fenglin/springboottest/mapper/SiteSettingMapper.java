package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.SiteSetting;
import org.apache.ibatis.annotations.Mapper;

/** 站点文案配置 Mapper */
@Mapper
public interface SiteSettingMapper extends BaseMapper<SiteSetting> {
}
