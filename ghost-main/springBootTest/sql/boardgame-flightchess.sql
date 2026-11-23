-- 沧元图 · 太虚弈局飞行棋改造（幂等，可重复执行）
-- 后端启动时会自动补列；此脚本供手动核对/补建使用。
USE cangyuan;

DROP PROCEDURE IF EXISTS cy_add_col;
DELIMITER $$
CREATE PROCEDURE cy_add_col(IN t VARCHAR(64), IN c VARCHAR(64), IN d VARCHAR(255))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = t AND COLUMN_NAME = c) THEN
    SET @sql = CONCAT('ALTER TABLE `', t, '` ADD COLUMN `', c, '` ', d);
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL cy_add_col('board_game_log', 'plane_states', 'VARCHAR(64) NOT NULL DEFAULT ''-1,-1,-1,-1'' COMMENT ''飞行棋4架飞机位置'' AFTER `position`');
CALL cy_add_col('board_game_log', 'guards',       'VARCHAR(32) NOT NULL DEFAULT ''1,1'' COMMENT ''劫妖守军存活状态'' AFTER `plane_states`');
CALL cy_add_col('board_game_log', 'active_plane', 'INT NOT NULL DEFAULT 0 COMMENT ''当前选中飞机编号'' AFTER `guards`');
CALL cy_add_col('board_game_log', 'extra_roll',   'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''掷6再掷标记'' AFTER `active_plane`');

CALL cy_add_col('board_pvp_match', 'challenger_planes', 'VARCHAR(64) NOT NULL DEFAULT ''-1,-1,-1,-1'' COMMENT ''挑战者4架飞机位置'' AFTER `defender_pos`');
CALL cy_add_col('board_pvp_match', 'defender_planes',   'VARCHAR(64) NOT NULL DEFAULT ''-1,-1,-1,-1'' COMMENT ''应战者4架飞机位置'' AFTER `challenger_planes`');
CALL cy_add_col('board_pvp_match', 'challenger_active', 'INT NOT NULL DEFAULT 0 COMMENT ''挑战者选中飞机'' AFTER `defender_planes`');
CALL cy_add_col('board_pvp_match', 'defender_active',   'INT NOT NULL DEFAULT 0 COMMENT ''应战者选中飞机'' AFTER `challenger_active`');
CALL cy_add_col('board_pvp_match', 'extra_roll',        'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''掷6再掷标记'' AFTER `defender_active`');

DROP PROCEDURE IF EXISTS cy_add_col;
