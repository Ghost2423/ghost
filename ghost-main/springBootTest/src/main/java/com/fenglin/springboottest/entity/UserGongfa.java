package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 沧元图 · 用户已购功法（表 user_gongfa）
 * 每功法限购一次，永久持有。
 */
@TableName("user_gongfa")
public class UserGongfa {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** avatar_foundation.id */
    private Long userId;

    /** 功法名（cy_codex.name） */
    private String gongfaName;

    /** 购得时间 */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getGongfaName() { return gongfaName; }
    public void setGongfaName(String gongfaName) { this.gongfaName = gongfaName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
