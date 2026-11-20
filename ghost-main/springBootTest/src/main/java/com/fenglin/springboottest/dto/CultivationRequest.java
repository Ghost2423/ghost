package com.fenglin.springboottest.dto;

/**
 * 修炼系统请求体（秘境 / 切磋 / 购功法 / 突破 / 卡片首阅 共用）
 * name + spell 为当前操作者凭证；target 为切磋对象账号；gongfa 为欲购功法名；
 * card_key / card_name 为首页卡片首阅的标识与名号。
 * 全局 Jackson 为 SNAKE_CASE 策略。
 */
public class CultivationRequest {

    /** 操作者账号（即 deity_name） */
    private String name;
    /** 操作者密码（即 spell，明文比对） */
    private String spell;
    /** 切磋对象账号（deity_name） */
    private String target;
    /** 欲购功法名 */
    private String gongfa;
    /** 首页卡片唯一标识（codex_{id}） */
    private String cardKey;
    /** 首页卡片名号 */
    private String cardName;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpell() { return spell; }
    public void setSpell(String spell) { this.spell = spell; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getGongfa() { return gongfa; }
    public void setGongfa(String gongfa) { this.gongfa = gongfa; }

    public String getCardKey() { return cardKey; }
    public void setCardKey(String cardKey) { this.cardKey = cardKey; }

    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }
}
