package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 沧元图 · 元界图鉴（表 cy_codex）
 * 统管四大核心界域：神尊 / 功法 / 妖魔 / 秘境。
 * 单表 + category 字段，便于首页展示与跨类检索。
 */
@TableName("cy_codex")
public class CodexEntry {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 分类：神尊 / 功法 / 妖魔 / 秘境 */
    private String category;

    /** 名号 */
    private String name;

    /** 称号 / 标签（如「主角·刀道」「火妖」） */
    private String title;

    /** 简介 */
    private String description;

    /** 设定台词（flavor） */
    private String flavor;

    /** 功法售价（铜币），仅功法类有效 */
    private Integer price;

    /** 购得功法获得的神尊力，仅功法类有效 */
    private Integer powerGain;

    /** 卡片主色（首页卡片左侧色带/标签色） */
    private String color;

    /** 排序（越小越前） */
    private Integer sortNo;

    /** 状态：1=上架（首页展示） 0=下架 */
    private Integer status;

    /** 录入时间（数据库默认 CURRENT_TIMESTAMP） */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFlavor() { return flavor; }
    public void setFlavor(String flavor) { this.flavor = flavor; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public Integer getPowerGain() { return powerGain; }
    public void setPowerGain(Integer powerGain) { this.powerGain = powerGain; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
