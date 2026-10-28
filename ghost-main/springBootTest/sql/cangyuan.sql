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
  permission  TINYINT(1)     NOT NULL DEFAULT 1                COMMENT '权限：1=默认用户，0=管理员（管理员可登录后台管理系统）',
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

-- 3.1) 内置管理员账号（permission=0；账号 沧元祖师，密码 admin123，明文存储）
INSERT IGNORE INTO avatar_foundation (region, deity_name, myth_name, spell, myth_story, agreed, permission) VALUES
('天枢殿', '沧元祖师', '元界天帝', 'admin123', '元界天帝：执掌天枢，统御元界后台。', 1, 0);

-- 3.2) 已有库升级：为旧表补上权限列（全新建库执行 3.1 前的建表语句已含该列，此处仅旧库需要）
-- ALTER TABLE avatar_foundation
--   ADD COLUMN permission TINYINT(1) NOT NULL DEFAULT 1 COMMENT '权限：1=默认用户，0=管理员' AFTER agreed;
-- 将某个已有用户提升为管理员（示例）：
-- UPDATE avatar_foundation SET permission = 0 WHERE deity_name = '你的账号';

-- 3) 校验
SELECT COUNT(*) AS 已锻造神尊数 FROM avatar_foundation;
