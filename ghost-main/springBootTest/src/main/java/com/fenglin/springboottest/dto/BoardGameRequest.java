package com.fenglin.springboottest.dto;

/**
 * 太虚弈局（桌游）请求体。
 * name + spell 为当前操作者凭证；action 预留（start/roll/history，实际由路径区分）。
 * 全局 Jackson 为 SNAKE_CASE 策略。
 */
public class BoardGameRequest {

    /** 操作者账号（即 deity_name） */
    private String name;
    /** 操作者密码（即 spell，明文比对） */
    private String spell;
    /** 预留动作 */
    private String action;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpell() { return spell; }
    public void setSpell(String spell) { this.spell = spell; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
