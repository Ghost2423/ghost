package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 沧元图 · 论道论坛帖子（表 cy_forum_post）
 */
@TableName("cy_forum_post")
public class ForumPost {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 板块：论道 / 切磋 / 妖情 / 闲谈 */
    private String category;

    private String title;

    private String content;

    /** 发帖账号（avatar_foundation.deity_name） */
    private String authorName;

    /** 展示名号（神尊封号） */
    private String authorAlias;

    private Integer viewCount;

    private Integer likeCount;

    private Integer replyCount;

    /** 1=置顶 0=普通 */
    private Integer isPinned;

    /** 1=正常 0=已隐藏 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastReplyAt;

    /** 非数据库字段：当前浏览者是否已共鸣（点赞） */
    @TableField(exist = false)
    private Boolean liked;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorAlias() { return authorAlias; }
    public void setAuthorAlias(String authorAlias) { this.authorAlias = authorAlias; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public Integer getReplyCount() { return replyCount; }
    public void setReplyCount(Integer replyCount) { this.replyCount = replyCount; }

    public Integer getIsPinned() { return isPinned; }
    public void setIsPinned(Integer isPinned) { this.isPinned = isPinned; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastReplyAt() { return lastReplyAt; }
    public void setLastReplyAt(LocalDateTime lastReplyAt) { this.lastReplyAt = lastReplyAt; }

    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }
}
