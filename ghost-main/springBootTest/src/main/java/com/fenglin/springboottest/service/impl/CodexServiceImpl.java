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
        // 仅检索上架（status=1）条目，与首页展示保持一致
        qw.eq("status", 1);
        qw.and(w -> w.like("name", kw)
                .or().like("title", kw)
                .or().like("description", kw)
                .or().like("flavor", kw));
        // 检索结果按分类排序（神尊/功法/妖魔/秘境）再按人工排序
        qw.last("ORDER BY FIELD(category,'神尊','功法','妖魔','秘境'), sort_no ASC, id ASC");
        return list(qw);
    }
}
