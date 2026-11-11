package com.fenglin.springboottest.dto;

/**
 * 论道论坛通用请求体（Jackson 全局 SNAKE_CASE：page_size / post_id / reply_id 等）。
 * 读接口无需凭证；写接口以 name + spell 校验身份（与登录、修炼系统一致）。
 */
public class ForumRequest {

    private String name;
    private String spell;
    private String category;
    private String keyword;
    private String sort;
    private String title;
    private String content;
    private Long id;
    private Long postId;
    private Long replyId;
    private Integer pinned;
    private Integer status;
    private Integer page;
    private Integer pageSize;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpell() { return spell; }
    public void setSpell(String spell) { this.spell = spell; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public Long getReplyId() { return replyId; }
    public void setReplyId(Long replyId) { this.replyId = replyId; }

    public Integer getPinned() { return pinned; }
    public void setPinned(Integer pinned) { this.pinned = pinned; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
