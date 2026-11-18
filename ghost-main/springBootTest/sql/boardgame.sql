-- =============================================================
-- 沧元图 · 太虚弈局（桌游）数据库迁移脚本（幂等，可重复执行）
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < sql\boardgame.sql
-- 内容：
--   1) board_game_log  太虚弈局对局记录（落格收益/掷骰轨迹/通关状态）
--   说明：桌游产出的铜币与神尊力直接写入 user_cultivation（copper / power），
--         本表仅记录每局过程与收益明细，便于追溯与排行榜。
-- =============================================================

CREATE DATABASE IF NOT EXISTS cangyuan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE cangyuan;

-- -------------------------------------------------------------
-- 1) 太虚弈局对局记录表
--    一局一行：start 建行(status=playing) -> roll 逐步推进 -> 通关/次数用尽结算(status=finished)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS board_game_log (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT        COMMENT '主键',
  user_id       INT UNSIGNED   NOT NULL                        COMMENT 'avatar_foundation.id',
  status        VARCHAR(16)    NOT NULL DEFAULT 'playing'      COMMENT '对局状态：playing=进行中 / finished=已结束',
  position      INT            NOT NULL DEFAULT 0              COMMENT '当前所在格（0 起，BOARD_SIZE-1 为终点）',
  rolls         INT            NOT NULL DEFAULT 0              COMMENT '已掷骰次数',
  dice_log      VARCHAR(255)   DEFAULT NULL                    COMMENT '掷骰轨迹，如 3,6,2,5',
  gain_copper   INT            NOT NULL DEFAULT 0              COMMENT '本局累计获得铜币（落格+通关，不含报名费扣减）',
  gain_power    INT            NOT NULL DEFAULT 0              COMMENT '本局累计获得神尊力（落格+通关）',
  pass          TINYINT(1)     NOT NULL DEFAULT 0              COMMENT '是否通关登顶：1=通关 0=未通关',
  created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开局时间',
  updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近推进时间',
  finished_at   DATETIME       DEFAULT NULL                    COMMENT '结算时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·太虚弈局对局记录';

-- -------------------------------------------------------------
-- 2) 校验
-- -------------------------------------------------------------
SELECT COUNT(*) AS 对局记录数 FROM board_game_log;
