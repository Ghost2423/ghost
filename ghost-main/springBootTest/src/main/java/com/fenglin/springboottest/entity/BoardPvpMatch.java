package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 太虚弈局 · PVP 对战记录（表 board_pvp_match） */
@TableName("board_pvp_match")
public class BoardPvpMatch {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer challengerId;
    private Integer defenderId;
    private Integer defenderAi;
    private String status;
    private Integer challengerPos;
    private Integer defenderPos;
    private Integer challengerRolls;
    private Integer defenderRolls;
    private String turn;
    private Integer pot;
    private String winner;
    private Integer challengerGainCopper;
    private Integer challengerGainPower;
    private Integer defenderGainCopper;
    private Integer defenderGainPower;
    private Integer challengerWard;
    private Integer defenderWard;
    private String diceLog;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getChallengerId() { return challengerId; }
    public void setChallengerId(Integer challengerId) { this.challengerId = challengerId; }
    public Integer getDefenderId() { return defenderId; }
    public void setDefenderId(Integer defenderId) { this.defenderId = defenderId; }
    public Integer getDefenderAi() { return defenderAi; }
    public void setDefenderAi(Integer defenderAi) { this.defenderAi = defenderAi; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getChallengerPos() { return challengerPos; }
    public void setChallengerPos(Integer challengerPos) { this.challengerPos = challengerPos; }
    public Integer getDefenderPos() { return defenderPos; }
    public void setDefenderPos(Integer defenderPos) { this.defenderPos = defenderPos; }
    public Integer getChallengerRolls() { return challengerRolls; }
    public void setChallengerRolls(Integer challengerRolls) { this.challengerRolls = challengerRolls; }
    public Integer getDefenderRolls() { return defenderRolls; }
    public void setDefenderRolls(Integer defenderRolls) { this.defenderRolls = defenderRolls; }
    public String getTurn() { return turn; }
    public void setTurn(String turn) { this.turn = turn; }
    public Integer getPot() { return pot; }
    public void setPot(Integer pot) { this.pot = pot; }
    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
    public Integer getChallengerGainCopper() { return challengerGainCopper; }
    public void setChallengerGainCopper(Integer challengerGainCopper) { this.challengerGainCopper = challengerGainCopper; }
    public Integer getChallengerGainPower() { return challengerGainPower; }
    public void setChallengerGainPower(Integer challengerGainPower) { this.challengerGainPower = challengerGainPower; }
    public Integer getDefenderGainCopper() { return defenderGainCopper; }
    public void setDefenderGainCopper(Integer defenderGainCopper) { this.defenderGainCopper = defenderGainCopper; }
    public Integer getDefenderGainPower() { return defenderGainPower; }
    public void setDefenderGainPower(Integer defenderGainPower) { this.defenderGainPower = defenderGainPower; }
    public Integer getChallengerWard() { return challengerWard; }
    public void setChallengerWard(Integer challengerWard) { this.challengerWard = challengerWard; }
    public Integer getDefenderWard() { return defenderWard; }
    public void setDefenderWard(Integer defenderWard) { this.defenderWard = defenderWard; }
    public String getDiceLog() { return diceLog; }
    public void setDiceLog(String diceLog) { this.diceLog = diceLog; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
