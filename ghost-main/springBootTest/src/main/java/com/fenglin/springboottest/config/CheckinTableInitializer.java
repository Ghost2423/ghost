package com.fenglin.springboottest.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 签到表自举：服务启动时确保 user_checkin 存在，避免旧库忘记执行 SQL 脚本。
 * 使用 CREATE TABLE IF NOT EXISTS，重复启动无副作用；失败只记日志，不影响其它接口。
 */
@Component
public class CheckinTableInitializer {

    private static final Logger log = LoggerFactory.getLogger(CheckinTableInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public CheckinTableInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS user_checkin (" +
                    " id INT UNSIGNED NOT NULL AUTO_INCREMENT," +
                    " user_id INT UNSIGNED NOT NULL COMMENT 'avatar_foundation.id'," +
                    " checkin_date DATE DEFAULT NULL COMMENT '最近签到日期'," +
                    " streak INT NOT NULL DEFAULT 0 COMMENT '当前连续签到天数'," +
                    " total INT NOT NULL DEFAULT 0 COMMENT '累计签到天数'," +
                    " PRIMARY KEY (id)," +
                    " UNIQUE KEY uk_user (user_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci " +
                    "COMMENT='沧元图·每日签到'");
        } catch (Exception e) {
            log.warn("签到表初始化失败（可手动执行 sql/add_daily_checkin.sql）：{}", e.getMessage());
        }
    }
}
