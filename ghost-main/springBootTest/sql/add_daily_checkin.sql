-- 沧元图 · 每日签到表（可重复执行）
-- 服务启动时会自举建表；此脚本供手动补建/核对结构使用。
USE cangyuan;

CREATE TABLE IF NOT EXISTS user_checkin (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT          COMMENT '主键',
  user_id     INT UNSIGNED NOT NULL                         COMMENT 'avatar_foundation.id',
  checkin_date DATE        DEFAULT NULL                     COMMENT '最近签到日期（每日一次）',
  streak      INT          NOT NULL DEFAULT 0               COMMENT '当前连续签到天数（断签重置）',
  total       INT          NOT NULL DEFAULT 0               COMMENT '累计签到天数',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·每日签到';
