package com.fenglin.springboottest.dto;

/**
 * 锻造神尊根基（注册）请求体
 * 全局 Jackson 为 SNAKE_CASE 策略，字段 deityName 对应 JSON 的 deity_name。
 */
public class ForgeRequest {

    private String region;
    private String deityName;
    private String mythName;
    private String spell;
    private String mythStory;
    private Integer agreed;

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getDeityName() { return deityName; }
    public void setDeityName(String deityName) { this.deityName = deityName; }

    public String getMythName() { return mythName; }
    public void setMythName(String mythName) { this.mythName = mythName; }

    public String getSpell() { return spell; }
    public void setSpell(String spell) { this.spell = spell; }

    public String getMythStory() { return mythStory; }
    public void setMythStory(String mythStory) { this.mythStory = mythStory; }

    public Integer getAgreed() { return agreed; }
    public void setAgreed(Integer agreed) { this.agreed = agreed; }
}
