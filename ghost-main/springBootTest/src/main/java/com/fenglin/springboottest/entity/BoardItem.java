package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 太虚弈局 · 道具背包（表 board_item） */
@TableName("board_item")
public class BoardItem {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** avatar_foundation.id */
    private Integer userId;

    /** 道具标识：leap/coin/rain/ward */
    private String itemKey;

    /** 持有数量 */
    private Integer cnt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }
    public Integer getCnt() { return cnt; }
    public void setCnt(Integer cnt) { this.cnt = cnt; }
}
