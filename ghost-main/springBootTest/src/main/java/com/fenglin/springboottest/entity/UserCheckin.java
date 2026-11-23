package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

/**
 * 沧元图 · 用户每日签到记录（表 user_checkin）
 * 每个 avatar_foundation.id 一行，记录最近签到日期、连续天数与累计天数。
 */
@TableName("user_checkin")
public class UserCheckin {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** avatar_foundation.id */
    private Long userId;

    /** 最近一次签到日期（每日一次） */
    private LocalDate checkinDate;

    /** 当前连续签到天数（断签重置为 1） */
    private Integer streak;

    /** 累计签到天数 */
    private Integer total;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getCheckinDate() { return checkinDate; }
    public void setCheckinDate(LocalDate checkinDate) { this.checkinDate = checkinDate; }

    public Integer getStreak() { return streak; }
    public void setStreak(Integer streak) { this.streak = streak; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
}
