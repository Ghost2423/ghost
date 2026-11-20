package com.fenglin.springboottest.dto;

import java.util.Map;

/**
 * 后台「数据编辑」通用请求体（用户资料 / 注册记录 / 论道帖 / 回帖 共用）
 *
 * name + spell 为操作者管理员凭证；item 为要写入的字段（键采用 snake_case，
 * 与全局 Jackson SNAKE_CASE 及数据库列名保持一致）。只传需要修改的字段，
 * 未出现的字段保持数据库原值。
 */
public class AdminDataRequest {

    /** 操作者账号 */
    private String name;
    /** 操作者密码 */
    private String spell;
    /** 目标用户 id（user/update） */
    private Long targetId;
    /** 目标记录 id（register/update、forum/edit） */
    private Long id;
    /** 目标回帖 id（forum/reply/edit） */
    private Long replyId;
    /** 是否作用于全部账号（修炼数据批量编辑：target_all=true 时忽略 target_id） */
    private Boolean targetAll;
    /** 要写入的字段 */
    private Map<String, Object> item;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpell() { return spell; }
    public void setSpell(String spell) { this.spell = spell; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReplyId() { return replyId; }
    public void setReplyId(Long replyId) { this.replyId = replyId; }

    public Boolean getTargetAll() { return targetAll; }
    public void setTargetAll(Boolean targetAll) { this.targetAll = targetAll; }

    public Map<String, Object> getItem() { return item; }
    public void setItem(Map<String, Object> item) { this.item = item; }
}
