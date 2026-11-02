-- =============================================================
-- 沧元图 · 修炼系统 数据库迁移脚本（幂等，可重复执行）
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < sql\cultivation.sql
-- 内容：
--   1) cy_codex         元界图鉴（神尊/功法/妖魔/秘境）+ 功法价格/神尊力字段
--   2) user_cultivation 用户修炼状态（境界/神尊力/铜币/称号/秘境次数）
--   3) user_gongfa      用户已购功法
--   4) spar_log         切磋记录
-- =============================================================

CREATE DATABASE IF NOT EXISTS cangyuan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE cangyuan;

-- -------------------------------------------------------------
-- 1) 元界图鉴表 cy_codex（原后端已引用但未建表，此处补建）
--    功法类条目带 price（铜币价格）与 power_gain（购买后获得神尊力）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cy_codex (
  id          INT UNSIGNED  NOT NULL AUTO_INCREMENT          COMMENT '主键',
  category    VARCHAR(16)   NOT NULL                         COMMENT '分类：神尊/功法/妖魔/秘境',
  name        VARCHAR(64)   NOT NULL                         COMMENT '名号',
  title       VARCHAR(64)   DEFAULT NULL                     COMMENT '称号/标签',
  description VARCHAR(1024) DEFAULT NULL                     COMMENT '简介',
  flavor      VARCHAR(512)  DEFAULT NULL                     COMMENT '设定台词',
  price       INT           NOT NULL DEFAULT 100             COMMENT '功法售价（铜币），仅功法类有效',
  power_gain  INT           NOT NULL DEFAULT 40              COMMENT '购得功法获得的神尊力，仅功法类有效',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_category_name (category, name),
  KEY idx_category (category)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·元界图鉴';

-- 图鉴种子数据（INSERT IGNORE，幂等）
INSERT IGNORE INTO cy_codex (category, name, title, description, flavor, price, power_gain) VALUES
-- 神尊
('神尊', '孟川', '三修·神魔', '东宁府孟家弟子，绘画天才，年幼时母亲被妖族当面杀死，在那次经历中意外获得元神，开始向成为神尊努力，成为神尊后初心不变，以刀斩妖邪，以笔绘众生', '众生是朝阳，而我，向着朝阳。', 0, 0),
('神尊', '柳七月', '血脉·凤凰', '沧元界最后一个凤凰血脉，孟川的未婚妻，在凤凰一脉被屠杀后幸存下来，并与父亲柳夜白来到东宁府住下，与孟川日久生情，修炼血脉之路后与孟川一同杀妖。', '赐尔等一场，腥风炎雨。', 0, 0),
('神尊', '秦五', '神尊·引路', '孟川的引路与庇护者，神尊境前辈，镇守一方，点化无数人族天骄。', '神尊之路无尽，且看后来者登顶。', 0, 0),
('神尊', '晏烬', '天骄·同辈', '与孟川同辈的人族天骄，剑意凌厉，于妖潮中淬炼成锋。', '剑锋所指，妖潮退散。', 0, 0),
-- 功法（带售价与神尊力收益）
('功法', '元神淬体诀', '淬体', '凝元淬体，熔炼神魂，奠定肉身成圣之不灭根基，元界修行之入门根本。', '炼体如炼心，不破不立。', 100, 40),
('功法', '水墨刀意', '刀道', '孟川主修之刀道极致，以意为锋，一刀山河动，斩妖于无形。', '一笔山河，一刀断生死。', 150, 60),
('功法', '九霄雷法', '雷系', '引动九天雷霆，焚邪破障，天威浩荡，专克妖族阴晦之气。', '天命雷霆，代天罚恶。', 180, 70),
('功法', '不灭身', '肉身', '肉身境之极致，滴血可重生，妖王难伤其分毫，护道之铁壁。', '肉身不灭，则道心不坠。', 220, 90),
('功法', '心意刀', '刀道', '天地游龙，心意无尽', '人说谎言，刀说真相', 150, 60),
-- 妖魔
('妖魔', '噬魂魔', '妖魔·魂', '潜于暗影吞噬神魂，最善惑人心智，杀人于无形，妖族中的诡谲一支。', '你的恐惧，便是我的食粮。', 0, 0),
('妖魔', '赤焰妖将', '火妖', '焚天赤焰，所过寸草不生，凶悍异常的大妖战将，统御一方妖兵。', '焚尽八荒，方显吾之凶威。', 0, 0),
('妖魔', '蚀骨蛛皇', '虫妖', '盘踞幽渊，吐丝成阵，困敌于无形之网，猎物难逃其噬。', '落入吾网，便再无归途。', 0, 0),
('妖魔', '黑铁妖王', '妖王', '统御一方妖族之妖王，气息如渊，人族神尊亦需忌惮三分。', '人族神尊？不过尔尔。', 0, 0),
-- 秘境
('秘境', '不周遗墟', '遗迹', '上古不周山倾塌后遗留的墟境，藏有开天传承与上古兵器，探秘者众。', '开天之力，尽藏于此墟之中。', 0, 0),
('秘境', '元初山', '圣地', '人族神尊修炼圣地，灵气如海，禁制重重，乃问道之至高所在。', '问道元初，方知神尊之浩瀚。', 0, 0),
('秘境', '暗星界', '秘界', '游离于元界之外的暗星之界，妖族据守，星光尽灭，凶险万分。', '星光尽灭处，妖族称王。', 0, 0),
('秘境', '归海秘境', '水境', '柳七月一脉守护的归海秘境，水脉本源孕养万物，藏有水神传承。', '海纳百川，孕养一方生灵。', 0, 0);

-- -------------------------------------------------------------
-- 2) 用户修炼状态表（每注册用户一行；首次访问修炼殿时惰性初始化）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_cultivation (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT          COMMENT '主键',
  user_id      INT UNSIGNED NOT NULL                         COMMENT 'avatar_foundation.id',
  realm        VARCHAR(32)  NOT NULL DEFAULT '筑基'           COMMENT '当前境界（筑基起）',
  power        INT          NOT NULL DEFAULT 0               COMMENT '神尊力（修炼经验）',
  copper       INT          NOT NULL DEFAULT 200             COMMENT '铜币',
  title        VARCHAR(32)  DEFAULT NULL                     COMMENT '称号：封侯神尊/封王神尊',
  secret_date  DATE         DEFAULT NULL                     COMMENT '秘境修炼计数日期（每日重置）',
  secret_count INT          NOT NULL DEFAULT 0               COMMENT '当日已入秘境次数（上限 20）',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开辟时间',
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近修炼时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·修炼状态';

-- -------------------------------------------------------------
-- 3) 用户已购功法表（每功法限购一次，永久持有）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_gongfa (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT           COMMENT '主键',
  user_id     INT UNSIGNED NOT NULL                          COMMENT 'avatar_foundation.id',
  gongfa_name VARCHAR(64)  NOT NULL                          COMMENT '功法名（cy_codex.name）',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '购得时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_gongfa (user_id, gongfa_name)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·已购功法';

-- -------------------------------------------------------------
-- 4) 切磋记录表（双方各得神尊力与铜币）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS spar_log (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT           COMMENT '主键',
  initiator_id INT UNSIGNED NOT NULL                          COMMENT '发起者 user_id',
  target_id    INT UNSIGNED NOT NULL                          COMMENT '应战者 user_id',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '切磋时间',
  PRIMARY KEY (id),
  KEY idx_initiator (initiator_id),
  KEY idx_target (target_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·切磋记录';
