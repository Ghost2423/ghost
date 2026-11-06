package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 沧元图 · 首页「元界动态」滚动条（表 cy_home_news）
 */
@TableName("cy_home_news")
public class HomeNews {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 标签：公告/预警/活动/历练 */
    private String tag;

    /** 动态文案 */
    private String content;

    /** 点击跳转的检索关键词 */
    private String keyword;

    /** 文字主色 */
    private String color;

    /** 排序（越小越前） */
    private Integer sortNo;

    /** 状态：1=展示 0=隐藏 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
