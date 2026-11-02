package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 沧元图 · 用户修炼状态（表 user_cultivation）
 * 每注册用户一行（登录后首次访问修炼殿时惰性初始化）。
 */
@TableName("user_cultivation")
public class UserCultivation {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** avatar_foundation.id */
    private Long userId;

    /** 当前境界（筑基起） */
    private String realm;

    /** 神尊力（修炼经验） */
    private Integer power;

    /** 铜币 */
    private Integer copper;

    /** 称号：封侯神尊 / 封王神尊 */
    private String title;

    /** 秘境修炼计数日期（每日重置） */
    private LocalDate secretDate;

    /** 当日已入秘境次数 */
    private Integer secretCount;

    /** 开辟时间 */
    private LocalDateTime createdAt;

    /** 最近修炼时间 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRealm() { return realm; }
    public void setRealm(String realm) { this.realm = realm; }

    public Integer getPower() { return power; }
    public void setPower(Integer power) { this.power = power; }

    public Integer getCopper() { return copper; }
    public void setCopper(Integer copper) { this.copper = copper; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getSecretDate() { return secretDate; }
    public void setSecretDate(LocalDate secretDate) { this.secretDate = secretDate; }

    public Integer getSecretCount() { return secretCount; }
    public void setSecretCount(Integer secretCount) { this.secretCount = secretCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
