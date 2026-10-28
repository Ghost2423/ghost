package com.fenglin.springboottest.dto;

/**
 * 后台管理系统请求体（登录 / 用户列表 / 权限变更 / 删除用户 共用）
 * name + spell 为当前操作者的管理员凭证；targetId / permission 为管理操作目标。
 * 全局 Jackson 为 SNAKE_CASE 策略。
 */
public class AdminRequest {

    /** 操作者账号（即 deity_name） */
    private String name;
    /** 操作者密码（即 spell，明文比对） */
    private String spell;
    /** 目标用户 id（列表登录接口可不传） */
    private Long targetId;
    /** 目标权限：1=默认用户，0=管理员 */
    private Integer permission;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpell() { return spell; }
    public void setSpell(String spell) { this.spell = spell; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public Integer getPermission() { return permission; }
    public void setPermission(Integer permission) { this.permission = permission; }
}
