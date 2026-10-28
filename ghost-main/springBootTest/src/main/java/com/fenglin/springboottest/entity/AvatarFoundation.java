package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 沧元界 · 神尊根基（表 avatar_foundation）
 * 对应一次「锻造神尊根基」提交，登录按神尊相(deity_name)精确匹配。
 */
@TableName("avatar_foundation")
public class AvatarFoundation {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属地域 */
    private String region;

    /** 觉醒神尊相（名号+封号，如「烛龙真君」） */
    private String deityName;

    /** 上古神尊原型（如「烛龙」「女娲」） */
    private String mythName;

    /** 召唤语（8–16 字，须含神尊名） */
    private String spell;

    /** 神话原型简介 */
    private String mythStory;

    /** 是否立下心魔誓约（用户协议） */
    private Integer agreed;

    /** 权限：1=默认用户（注册默认），0=管理员（可登录后台管理系统） */
    private Integer permission;

    /** 锻造时间（数据库默认 CURRENT_TIMESTAMP） */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getDeityName() { return deityName; }
    public void setDeityName(String deityName) { this.deityName = deityName; }

    public String getMythName() { return mythName; }
    public void setMythName(String mythName) { this.mythName = mythName; }

    public String getSpell() { return spell; }
    public void setSpell(String spell) { this.spell = spell; }

    public String getMythStory() { return mythStory; }
    public void setMythStory(String mythStory) { this.mythStory = mythStory; }

    public Integer getAgreed() { return agreed; }
    public void setAgreed(Integer agreed) { this.agreed = agreed; }

    public Integer getPermission() { return permission; }
    public void setPermission(Integer permission) { this.permission = permission; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
