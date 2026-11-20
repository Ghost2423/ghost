package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 沧元图 · 首页卡片首阅记录（表 user_card_view）
 * 每个账号每张卡片首次点开时插入一行，并奖励铜币；重复点开不再奖励。
 */
@TableName("user_card_view")
public class UserCardView {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** avatar_foundation.id */
    private Long userId;

    /** 卡片唯一标识：codex_{cy_codex.id} */
    private String cardKey;

    /** 卡片名号（冗余便于查看） */
    private String cardName;

    /** 首次浏览时间 */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCardKey() { return cardKey; }
    public void setCardKey(String cardKey) { this.cardKey = cardKey; }

    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
