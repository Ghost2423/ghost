package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fenglin.springboottest.entity.CodexEntry;
import com.fenglin.springboottest.mapper.CodexEntryMapper;
import com.fenglin.springboottest.service.CodexService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CodexServiceImpl
        extends ServiceImpl<CodexEntryMapper, CodexEntry>
        implements CodexService {

    @Override
    public List<CodexEntry> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ArrayList<>();
        }
        String kw = "%" + keyword.trim() + "%";
        QueryWrapper<CodexEntry> qw = new QueryWrapper<>();
        qw.like("name", kw)
          .or().like("title", kw)
          .or().like("description", kw)
          .or().like("flavor", kw);
        qw.orderByDesc("id");
        return list(qw);
    }
}
