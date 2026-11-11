package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 沧元图 · 论道论坛回帖（表 cy_forum_reply）
 */
@TableName("cy_forum_reply")
public class ForumReply {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long postId;

    private String authorName;

    private String authorAlias;

    private String content;

    /** 楼层：1 楼为主帖，回帖从 2 楼起 */
    private Integer floorNo;

    /** 1=正常 0=已隐藏 */
    private Integer status;

    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorAlias() { return authorAlias; }
    public void setAuthorAlias(String authorAlias) { this.authorAlias = authorAlias; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getFloorNo() { return floorNo; }
    public void setFloorNo(Integer floorNo) { this.floorNo = floorNo; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
