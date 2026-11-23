package com.fenglin.springboottest.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 太虚弈局飞行棋字段自举：启动时为旧库的对局表补齐新列，避免漏执行 SQL 脚本导致 500。
 * 通过 information_schema 判断列是否存在，可重复执行；失败只记日志。
 */
@Component
public class BoardGameSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(BoardGameSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public BoardGameSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureColumns() {
        try {
            addColumn("board_game_log", "plane_states",
                    "VARCHAR(64) NOT NULL DEFAULT '-1,-1,-1,-1' COMMENT '飞行棋4架飞机位置'");
            addColumn("board_game_log", "guards",
                    "VARCHAR(32) NOT NULL DEFAULT '1,1' COMMENT '劫妖守军存活状态'");
            addColumn("board_game_log", "active_plane",
                    "INT NOT NULL DEFAULT 0 COMMENT '当前选中飞机编号'");
            addColumn("board_game_log", "extra_roll",
                    "TINYINT(1) NOT NULL DEFAULT 0 COMMENT '掷6再掷标记'");

            addColumn("board_pvp_match", "challenger_planes",
                    "VARCHAR(64) NOT NULL DEFAULT '-1,-1,-1,-1' COMMENT '挑战者4架飞机位置'");
            addColumn("board_pvp_match", "defender_planes",
                    "VARCHAR(64) NOT NULL DEFAULT '-1,-1,-1,-1' COMMENT '应战者4架飞机位置'");
            addColumn("board_pvp_match", "challenger_active",
                    "INT NOT NULL DEFAULT 0 COMMENT '挑战者选中飞机'");
            addColumn("board_pvp_match", "defender_active",
                    "INT NOT NULL DEFAULT 0 COMMENT '应战者选中飞机'");
            addColumn("board_pvp_match", "extra_roll",
                    "TINYINT(1) NOT NULL DEFAULT 0 COMMENT '掷6再掷标记'");
        } catch (Exception e) {
            log.warn("飞行棋字段自举失败（可手动执行 sql/boardgame-flightchess.sql）：{}", e.getMessage());
        }
    }

    private void addColumn(String table, String column, String definition) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (cnt != null && cnt == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("飞行棋字段自举：{} 新增列 {}", table, column);
        }
    }
}
