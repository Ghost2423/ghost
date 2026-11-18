package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 沧元图 · 太虚弈局对局记录（表 board_game_log）
 * 一局一行：start 建行 -> roll 逐步推进 -> 通关/次数用尽结算。
 */
@TableName("board_game_log")
public class BoardGameLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** avatar_foundation.id */
    private Long userId;

    /** playing=进行中 / finished=已结束 */
    private String status;

    /** 当前所在格（0 起） */
    private Integer position;

    /** 已掷骰次数 */
    private Integer rolls;

    /** 掷骰轨迹，如 "3,6,2,5" */
    private String diceLog;

    /** 本局累计获得铜币 */
    private Integer gainCopper;

    /** 本局累计获得神尊力 */
    private Integer gainPower;

    /** 是否通关：1=通关 0=未通关 */
    private Integer pass;

    /** 镇厄符护盾：1=本局免疫下一次劫难 */
    private Integer ward;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public Integer getRolls() { return rolls; }
    public void setRolls(Integer rolls) { this.rolls = rolls; }
    public String getDiceLog() { return diceLog; }
    public void setDiceLog(String diceLog) { this.diceLog = diceLog; }
    public Integer getGainCopper() { return gainCopper; }
    public void setGainCopper(Integer gainCopper) { this.gainCopper = gainCopper; }
    public Integer getGainPower() { return gainPower; }
    public void setGainPower(Integer gainPower) { this.gainPower = gainPower; }
    public Integer getPass() { return pass; }
    public void setPass(Integer pass) { this.pass = pass; }
    public Integer getWard() { return ward; }
    public void setWard(Integer ward) { this.ward = ward; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
