package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 沧元界 · 注册账号记录（表 user_register_record）
 * 每次注册成功后独立落一条，用户被后台删除 / 改权限也不影响此历史记录。
 */
@TableName("user_register_record")
public class UserRegisterRecord {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联的 avatar_foundation.id（历史留痕，用户删除后仍保留） */
    private Long avatarId;

    /** 注册账号 */
    private String account;

    /** 注册密码（明文，与 avatar_foundation.spell 约定一致） */
    private String password;

    /** 地域 */
    private String region;

    /** 觉醒神尊名号 */
    private String mythName;

    /** 神话原型简介 */
    private String mythStory;

    /** 是否立下心魔誓约 */
    private Integer agreed;

    /** 注册时权限快照 */
    private Integer permission;

    /** 注册来源 IP */
    private String registerIp;

    /** 来源：forge / migrate */
    private String source;

    /** 注册时间 */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAvatarId() { return avatarId; }
    public void setAvatarId(Long avatarId) { this.avatarId = avatarId; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getMythName() { return mythName; }
    public void setMythName(String mythName) { this.mythName = mythName; }

    public String getMythStory() { return mythStory; }
    public void setMythStory(String mythStory) { this.mythStory = mythStory; }

    public Integer getAgreed() { return agreed; }
    public void setAgreed(Integer agreed) { this.agreed = agreed; }

    public Integer getPermission() { return permission; }
    public void setPermission(Integer permission) { this.permission = permission; }

    public String getRegisterIp() { return registerIp; }
    public void setRegisterIp(String registerIp) { this.registerIp = registerIp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
