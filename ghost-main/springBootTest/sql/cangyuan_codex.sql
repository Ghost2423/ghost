-- =============================================================
-- 沧元图 · 元界图鉴 建表 + 种子数据（幂等，可重复执行）
-- 技术栈：Spring Boot 3 + MyBatis-Plus + MySQL 8
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < sql\cangyuan_codex.sql
--       或在本机用任意 MySQL 客户端执行本文件。
-- 字符集：utf8mb4
-- 说明：单表 cy_codex 统管「神尊 / 功法 / 妖魔 / 秘境」四大界域，
--       配合后端 GET /api/codex（首页展示）与 POST /api/search（检索）。
-- =============================================================

CREATE DATABASE IF NOT EXISTS cangyuan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE cangyuan;

-- 元界图鉴表
CREATE TABLE IF NOT EXISTS cy_codex (
  id          INT UNSIGNED   NOT NULL AUTO_INCREMENT            COMMENT '主键',
  category    VARCHAR(16)    NOT NULL                          COMMENT '分类：神尊/功法/妖魔/秘境',
  name        VARCHAR(64)    NOT NULL                          COMMENT '名号',
  title       VARCHAR(64)    DEFAULT ''                        COMMENT '称号/标签（如「主角·刀道」「火妖」）',
  description VARCHAR(512)   DEFAULT ''                        COMMENT '简介',
  flavor      VARCHAR(255)   DEFAULT ''                        COMMENT '设定台词',
  created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cat_name (category, name),   -- 同分类下名号唯一
  KEY idx_category (category)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沧元图·元界图鉴';

-- 种子数据（INSERT IGNORE：已存在的 (category,name) 不会被覆盖）
INSERT IGNORE INTO cy_codex (category, name, title, description, flavor) VALUES
  -- —— 神尊 ——
  ('神尊', '孟川', '三修·神魔', '吴州城孟家之子，刀道天才。幼时母亲殒于妖爪，自此立誓斩尽妖邪。以刀入道，淬肉身、凝元神，踏上神尊之路。', '众生是朝阳，而我，向着朝阳。'),
  ('神尊', '柳七月', '血脉·凤凰', '沧元界最后一个凤凰血脉，孟川的未婚妻，在凤凰一脉被屠杀后幸存下来，并与父亲柳夜白来到东宁府住下，与孟川日久生情，修炼血脉之路后与孟川一同杀妖。', '赐尔等一场，腥风炎雨。'),
  ('神尊', '秦五', '神尊·引路', '孟川的引路与庇护者，神尊境前辈，镇守一方，点化无数人族天骄。', '神尊之路无尽，且看后来者登顶。'),
  ('神尊', '晏烬', '天骄·同辈', '与孟川同辈的人族天骄，剑意凌厉，于妖潮中淬炼成锋。', '剑锋所指，妖潮退散。'),

  -- —— 功法 ——
  ('功法', '元神淬体诀', '淬体', '凝元淬体，熔炼神魂，奠定肉身成圣之不灭根基，元界修行之入门根本。', '炼体如炼心，不破不立。'),
  ('功法', '水墨刀意', '刀道', '孟川主修之刀道极致，以意为锋，一刀山河动，斩妖于无形。', '一笔山河，一刀断生死。'),
  ('功法', '九霄雷法', '雷系', '引动九天雷霆，焚邪破障，天威浩荡，专克妖族阴晦之气。', '天命雷霆，代天罚恶。'),
  ('功法', '不灭身', '肉身', '肉身境之极致，滴血可重生，妖王难伤其分毫，护道之铁壁。', '肉身不灭，则道心不坠。'),

  -- —— 妖魔 ——
  ('妖魔', '噬魂魔', '妖魔·魂', '潜于暗影吞噬神魂，最善惑人心智，杀人于无形，妖族中的诡谲一支。', '你的恐惧，便是我的食粮。'),
  ('妖魔', '赤焰妖将', '火妖', '焚天赤焰，所过寸草不生，凶悍异常的大妖战将，统御一方妖兵。', '焚尽八荒，方显吾之凶威。'),
  ('妖魔', '蚀骨蛛皇', '虫妖', '盘踞幽渊，吐丝成阵，困敌于无形之网，猎物难逃其噬。', '落入吾网，便再无归途。'),
  ('妖魔', '黑铁妖王', '妖王', '统御一方妖族之妖王，气息如渊，人族神尊亦需忌惮三分。', '人族神尊？不过尔尔。'),

  -- —— 秘境 ——
  ('秘境', '不周遗墟', '遗迹', '上古不周山倾塌后遗留的墟境，藏有开天传承与上古兵器，探秘者众。', '开天之力，尽藏于此墟之中。'),
  ('秘境', '元初山', '圣地', '人族神尊修炼圣地，灵气如海，禁制重重，乃问道之至高所在。', '问道元初，方知神尊之浩瀚。'),
  ('秘境', '暗星界', '秘界', '游离于元界之外的暗星之界，妖族据守，星光尽灭，凶险万分。', '星光尽灭处，妖族称王。'),
  ('秘境', '归海秘境', '水境', '柳七月一脉守护的归海秘境，水脉本源孕养万物，藏有水神传承。', '海纳百川，孕养一方生灵。');

-- 校验：各分类收录数
SELECT category AS 界域, COUNT(*) AS 收录数
FROM cy_codex GROUP BY category ORDER BY FIELD(category, '神尊', '功法', '妖魔', '秘境');
