-- =============================================================
-- 沧元界 · 神尊根基 建库建表脚本（幂等，可重复执行）
-- 技术栈：Spring Boot 3 + MyBatis-Plus + MySQL 8
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < sql\cangyuan.sql
-- 字符集：utf8mb4（完整支持中文及生僻字）
-- =============================================================

CREATE DATABASE IF NOT EXISTS cangyuan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE cangyuan;

-- 1) 神尊根基表（对应一次「锻造神尊根基」提交）
CREATE TABLE IF NOT EXISTS avatar_foundation (
  id          INT UNSIGNED   NOT NULL AUTO_INCREMENT            COMMENT '主键',
  region      VARCHAR(64)    NOT NULL                          COMMENT '所属地域（注册页的「地域」输入）',
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

-- 2) 种子数据（INSERT IGNORE：已存在的神尊相不会被覆盖）
INSERT IGNORE INTO avatar_foundation (region, deity_name, myth_name, spell, myth_story, agreed) VALUES
('东宁府', '九天应元雷声普化天尊', '孟川',     '诸般雷霆，遵吾号令', '沧元图主角孟川觉醒之神尊相，雷霆加护，万物臣服。', 1),
('幽都山', '烛龙真君',             '烛龙',     '烛龙衔烛，昼夜由心', '衔烛而照幽都，睁眼为昼，闭眼为夜。', 1),
('昆仑山', '西王母元君',           '西王母',   '西王母临，瑶池生辉', '昆仑之主，瑶池生辉，掌不死之药。', 1);

-- 3) 校验
SELECT COUNT(*) AS 已锻造神尊数 FROM avatar_foundation;
