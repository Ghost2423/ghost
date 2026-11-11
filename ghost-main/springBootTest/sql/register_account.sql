-- =============================================================
-- 沧元界 · 注册账号记录表（幂等，可重复执行）
-- 用途：每次「锻造神尊根基」（注册）成功后，除写入 avatar_foundation 外，
--       独立落一条注册账号记录到 user_register_record，永久留痕。
--       后台删除用户 / 修改权限 / 改名，都不会影响这张历史注册表。
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < sql\register_account.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS cangyuan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE cangyuan;

CREATE TABLE IF NOT EXISTS user_register_record (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT            COMMENT '主键（一次注册一条）',
  avatar_id   INT UNSIGNED    DEFAULT NULL                      COMMENT '关联的 avatar_foundation.id，用户被后台删除后此值仅作历史留痕',
  account     VARCHAR(64)     NOT NULL                          COMMENT '注册账号（deity_name）',
  password    VARCHAR(128)    NOT NULL                          COMMENT '注册密码（spell 召唤语，按现有约定明文存储）',
  region      VARCHAR(64)     DEFAULT NULL                      COMMENT '注册时填写的地域',
  myth_name   VARCHAR(64)     DEFAULT NULL                      COMMENT '觉醒神尊名号（展示用）',
  myth_story  VARCHAR(255)    DEFAULT NULL                      COMMENT '神话原型简介',
  agreed      TINYINT(1)      NOT NULL DEFAULT 0                COMMENT '是否立下心魔誓约',
  permission  TINYINT(1)      NOT NULL DEFAULT 1                COMMENT '注册时权限快照：1=默认用户，0=管理员',
  register_ip VARCHAR(64)     DEFAULT NULL                      COMMENT '注册来源 IP',
  source      VARCHAR(32)     NOT NULL DEFAULT 'forge'          COMMENT '来源：forge=前端注册 / migrate=历史数据回填',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (id),
  KEY idx_account (account),
  KEY idx_created_at (created_at),
  KEY idx_avatar_id (avatar_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元界·每次注册的账号留痕';

-- 历史数据回填：把建表前已存在的账号补成注册记录（重复执行不会重复插入）
INSERT INTO user_register_record
  (avatar_id, account, password, region, myth_name, myth_story, agreed, permission, register_ip, source, created_at)
SELECT a.id, a.deity_name, a.spell, a.region, a.myth_name, a.myth_story,
       a.agreed, a.permission, NULL, 'migrate', a.created_at
FROM avatar_foundation a
WHERE NOT EXISTS (
  SELECT 1 FROM user_register_record r WHERE r.avatar_id = a.id
);

-- 校验：当前累计注册账号数
SELECT COUNT(*) AS 已记录注册账号数 FROM user_register_record;
