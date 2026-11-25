-- =============================================================
-- 沧元图 · 图鉴表补 image 列（幂等，可重复执行）
-- 原因：后端 CodexEntry 实体已包含 image 字段，旧库 cy_codex 无此列时，
--       后台图鉴接口会报 Unknown column 'image' in 'field list'，
--       表现为「图鉴管理页面没有数据」。
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < sql\upgrade_codex_image.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS cangyuan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE cangyuan;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'cy_codex'
    AND COLUMN_NAME = 'image'
);
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE cy_codex ADD COLUMN image VARCHAR(255) NULL DEFAULT NULL COMMENT ''卡片配图路径（前端静态资源）'' AFTER color',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
