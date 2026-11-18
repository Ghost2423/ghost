package com.fenglin.springboottest.dto;

/**
 * 太虚弈局（桌游）请求体。
 * name + spell 为当前操作者凭证；全局 Jackson 为 SNAKE_CASE 策略。
 *
 * 单人：action 由路径区分（start/roll）
 * PVP： match_id 指定对局；create 时 opponent_name 为空表示挑战天道AI
 * 道具：item_key 指定要使用的道具
 */
public class BoardGameRequest {

    /** 操作者账号（即 deity_name） */
    private String name;
    /** 操作者密码（即 spell，明文比对） */
    private String spell;
    /** 预留动作 */
    private String action;
    /** PVP 对局 ID */
    private Long matchId;
    /** 应战对手名号（为空则由天道AI接战） */
    private String opponentName;
    /** 是否挑战天道AI */
    private Boolean versusAi;
    /** 要使用的道具标识 */
    private String itemKey;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpell() { return spell; }
    public void setSpell(String spell) { this.spell = spell; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }
    public String getOpponentName() { return opponentName; }
    public void setOpponentName(String opponentName) { this.opponentName = opponentName; }
    public Boolean getVersusAi() { return versusAi; }
    public void setVersusAi(Boolean versusAi) { this.versusAi = versusAi; }
    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }
}
