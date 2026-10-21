package com.fenglin.springboottest.dto;

/**
 * 觉醒（登录）请求体
 */
public class LoginRequest {

    private String name;
    private String spell;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpell() { return spell; }
    public void setSpell(String spell) { this.spell = spell; }
}
