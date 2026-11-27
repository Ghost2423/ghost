package com.fenglin.springboottest.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 图鉴字段自举：启动时为旧库的 cy_codex 补齐实体类所需的列。
 *
 * 背景：CodexEntry 实体包含 image 等字段，MyBatis-Plus 的 SELECT 会把所有字段列进字段清单。
 * 旧库若漏执行 sql/upgrade_codex_image.sql（没有 image 列），会导致所有图鉴相关查询报
 * "Unknown column 'image' in 'field list'"，表现为：后台图鉴管理页空白、首页图鉴/检索失效、
 * 修炼殿功法阁查询失败进而使首页「我的修行」数据条整块不显示。
 *
 * 通过 information_schema 判断列是否存在，幂等可重复执行；失败只记日志、不阻断启动。
 */
@Component
public class CodexSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(CodexSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public CodexSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureColumns() {
        try {
            addColumn("cy_codex", "image",
                    "VARCHAR(255) NULL DEFAULT NULL COMMENT '卡片配图路径（前端静态资源）'");
        } catch (Exception e) {
            log.warn("图鉴字段自举失败（可手动执行 sql/upgrade_codex_image.sql）：{}", e.getMessage());
        }
    }

    /** 列不存在时补列；已存在则跳过。 */
    private void addColumn(String table, String column, String definition) {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, column);
            if (cnt != null && cnt == 0) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                log.info("图鉴字段自举：{} 新增列 {}", table, column);
            }
        } catch (Exception e) {
            log.warn("图鉴字段自举补列失败 {}.{}: {}", table, column, e.getMessage());
        }
    }
}
