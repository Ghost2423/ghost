package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fenglin.springboottest.entity.AvatarFoundation;

/**
 * 神尊根基 Service
 */
public interface AvatarFoundationService extends IService<AvatarFoundation> {

    /** 按神尊相名号精确查询 */
    AvatarFoundation getByDeityName(String name);
}
