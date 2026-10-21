package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fenglin.springboottest.entity.CodexEntry;

import java.util.List;

/**
 * 元界图鉴 Service
 */
public interface CodexService extends IService<CodexEntry> {

    /** 跨四类（神尊/功法/妖魔/秘境）模糊检索 */
    List<CodexEntry> search(String keyword);
}
