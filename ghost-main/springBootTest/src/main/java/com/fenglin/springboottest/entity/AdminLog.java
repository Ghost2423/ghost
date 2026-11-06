package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 沧元图 · 后台操作日志（表 cy_admin_log）
 */
@TableName("cy_admin_log")
public class AdminLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 管理员用户 id */
    private Long adminId;

    /** 管理员账号 */
    private String adminName;

    /** 动作：新增/修改/删除/上下架/登录 */
    private String action;

    /** 目标类型：图鉴/展示框/动态/文案/用户 */
    private String targetType;

    /** 目标 id */
    private String targetId;

    /** 操作摘要 */
    private String detail;

    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
