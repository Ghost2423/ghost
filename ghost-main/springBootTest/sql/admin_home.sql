-- =============================================================
-- 沧元图 · 后台管理系统（首页数据管理）建库建表脚本
-- 技术栈：Spring Boot 3 + MyBatis-Plus + MySQL 8
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < sql\admin_home.sql
-- 特性：幂等，可重复执行（已存在的表/列/数据不会被破坏）
--
-- 首页「展示功能」对应的数据落库设计：
--   1) cy_codex         元界图鉴（神尊/功法/妖魔/秘境）—— 首页四大界域卡片 + 检索
--                       本次扩展 color（卡片色）/ sort_no（排序）/ status（上下架）
--   2) cy_home_section  首页「核心界域」展示框 —— 标题/图标/主色/简介/排序/显隐
--   3) cy_home_news     首页「元界动态」滚动条 —— 标签/文案/跳转关键词/颜色/排序/显隐
--   4) cy_site_setting  站点文案（品牌名/英雄标题/检索占位/热门标签/页脚）
--   5) cy_admin_log     后台操作审计日志
-- =============================================================

CREATE DATABASE IF NOT EXISTS cangyuan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE cangyuan;

-- -------------------------------------------------------------
-- 0) 幂等加列工具：列不存在时补列
-- -------------------------------------------------------------
DROP PROCEDURE IF EXISTS cy_add_column;
DROP PROCEDURE IF EXISTS cy_add_index;

DELIMITER $$

CREATE PROCEDURE cy_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl VARCHAR(512))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE cy_add_index(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_ddl VARCHAR(512))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD ', p_ddl);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

DELIMITER ;

-- -------------------------------------------------------------
-- 1) 元界图鉴扩展：卡片主色 / 排序 / 上下架状态
-- -------------------------------------------------------------
ALTER TABLE cy_codex CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CALL cy_add_column('cy_codex', 'color',   '`color` VARCHAR(16) NOT NULL DEFAULT ''#e9c46a'' COMMENT ''卡片主色'' AFTER `flavor`');
CALL cy_add_column('cy_codex', 'sort_no', '`sort_no` INT NOT NULL DEFAULT 0 COMMENT ''排序（越小越前）'' AFTER `color`');
CALL cy_add_column('cy_codex', 'status',  '`status` TINYINT NOT NULL DEFAULT 1 COMMENT ''状态：1=上架（首页展示） 0=下架'' AFTER `sort_no`');
CALL cy_add_index('cy_codex', 'idx_status_sort', 'KEY `idx_status_sort` (`status`, `category`, `sort_no`)');

-- 按分类补齐卡片主色（与首页四大界域主色一致）
UPDATE cy_codex SET color = '#e9c46a' WHERE category = '神尊' AND (color IS NULL OR color = '' OR color = '#e9c46a');
UPDATE cy_codex SET color = '#2dd4bf' WHERE category = '功法' AND (color IS NULL OR color = '' OR color = '#e9c46a');
UPDATE cy_codex SET color = '#ef4454' WHERE category = '妖魔' AND (color IS NULL OR color = '' OR color = '#e9c46a');
UPDATE cy_codex SET color = '#5eead4' WHERE category = '秘境' AND (color IS NULL OR color = '' OR color = '#e9c46a');

