-- =============================================================
-- 沧元界 · 神尊根基 建库建表脚本
-- 数据来源：ghost-main/ghost-main/register.html（锻造神尊根基）
-- 说明：验证码（神纹）为一次性前端校验，不入库。
-- 字符集：utf8mb4（完整支持中文及生僻字）
-- 执行：mysql -u root -p < cangyuan_init.sql
-- =============================================================

-- 1) 建库
CREATE DATABASE IF NOT EXISTS cangyuan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE cangyuan;

-- 2) 神尊根基表（对应一次"锻造神尊根基"提交）
DROP TABLE IF EXISTS avatar_foundation;
CREATE TABLE avatar_foundation (
  id          INT UNSIGNED   NOT NULL AUTO_INCREMENT            COMMENT '主键',
  region      VARCHAR(64)    NOT NULL                          COMMENT '所属地域（register.html 的「地域」输入）',
  deity_name  VARCHAR(64)    NOT NULL                          COMMENT '觉醒神尊相（名号+封号，如「烛龙真君」）',
  myth_name   VARCHAR(64)    NOT NULL                          COMMENT '上古神尊原型（如「烛龙」「女娲」）',
  spell       VARCHAR(64)    NOT NULL                          COMMENT '召唤语（8–16 字，须含神尊名）',
  myth_story  VARCHAR(255)   DEFAULT NULL                      COMMENT '神话原型简介',
  agreed      TINYINT(1)     NOT NULL DEFAULT 0                COMMENT '是否立下心魔誓约（用户协议）',
  created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '锻造时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_deity_name (deity_name),   -- 登录按神尊相精确匹配，故唯一
  KEY idx_region (region),
  KEY idx_spell  (spell)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元界·神尊根基';

-- 3)（可选）为网站创建专用低权限账号，避免在前端直连 root
--    请将 'StrongPwd!2026' 替换为真实强密码后再执行
-- CREATE USER IF NOT EXISTS 'cangyuan_app'@'localhost' IDENTIFIED BY 'StrongPwd!2026';
-- GRANT SELECT, INSERT, UPDATE ON cangyuan.* TO 'cangyuan_app'@'localhost';
-- FLUSH PRIVILEGES;

-- 4) 校验
SHOW TABLES;
DESCRIBE avatar_foundation;
