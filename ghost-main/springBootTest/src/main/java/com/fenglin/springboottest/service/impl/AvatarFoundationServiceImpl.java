package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.mapper.AvatarFoundationMapper;
import com.fenglin.springboottest.service.AvatarFoundationService;
import org.springframework.stereotype.Service;

@Service
public class AvatarFoundationServiceImpl
        extends ServiceImpl<AvatarFoundationMapper, AvatarFoundation>
        implements AvatarFoundationService {

    @Override
    public AvatarFoundation getByDeityName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return getOne(new QueryWrapper<AvatarFoundation>().eq("deity_name", name.trim()));
    }
}
