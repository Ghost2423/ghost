-- =============================================================
-- 沧元界 · 论道论坛 建库建表脚本（幂等，可重复执行）
-- 三张表：cy_forum_post（帖子）/ cy_forum_reply（回复）/ cy_forum_like（点赞）
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < sql\forum.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS cangyuan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE cangyuan;

-- 1) 论道帖
CREATE TABLE IF NOT EXISTS cy_forum_post (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT            COMMENT '帖子 ID',
  category      VARCHAR(24)     NOT NULL DEFAULT '论道'           COMMENT '板块：论道/切磋/妖情/闲谈',
  title         VARCHAR(120)    NOT NULL                          COMMENT '论题',
  content       TEXT            NOT NULL                          COMMENT '正文',
  author_name   VARCHAR(64)     NOT NULL                          COMMENT '发帖账号（avatar_foundation.deity_name）',
  author_alias  VARCHAR(64)     DEFAULT NULL                      COMMENT '展示名号（神尊封号 myth_name）',
  view_count    INT UNSIGNED    NOT NULL DEFAULT 0                COMMENT '阅卷数',
  like_count    INT UNSIGNED    NOT NULL DEFAULT 0                COMMENT '共鸣数（点赞）',
  reply_count   INT UNSIGNED    NOT NULL DEFAULT 0                COMMENT '回帖数',
  is_pinned     TINYINT(1)      NOT NULL DEFAULT 0                COMMENT '置顶：1=置顶 0=普通',
  status        TINYINT(1)      NOT NULL DEFAULT 1                COMMENT '状态：1=正常 0=已隐藏（作者删除/后台屏蔽）',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发帖时间',
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  last_reply_at DATETIME        DEFAULT NULL                      COMMENT '最后回帖时间（用于最新排序）',
  PRIMARY KEY (id),
  KEY idx_category (category),
  KEY idx_status_pinned (status, is_pinned, last_reply_at),
  KEY idx_author (author_name),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元界·论道论坛帖子';

-- 2) 回帖（楼层）
CREATE TABLE IF NOT EXISTS cy_forum_reply (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT            COMMENT '回复 ID',
  post_id       BIGINT UNSIGNED NOT NULL                          COMMENT '所属帖子 ID',
  author_name   VARCHAR(64)     NOT NULL                          COMMENT '回帖账号',
  author_alias  VARCHAR(64)     DEFAULT NULL                      COMMENT '展示名号',
  content       TEXT            NOT NULL                          COMMENT '回帖正文',
  floor_no      INT UNSIGNED    NOT NULL DEFAULT 1                COMMENT '楼层（从 2 楼起，1 楼为主帖）',
  status        TINYINT(1)      NOT NULL DEFAULT 1                COMMENT '状态：1=正常 0=已隐藏',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回帖时间',
  PRIMARY KEY (id),
  KEY idx_post_floor (post_id, floor_no),
  KEY idx_author (author_name),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元界·论道论坛回帖';

-- 3) 共鸣（点赞），一人一帖仅一次，可取消
CREATE TABLE IF NOT EXISTS cy_forum_like (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT            COMMENT '主键',
  post_id       BIGINT UNSIGNED NOT NULL                          COMMENT '帖子 ID',
  user_name     VARCHAR(64)     NOT NULL                          COMMENT '点赞账号',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_post_user (post_id, user_name),
  KEY idx_user (user_name)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元界·论道论坛共鸣记录';

-- 4) 示例论题（首次执行才插入，重复执行不会重复）
INSERT INTO cy_forum_post
  (category, title, content, author_name, author_alias, view_count, like_count, reply_count, is_pinned, status, last_reply_at)
