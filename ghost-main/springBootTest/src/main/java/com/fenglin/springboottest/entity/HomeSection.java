package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 沧元图 · 首页「核心界域」展示框（表 cy_home_section）
 * 对应首页四大展示框：神尊殿 / 修炼功法 / 妖魔图鉴 / 元界秘境。
 * section_type 与图鉴 cy_codex.category 一一对应，收录数由图鉴实时统计。
 */
@TableName("cy_home_section")
public class HomeSection {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 界域类型：神尊/功法/妖魔/秘境 */
    private String sectionType;

    /** 展示框标题 */
    private String title;

    /** 展示框图标 */
    private String icon;

    /** 展示框主色 */
    private String color;

    /** 展示框简介 */
    private String description;

    /** 排序（越小越前） */
    private Integer sortNo;

    /** 状态：1=展示 0=隐藏 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSectionType() { return sectionType; }
    public void setSectionType(String sectionType) { this.sectionType = sectionType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
