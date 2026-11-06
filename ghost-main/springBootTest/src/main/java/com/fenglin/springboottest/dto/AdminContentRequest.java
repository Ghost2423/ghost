package com.fenglin.springboottest.dto;

import java.util.Map;

/**
 * 后台内容管理请求体（图鉴 / 展示框 / 动态 / 文案 共用）
 *
 * name + spell 为管理员凭证；item 为新增/编辑主体（键采用 snake_case，如 sort_no）；
 * settings 为站点文案批量保存的 key-value。
 * 说明：主体放在 item 内，避免与其自身的 name（名号）字段冲突。
 */
public class AdminContentRequest {

    /** 管理员账号 */
    private String name;
    /** 管理员密码 */
    private String spell;
    /** 目标 id（删除 / 上下架） */
    private Long id;
    /** 分类筛选（图鉴） */
    private String category;
    /** 关键词筛选 */
    private String keyword;
    /** 状态：1=上架/展示，0=下架/隐藏 */
    private Integer status;
    /** 新增/编辑主体 */
    private Map<String, Object> item;
    /** 文案批量保存 */
    private Map<String, String> settings;
    /** 分页：页码（从 1 开始，缺省 1） */
    private Integer page;
    /** 分页：每页条数（缺省 20，上限 100） */
    private Integer pageSize;
    /** 批量操作的目标 id 列表（批量上下架） */
    private java.util.List<Long> ids;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpell() { return spell; }
    public void setSpell(String spell) { this.spell = spell; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Map<String, Object> getItem() { return item; }
    public void setItem(Map<String, Object> item) { this.item = item; }

    public Map<String, String> getSettings() { return settings; }
    public void setSettings(Map<String, String> settings) { this.settings = settings; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public java.util.List<Long> getIds() { return ids; }
    public void setIds(java.util.List<Long> ids) { this.ids = ids; }
}