SELECT * FROM (
  SELECT '论道' AS category,
         '【坛规】元初山论道 · 入坛须知' AS title,
         '诸位道友：论道坛乃元界同道论法之所。一不论人身攻击，二不泄根基口令，三不造妖言惑众。凡有道法疑难、秘境见闻、刀意心得，皆可开帖共议。愿诸君以道会友，共守人族山河。' AS content,
         '沧元祖师' AS author_name,
         '沧元祖师' AS author_alias,
         128 AS view_count, 36 AS like_count, 2 AS reply_count,
         1 AS is_pinned, 1 AS status, NOW() AS last_reply_at
) s
WHERE NOT EXISTS (SELECT 1 FROM cy_forum_post p WHERE p.title = s.title);

INSERT INTO cy_forum_post
  (category, title, content, author_name, author_alias, view_count, like_count, reply_count, is_pinned, status, last_reply_at)
SELECT * FROM (
  SELECT '切磋' AS category,
         '水墨刀意第七式，如何破「无相」之境？' AS title,
         '近日于元初山演武台与同辈切磋，刀意至第七式便被「无相」化去，锋芒尽散。窃以为当以元神凝意为锋，不以形取势，然屡试未成。敢问诸位道友，可有破局之法？' AS content,
         '孟川' AS author_name,
         '水墨刀君' AS author_alias,
         96 AS view_count, 24 AS like_count, 1 AS reply_count,
         0 AS is_pinned, 1 AS status, NOW() AS last_reply_at
) s
WHERE NOT EXISTS (SELECT 1 FROM cy_forum_post p WHERE p.title = s.title);

INSERT INTO cy_forum_post
  (category, title, content, author_name, author_alias, view_count, like_count, reply_count, is_pinned, status, last_reply_at)
SELECT * FROM (
  SELECT '妖情' AS category,
         '东海之滨妖气骤盛 · 巡守见闻' AS title,
         '昨夜三更，东宁府外海雾泛赤，巡守见赤焰妖将踪迹。妖潮或提前至，各府道友宜早备丹药与符箓，勿单独夜行。若有最新妖情，烦请续于此帖之下。' AS content,
         '柳七月' AS author_name,
         '凤凰血脉' AS author_alias,
         73 AS view_count, 18 AS like_count, 0 AS reply_count,
         0 AS is_pinned, 1 AS status, NULL AS last_reply_at
) s
WHERE NOT EXISTS (SELECT 1 FROM cy_forum_post p WHERE p.title = s.title);

-- 5) 示例回帖（依附示例帖，重复执行不会重复）
INSERT INTO cy_forum_reply (post_id, author_name, author_alias, content, floor_no, status)
SELECT p.id, '秦五', '元初山尊者', '规矩简明，照此论道即可。后来者可先阅此帖，再开新题。', 2, 1
FROM cy_forum_post p
WHERE p.title = '【坛规】元初山论道 · 入坛须知'
  AND NOT EXISTS (SELECT 1 FROM cy_forum_reply r WHERE r.post_id = p.id AND r.floor_no = 2);

INSERT INTO cy_forum_reply (post_id, author_name, author_alias, content, floor_no, status)
SELECT p.id, '晏烬', '剑锋天骄', '已阅。愿与诸君共守元界清朗。', 3, 1
FROM cy_forum_post p
WHERE p.title = '【坛规】元初山论道 · 入坛须知'
  AND NOT EXISTS (SELECT 1 FROM cy_forum_reply r WHERE r.post_id = p.id AND r.floor_no = 3);

INSERT INTO cy_forum_reply (post_id, author_name, author_alias, content, floor_no, status)
SELECT p.id, '秦五', '元初山尊者', '「无相」化形不化意。刀在意先，意断则势自散。不妨先收刀三日，再以一笔落尽山河。', 2, 1
FROM cy_forum_post p
WHERE p.title = '水墨刀意第七式，如何破「无相」之境？'
  AND NOT EXISTS (SELECT 1 FROM cy_forum_reply r WHERE r.post_id = p.id AND r.floor_no = 2);

-- 校验
SELECT
  (SELECT COUNT(*) FROM cy_forum_post)  AS 论道帖数,
  (SELECT COUNT(*) FROM cy_forum_reply) AS 回帖数,
  (SELECT COUNT(*) FROM cy_forum_like)  AS 共鸣数;
