-- =============================================================
-- 沧元图 · 太虚弈局 扩展迁移脚本（PVP 对战 + 道具格，幂等可重复执行）
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < sql\boardgame-pvp.sql
-- 依赖：先执行 boardgame.sql
-- 内容：
--   1) board_game_log 增列 ward（镇厄符护盾，单人局）
--   2) board_pvp_match  PVP 对战记录（擂台赛 / AI 对战）
--   3) board_item       道具背包（落道具格获得，可主动使用）
--   说明：产出的铜币/神尊力仍直接写入 user_cultivation。
-- =============================================================

CREATE DATABASE IF NOT EXISTS cangyuan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE cangyuan;

-- -------------------------------------------------------------
-- 1) 幂等给 board_game_log 增加 ward 列（镇厄符：1=本局免疫下次劫难）
-- -------------------------------------------------------------
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'board_game_log' AND COLUMN_NAME = 'ward'
);
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE board_game_log ADD COLUMN ward TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''镇厄符护盾:1=生效中'' AFTER pass',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------
-- 2) PVP 对战记录表
--    status: waiting=等待应战 / playing=对战中 / finished=已结束
--    defender_ai=1 表示对手为天道 AI（服务端自动掷骰）
--    turn: challenger / defender（当前轮到谁）
--    winner: challenger / defender / draw
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS board_pvp_match (
  id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT        COMMENT '主键',
  challenger_id            INT UNSIGNED   NOT NULL                        COMMENT '挑战者 avatar_foundation.id',
  defender_id              INT UNSIGNED   DEFAULT NULL                    COMMENT '应战者 id；AI 对战为 NULL',
  defender_ai              TINYINT(1)     NOT NULL DEFAULT 0              COMMENT '是否天道AI对手：1=是',
  status                   VARCHAR(16)    NOT NULL DEFAULT 'waiting'      COMMENT 'waiting/playing/finished',
  challenger_pos           INT            NOT NULL DEFAULT 0              COMMENT '挑战者当前格',
  defender_pos             INT            NOT NULL DEFAULT 0              COMMENT '对手当前格',
  challenger_rolls         INT            NOT NULL DEFAULT 0              COMMENT '挑战者已掷次数',
  defender_rolls           INT            NOT NULL DEFAULT 0              COMMENT '对手已掷次数',
  turn                     VARCHAR(16)    NOT NULL DEFAULT 'challenger'   COMMENT '当前回合：challenger/defender',
  pot                      INT            NOT NULL DEFAULT 0              COMMENT '奖池铜币（双方报名费之和）',
  winner                   VARCHAR(16)    DEFAULT NULL                    COMMENT '胜者：challenger/defender/draw',
  challenger_gain_copper   INT            NOT NULL DEFAULT 0              COMMENT '挑战者途中所得铜币',
  challenger_gain_power    INT            NOT NULL DEFAULT 0              COMMENT '挑战者途中所得神尊力',
  defender_gain_copper     INT            NOT NULL DEFAULT 0              COMMENT '对手途中所得铜币',
  defender_gain_power      INT            NOT NULL DEFAULT 0              COMMENT '对手途中所得神尊力',
  challenger_ward          TINYINT(1)     NOT NULL DEFAULT 0              COMMENT '挑战者镇厄符护盾',
  defender_ward            TINYINT(1)     NOT NULL DEFAULT 0              COMMENT '对手镇厄符护盾',
  dice_log                 TEXT                                           COMMENT '对战掷骰轨迹（含归属）',
  created_at               DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  updated_at               DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近推进',
  finished_at              DATETIME       DEFAULT NULL                    COMMENT '结束时间',
  PRIMARY KEY (id),
  KEY idx_status (status),
  KEY idx_challenger (challenger_id),
  KEY idx_defender (defender_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·太虚弈局PVP对战记录';

-- -------------------------------------------------------------
-- 3) 道具背包表
--    item_key: leap(缩地符) / coin(聚宝符) / rain(甘霖符) / ward(镇厄符)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS board_item (
  id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id   INT UNSIGNED   NOT NULL                  COMMENT 'avatar_foundation.id',
  item_key  VARCHAR(32)    NOT NULL                  COMMENT '道具标识',
  cnt       INT            NOT NULL DEFAULT 0        COMMENT '持有数量',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_item (user_id, item_key)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·太虚弈局道具背包';

-- -------------------------------------------------------------
-- 4) 校验
-- -------------------------------------------------------------
SELECT 'board_game_log.ward' AS item, COUNT(*) AS cnt FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='board_game_log' AND COLUMN_NAME='ward'
UNION ALL SELECT 'board_pvp_match', COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='board_pvp_match'
UNION ALL SELECT 'board_item', COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='board_item';