-- -------------------------------------------------------------
-- 2) 首页「核心界域」展示框
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cy_home_section (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT             COMMENT '主键',
  section_type VARCHAR(16)  NOT NULL                            COMMENT '界域类型：神尊/功法/妖魔/秘境（与图鉴 category 对应）',
  title        VARCHAR(32)  NOT NULL                            COMMENT '展示框标题，如「神尊殿」',
  icon         VARCHAR(8)   NOT NULL DEFAULT '❖'                COMMENT '展示框图标',
  color        VARCHAR(16)  NOT NULL DEFAULT '#e9c46a'          COMMENT '展示框主色',
  description  VARCHAR(255) NOT NULL DEFAULT ''                 COMMENT '展示框简介',
  sort_no      INT          NOT NULL DEFAULT 0                  COMMENT '排序（越小越前）',
  status       TINYINT      NOT NULL DEFAULT 1                  COMMENT '状态：1=展示 0=隐藏',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_section_type (section_type),
  KEY idx_status_sort (status, sort_no)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·首页核心界域展示框';

INSERT IGNORE INTO cy_home_section (section_type, title, icon, color, description, sort_no, status) VALUES
  ('神尊', '神尊殿',   '✦', '#e9c46a', '人族神尊列传，孟川、柳七月、秦五……元界守护者的名号与传说。', 10, 1),
  ('功法', '修炼功法', '✧', '#2dd4bf', '元神淬体、刀道雷法，淬肉身、凝神魂的修行法门总纲。',         20, 1),
  ('妖魔', '妖魔图鉴', '☠', '#ef4454', '妖族谱录：噬魂魔、赤焰妖将、妖王……识其形，方能斩其势。',     30, 1),
  ('秘境', '元界秘境', '❖', '#5eead4', '不周遗墟、元初山、暗星界，藏上古传承的凶险界域。',             40, 1);

-- -------------------------------------------------------------
-- 3) 首页「元界动态」滚动条
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cy_home_news (
  id         INT UNSIGNED NOT NULL AUTO_INCREMENT             COMMENT '主键',
  tag        VARCHAR(16)  NOT NULL DEFAULT '公告'              COMMENT '标签：公告/预警/活动/历练',
  content    VARCHAR(255) NOT NULL                             COMMENT '动态文案',
  keyword    VARCHAR(64)  NOT NULL DEFAULT ''                  COMMENT '点击后跳转检索的关键词',
  color      VARCHAR(16)  NOT NULL DEFAULT '#e9c46a'           COMMENT '文字主色',
  sort_no    INT          NOT NULL DEFAULT 0                   COMMENT '排序（越小越前）',
  status     TINYINT      NOT NULL DEFAULT 1                   COMMENT '状态：1=展示 0=隐藏',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tag_content (tag, content),
  KEY idx_status_sort (status, sort_no)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·首页元界动态';

INSERT IGNORE INTO cy_home_news (tag, content, keyword, color, sort_no, status) VALUES
  ('公告', '神尊归位大典 · 新晋神尊觉醒仪式将启', '神尊',     '#e9c46a', 10, 1),
  ('预警', '妖潮预警 · 东海之滨妖气骤盛',         '噬魂魔',   '#ef4454', 20, 1),
  ('活动', '不周遗墟开放 · 探秘可得上古传承',     '不周遗墟', '#5eead4', 30, 1),
  ('历练', '元初山论道 · 同辈天骄切磋在即',       '元初山',   '#2dd4bf', 40, 1);

-- -------------------------------------------------------------
-- 4) 站点文案（首页可配置文本，key-value）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cy_site_setting (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT            COMMENT '主键',
  setting_key   VARCHAR(64)  NOT NULL                           COMMENT '配置键',
  setting_value VARCHAR(1024) NOT NULL DEFAULT ''               COMMENT '配置值',
  remark        VARCHAR(128) NOT NULL DEFAULT ''                COMMENT '说明',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_setting_key (setting_key)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·站点文案配置';

INSERT IGNORE INTO cy_site_setting (setting_key, setting_value, remark) VALUES
  ('site_title',        '沧元图',                                  '品牌名（导航栏印章旁）'),
  ('site_subtitle',     'CANG YUAN TU · 元界',                     '品牌副标题'),
  ('hero_badge',        '元界 · 神尊归位',                          '首屏徽标'),
  ('hero_title',        '沧元界 · 检索万物',                        '首屏主标题'),
  ('hero_subtitle',     '刀斩妖邪，笔绘众生。于元界之中，寻你的神尊、功法与秘境。', '首屏副标题'),
  ('search_placeholder','输入神尊名号、功法、妖魔或秘境…',            '首屏检索占位文案'),
  ('hot_tags',          '孟川,柳七月,元神淬体诀,水墨刀意,噬魂魔,不周遗墟', '热门检索标签（英文逗号分隔）'),
  ('footer_text',       '© 沧元图主题演示 · 仅作展示用途，非官方站点',  '页脚文案');

-- -------------------------------------------------------------
-- 5) 后台操作审计日志
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cy_admin_log (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT            COMMENT '主键',
  admin_id    INT UNSIGNED NOT NULL DEFAULT 0                 COMMENT '管理员用户 id',
  admin_name  VARCHAR(64)  NOT NULL DEFAULT ''                COMMENT '管理员账号',
  action      VARCHAR(32)  NOT NULL                           COMMENT '动作：新增/修改/删除/上下架/登录',
  target_type VARCHAR(32)  NOT NULL DEFAULT ''                COMMENT '目标类型：图鉴/展示框/动态/文案/用户',
  target_id   VARCHAR(32)  NOT NULL DEFAULT ''                COMMENT '目标 id',
  detail      VARCHAR(512) NOT NULL DEFAULT ''                COMMENT '操作摘要',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_created (created_at),
  KEY idx_admin (admin_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·后台操作日志';

DROP PROCEDURE IF EXISTS cy_add_column;
DROP PROCEDURE IF EXISTS cy_add_index;

-- 校验
SELECT '图鉴' AS 模块, COUNT(*) AS 条数 FROM cy_codex
UNION ALL SELECT '核心界域', COUNT(*) FROM cy_home_section
UNION ALL SELECT '元界动态', COUNT(*) FROM cy_home_news
UNION ALL SELECT '站点文案', COUNT(*) FROM cy_site_setting;
