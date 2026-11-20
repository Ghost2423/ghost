/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : cangyuan

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 20/11/2026 17:03:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for avatar_foundation
-- ----------------------------
DROP TABLE IF EXISTS `avatar_foundation`;
CREATE TABLE `avatar_foundation`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `region` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属地域（注册页的「地域」输入）',
  `deity_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '觉醒神尊相（名号+封号，如「烛龙真君」）',
  `myth_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '上古神尊原型（如「烛龙」「女娲」）',
  `spell` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '召唤语（8–16 字，须含神尊名）',
  `myth_story` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '神话原型简介',
  `agreed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否立下心魔誓约（用户协议）',
  `permission` tinyint(1) NOT NULL DEFAULT 1 COMMENT '权限：1=默认用户，0=管理员（管理员可登录后台管理系统）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '锻造时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_deity_name`(`deity_name` ASC) USING BTREE,
  INDEX `idx_region`(`region` ASC) USING BTREE,
  INDEX `idx_spell`(`spell` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元界·神尊根基' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of avatar_foundation
-- ----------------------------
INSERT INTO `avatar_foundation` VALUES (1, '东宁府', '九天应元雷声普化天尊', '孟川', '诸般雷霆，遵吾号令', '沧元图主角孟川觉醒之神尊相，雷霆加护，万物臣服。', 1, 1, '2026-11-20 15:37:49');
INSERT INTO `avatar_foundation` VALUES (2, '幽都山', '烛龙真君', '烛龙', '烛龙衔烛，昼夜由心', '衔烛而照幽都，睁眼为昼，闭眼为夜。', 1, 1, '2026-11-20 15:37:49');
INSERT INTO `avatar_foundation` VALUES (3, '昆仑山', '西王母元君', '西王母', '西王母临，瑶池生辉', '昆仑之主，瑶池生辉，掌不死之药。', 1, 1, '2026-11-20 15:37:49');
INSERT INTO `avatar_foundation` VALUES (4, '天枢殿', '沧元祖师', '元界天帝', 'admin123', '元界天帝：执掌天枢，统御元界后台。', 1, 0, '2026-11-20 15:37:49');
INSERT INTO `avatar_foundation` VALUES (6, '测试府', '111', '女娲道尊·4657', '111111', '女娲：抟土造人，炼石补天，庇佑苍生。', 1, 1, '2026-11-20 16:06:14');
INSERT INTO `avatar_foundation` VALUES (12, '测试府', '12345', '神农道尊·2755', '123456', '神农：尝百草、播五谷，惠泽万民。', 1, 1, '2026-11-20 16:09:03');

-- ----------------------------
-- Table structure for board_game_log
-- ----------------------------
DROP TABLE IF EXISTS `board_game_log`;
CREATE TABLE `board_game_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int UNSIGNED NOT NULL COMMENT 'avatar_foundation.id',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'playing' COMMENT '对局状态：playing=进行中 / finished=已结束',
  `position` int NOT NULL DEFAULT 0 COMMENT '当前所在格（0 起，BOARD_SIZE-1 为终点）',
  `rolls` int NOT NULL DEFAULT 0 COMMENT '已掷骰次数',
  `dice_log` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '掷骰轨迹，如 3,6,2,5',
  `gain_copper` int NOT NULL DEFAULT 0 COMMENT '本局累计获得铜币（落格+通关，不含报名费扣减）',
  `gain_power` int NOT NULL DEFAULT 0 COMMENT '本局累计获得神尊力（落格+通关）',
  `pass` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否通关登顶：1=通关 0=未通关',
  `ward` tinyint(1) NOT NULL DEFAULT 0 COMMENT '镇厄符护盾:1=生效中',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开局时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近推进时间',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '结算时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_created`(`user_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元图·太虚弈局对局记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of board_game_log
-- ----------------------------

-- ----------------------------
-- Table structure for board_item
-- ----------------------------
DROP TABLE IF EXISTS `board_item`;
CREATE TABLE `board_item`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int UNSIGNED NOT NULL COMMENT 'avatar_foundation.id',
  `item_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '道具标识',
  `cnt` int NOT NULL DEFAULT 0 COMMENT '持有数量',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_item`(`user_id` ASC, `item_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元图·太虚弈局道具背包' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of board_item
-- ----------------------------

-- ----------------------------
-- Table structure for board_pvp_match
-- ----------------------------
DROP TABLE IF EXISTS `board_pvp_match`;
CREATE TABLE `board_pvp_match`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `challenger_id` int UNSIGNED NOT NULL COMMENT '挑战者 avatar_foundation.id',
  `defender_id` int UNSIGNED NULL DEFAULT NULL COMMENT '应战者 id；AI 对战为 NULL',
  `defender_ai` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否天道AI对手：1=是',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'waiting' COMMENT 'waiting/playing/finished',
  `challenger_pos` int NOT NULL DEFAULT 0 COMMENT '挑战者当前格',
  `defender_pos` int NOT NULL DEFAULT 0 COMMENT '对手当前格',
  `challenger_rolls` int NOT NULL DEFAULT 0 COMMENT '挑战者已掷次数',
  `defender_rolls` int NOT NULL DEFAULT 0 COMMENT '对手已掷次数',
  `turn` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'challenger' COMMENT '当前回合：challenger/defender',
  `pot` int NOT NULL DEFAULT 0 COMMENT '奖池铜币（双方报名费之和）',
  `winner` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '胜者：challenger/defender/draw',
  `challenger_gain_copper` int NOT NULL DEFAULT 0 COMMENT '挑战者途中所得铜币',
  `challenger_gain_power` int NOT NULL DEFAULT 0 COMMENT '挑战者途中所得神尊力',
  `defender_gain_copper` int NOT NULL DEFAULT 0 COMMENT '对手途中所得铜币',
  `defender_gain_power` int NOT NULL DEFAULT 0 COMMENT '对手途中所得神尊力',
  `challenger_ward` tinyint(1) NOT NULL DEFAULT 0 COMMENT '挑战者镇厄符护盾',
  `defender_ward` tinyint(1) NOT NULL DEFAULT 0 COMMENT '对手镇厄符护盾',
  `dice_log` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '对战掷骰轨迹（含归属）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近推进',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_challenger`(`challenger_id` ASC) USING BTREE,
  INDEX `idx_defender`(`defender_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元图·太虚弈局PVP对战记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of board_pvp_match
-- ----------------------------

-- ----------------------------
-- Table structure for cy_admin_log
-- ----------------------------
DROP TABLE IF EXISTS `cy_admin_log`;
CREATE TABLE `cy_admin_log`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `admin_id` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '管理员用户 id',
  `admin_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '管理员账号',
  `action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作：新增/修改/删除/上下架/登录',
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '目标类型：图鉴/展示框/动态/文案/用户',
  `target_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '目标 id',
  `detail` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '操作摘要',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE,
  INDEX `idx_admin`(`admin_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元图·后台操作日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cy_admin_log
-- ----------------------------
INSERT INTO `cy_admin_log` VALUES (1, 4, '沧元祖师', '修改', '修炼数据', '6', '账号 6 · {realm=混洞, power=500, copper=999} · 影响 1 人', '2026-11-20 16:35:55');
INSERT INTO `cy_admin_log` VALUES (2, 4, '沧元祖师', '修改', '修炼数据', 'ALL', '全部账号 · {copper=888} · 影响 6 人', '2026-11-20 16:36:00');
INSERT INTO `cy_admin_log` VALUES (3, 4, '沧元祖师', '修改', '用户', 'ALL', '批量资料 {region=测试地域} · 影响 6 人', '2026-11-20 16:36:06');

-- ----------------------------
-- Table structure for cy_codex
-- ----------------------------
DROP TABLE IF EXISTS `cy_codex`;
CREATE TABLE `cy_codex`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类：神尊/功法/妖魔/秘境',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '名号',
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '称号/标签',
  `description` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '简介',
  `flavor` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '设定台词',
  `color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#e9c46a' COMMENT '卡片主色',
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '卡片配图路径（前端静态资源）',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序（越小越前）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1=上架（首页展示） 0=下架',
  `price` int NOT NULL DEFAULT 100 COMMENT '功法售价（铜币），仅功法类有效',
  `power_gain` int NOT NULL DEFAULT 40 COMMENT '购得功法获得的神尊力，仅功法类有效',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_category_name`(`category` ASC, `name` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_status_sort`(`status` ASC, `category` ASC, `sort_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元图·元界图鉴' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cy_codex
-- ----------------------------
INSERT INTO `cy_codex` VALUES (1, '神尊', '孟川', '三修·神魔', '东宁府孟家弟子，绘画天才，年幼时母亲被妖族当面杀死，在那次经历中意外获得元神，开始向成为神尊努力，成为神尊后初心不变，以刀斩妖邪，以笔绘众生', '众生是朝阳，而我，向着朝阳。', '#e9c46a', NULL, 0, 1, 0, 0, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (2, '神尊', '柳七月', '血脉·凤凰', '沧元界最后一个凤凰血脉，孟川的未婚妻，在凤凰一脉被屠杀后幸存下来，并与父亲柳夜白来到东宁府住下，与孟川日久生情，修炼血脉之路后与孟川一同杀妖。', '赐尔等一场，腥风炎雨。', '#e9c46a', NULL, 0, 1, 0, 0, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (3, '神尊', '秦五', '神尊·引路', '孟川的引路与庇护者，神尊境前辈，镇守一方，点化无数人族天骄。', '神尊之路无尽，且看后来者登顶。', '#e9c46a', NULL, 0, 1, 0, 0, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (4, '神尊', '晏烬', '天骄·同辈', '与孟川同辈的人族天骄，剑意凌厉，于妖潮中淬炼成锋。', '剑锋所指，妖潮退散。', '#e9c46a', NULL, 0, 1, 0, 0, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (5, '功法', '元神淬体诀', '淬体', '凝元淬体，熔炼神魂，奠定肉身成圣之不灭根基，元界修行之入门根本。', '炼体如炼心，不破不立。', '#2dd4bf', NULL, 0, 1, 100, 40, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (6, '功法', '水墨刀意', '刀道', '孟川主修之刀道极致，以意为锋，一刀山河动，斩妖于无形。', '一笔山河，一刀断生死。', '#2dd4bf', NULL, 0, 1, 150, 60, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (7, '功法', '九霄雷法', '雷系', '引动九天雷霆，焚邪破障，天威浩荡，专克妖族阴晦之气。', '天命雷霆，代天罚恶。', '#2dd4bf', NULL, 0, 1, 180, 70, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (8, '功法', '不灭身', '肉身', '肉身境之极致，滴血可重生，妖王难伤其分毫，护道之铁壁。', '肉身不灭，则道心不坠。', '#2dd4bf', NULL, 0, 1, 220, 90, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (9, '功法', '心意刀', '刀道', '天地游龙，心意无尽', '人说谎言，刀说真相', '#2dd4bf', NULL, 0, 1, 150, 60, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (10, '妖魔', '噬魂魔', '妖魔·魂', '潜于暗影吞噬神魂，最善惑人心智，杀人于无形，妖族中的诡谲一支。', '你的恐惧，便是我的食粮。', '#ef4454', NULL, 0, 1, 0, 0, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (11, '妖魔', '赤焰妖将', '火妖', '焚天赤焰，所过寸草不生，凶悍异常的大妖战将，统御一方妖兵。', '焚尽八荒，方显吾之凶威。', '#ef4454', NULL, 0, 1, 0, 0, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (12, '妖魔', '蚀骨蛛皇', '虫妖', '盘踞幽渊，吐丝成阵，困敌于无形之网，猎物难逃其噬。', '落入吾网，便再无归途。', '#ef4454', NULL, 0, 1, 0, 0, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (13, '妖魔', '黑铁妖王', '妖王', '统御一方妖族之妖王，气息如渊，人族神尊亦需忌惮三分。', '人族神尊？不过尔尔。', '#ef4454', NULL, 0, 1, 0, 0, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (14, '秘境', '不周遗墟', '遗迹', '上古不周山倾塌后遗留的墟境，藏有开天传承与上古兵器，探秘者众。', '开天之力，尽藏于此墟之中。', '#5eead4', NULL, 0, 1, 0, 0, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (15, '秘境', '元初山', '圣地', '人族神尊修炼圣地，灵气如海，禁制重重，乃问道之至高所在。', '问道元初，方知神尊之浩瀚。', '#5eead4', NULL, 0, 1, 0, 0, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (16, '秘境', '暗星界', '秘界', '游离于元界之外的暗星之界，妖族据守，星光尽灭，凶险万分。', '星光尽灭处，妖族称王。', '#5eead4', NULL, 0, 1, 0, 0, '2026-11-20 15:37:49');
INSERT INTO `cy_codex` VALUES (17, '秘境', '归海秘境', '水境', '柳七月一脉守护的归海秘境，水脉本源孕养万物，藏有水神传承。', '海纳百川，孕养一方生灵。', '#5eead4', NULL, 0, 1, 0, 0, '2026-11-20 15:37:49');

-- ----------------------------
-- Table structure for cy_forum_like
-- ----------------------------
DROP TABLE IF EXISTS `cy_forum_like`;
CREATE TABLE `cy_forum_like`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `post_id` bigint UNSIGNED NOT NULL COMMENT '帖子 ID',
  `user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '点赞账号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_user`(`post_id` ASC, `user_name` ASC) USING BTREE,
  INDEX `idx_user`(`user_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元界·论道论坛共鸣记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cy_forum_like
-- ----------------------------

-- ----------------------------
-- Table structure for cy_forum_post
-- ----------------------------
DROP TABLE IF EXISTS `cy_forum_post`;
CREATE TABLE `cy_forum_post`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '帖子 ID',
  `category` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '论道' COMMENT '板块：论道/切磋/妖情/闲谈',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '论题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '正文',
  `author_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发帖账号（avatar_foundation.deity_name）',
  `author_alias` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '展示名号（神尊封号 myth_name）',
  `view_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '阅卷数',
  `like_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '共鸣数（点赞）',
  `reply_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '回帖数',
  `is_pinned` tinyint(1) NOT NULL DEFAULT 0 COMMENT '置顶：1=置顶 0=普通',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=已隐藏（作者删除/后台屏蔽）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发帖时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_reply_at` datetime NULL DEFAULT NULL COMMENT '最后回帖时间（用于最新排序）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_status_pinned`(`status` ASC, `is_pinned` ASC, `last_reply_at` ASC) USING BTREE,
  INDEX `idx_author`(`author_name` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元界·论道论坛帖子' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cy_forum_post
-- ----------------------------
INSERT INTO `cy_forum_post` VALUES (1, '论道', '【坛规】元初山论道 · 入坛须知', '诸位道友：论道坛乃元界同道论法之所。一不论人身攻击，二不泄根基口令，三不造妖言惑众。凡有道法疑难、秘境见闻、刀意心得，皆可开帖共议。愿诸君以道会友，共守人族山河。', '沧元祖师', '沧元祖师', 128, 36, 2, 1, 1, '2026-11-20 15:37:49', '2026-11-20 15:37:49', '2026-11-20 15:37:49');
INSERT INTO `cy_forum_post` VALUES (2, '切磋', '水墨刀意第七式，如何破「无相」之境？', '近日于元初山演武台与同辈切磋，刀意至第七式便被「无相」化去，锋芒尽散。窃以为当以元神凝意为锋，不以形取势，然屡试未成。敢问诸位道友，可有破局之法？', '孟川', '水墨刀君', 96, 24, 1, 0, 1, '2026-11-20 15:37:49', '2026-11-20 15:37:49', '2026-11-20 15:37:49');
INSERT INTO `cy_forum_post` VALUES (3, '妖情', '东海之滨妖气骤盛 · 巡守见闻', '昨夜三更，东宁府外海雾泛赤，巡守见赤焰妖将踪迹。妖潮或提前至，各府道友宜早备丹药与符箓，勿单独夜行。若有最新妖情，烦请续于此帖之下。', '柳七月', '凤凰血脉', 73, 18, 0, 0, 1, '2026-11-20 15:37:49', '2026-11-20 15:37:49', NULL);

-- ----------------------------
-- Table structure for cy_forum_reply
-- ----------------------------
DROP TABLE IF EXISTS `cy_forum_reply`;
CREATE TABLE `cy_forum_reply`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '回复 ID',
  `post_id` bigint UNSIGNED NOT NULL COMMENT '所属帖子 ID',
  `author_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回帖账号',
  `author_alias` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '展示名号',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回帖正文',
  `floor_no` int UNSIGNED NOT NULL DEFAULT 1 COMMENT '楼层（从 2 楼起，1 楼为主帖）',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=已隐藏',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回帖时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_floor`(`post_id` ASC, `floor_no` ASC) USING BTREE,
  INDEX `idx_author`(`author_name` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元界·论道论坛回帖' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cy_forum_reply
-- ----------------------------
INSERT INTO `cy_forum_reply` VALUES (1, 1, '秦五', '元初山尊者', '规矩简明，照此论道即可。后来者可先阅此帖，再开新题。', 2, 1, '2026-11-20 15:37:49');
INSERT INTO `cy_forum_reply` VALUES (2, 1, '晏烬', '剑锋天骄', '已阅。愿与诸君共守元界清朗。', 3, 1, '2026-11-20 15:37:49');
INSERT INTO `cy_forum_reply` VALUES (3, 2, '秦五', '元初山尊者', '「无相」化形不化意。刀在意先，意断则势自散。不妨先收刀三日，再以一笔落尽山河。', 2, 1, '2026-11-20 15:37:49');

-- ----------------------------
-- Table structure for cy_home_news
-- ----------------------------
DROP TABLE IF EXISTS `cy_home_news`;
CREATE TABLE `cy_home_news`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tag` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '公告' COMMENT '标签：公告/预警/活动/历练',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动态文案',
  `keyword` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '点击后跳转检索的关键词',
  `color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#e9c46a' COMMENT '文字主色',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序（越小越前）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1=展示 0=隐藏',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tag_content`(`tag` ASC, `content` ASC) USING BTREE,
  INDEX `idx_status_sort`(`status` ASC, `sort_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元图·首页元界动态' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cy_home_news
-- ----------------------------
INSERT INTO `cy_home_news` VALUES (1, '公告', '神尊归位大典 · 新晋神尊觉醒仪式将启', '神尊', '#e9c46a', 10, 1, '2026-11-20 15:37:49', '2026-11-20 15:37:49');
INSERT INTO `cy_home_news` VALUES (2, '预警', '妖潮预警 · 东海之滨妖气骤盛', '噬魂魔', '#ef4454', 20, 1, '2026-11-20 15:37:49', '2026-11-20 15:37:49');
INSERT INTO `cy_home_news` VALUES (3, '活动', '不周遗墟开放 · 探秘可得上古传承', '不周遗墟', '#5eead4', 30, 1, '2026-11-20 15:37:49', '2026-11-20 15:37:49');
INSERT INTO `cy_home_news` VALUES (4, '历练', '元初山论道 · 同辈天骄切磋在即', '元初山', '#2dd4bf', 40, 1, '2026-11-20 15:37:49', '2026-11-20 15:37:49');

-- ----------------------------
-- Table structure for cy_home_section
-- ----------------------------
DROP TABLE IF EXISTS `cy_home_section`;
CREATE TABLE `cy_home_section`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `section_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '界域类型：神尊/功法/妖魔/秘境（与图鉴 category 对应）',
  `title` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '展示框标题，如「神尊殿」',
  `icon` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '❖' COMMENT '展示框图标',
  `color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#e9c46a' COMMENT '展示框主色',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '展示框简介',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序（越小越前）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1=展示 0=隐藏',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_section_type`(`section_type` ASC) USING BTREE,
  INDEX `idx_status_sort`(`status` ASC, `sort_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元图·首页核心界域展示框' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cy_home_section
-- ----------------------------
INSERT INTO `cy_home_section` VALUES (1, '神尊', '神尊殿', '✦', '#e9c46a', '人族神尊列传，孟川、柳七月、秦五……元界守护者的名号与传说。', 10, 1, '2026-11-20 15:37:49', '2026-11-20 15:37:49');
INSERT INTO `cy_home_section` VALUES (2, '功法', '修炼功法', '✧', '#2dd4bf', '元神淬体、刀道雷法，淬肉身、凝神魂的修行法门总纲。', 20, 1, '2026-11-20 15:37:49', '2026-11-20 15:37:49');
INSERT INTO `cy_home_section` VALUES (3, '妖魔', '妖魔图鉴', '☠', '#ef4454', '妖族谱录：噬魂魔、赤焰妖将、妖王……识其形，方能斩其势。', 30, 1, '2026-11-20 15:37:49', '2026-11-20 15:37:49');
INSERT INTO `cy_home_section` VALUES (4, '秘境', '元界秘境', '❖', '#5eead4', '不周遗墟、元初山、暗星界，藏上古传承的凶险界域。', 40, 1, '2026-11-20 15:37:49', '2026-11-20 15:37:49');

-- ----------------------------
-- Table structure for cy_site_setting
-- ----------------------------
DROP TABLE IF EXISTS `cy_site_setting`;
CREATE TABLE `cy_site_setting`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `setting_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `setting_value` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '配置值',
  `remark` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '说明',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_setting_key`(`setting_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元图·站点文案配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cy_site_setting
-- ----------------------------
INSERT INTO `cy_site_setting` VALUES (1, 'site_title', '沧元图', '品牌名（导航栏印章旁）', '2026-11-20 15:37:49');
INSERT INTO `cy_site_setting` VALUES (2, 'site_subtitle', 'CANG YUAN TU · 元界', '品牌副标题', '2026-11-20 15:37:49');
INSERT INTO `cy_site_setting` VALUES (3, 'hero_badge', '元界 · 神尊归位', '首屏徽标', '2026-11-20 15:37:49');
INSERT INTO `cy_site_setting` VALUES (4, 'hero_title', '沧元界 · 检索万物', '首屏主标题', '2026-11-20 15:37:49');
INSERT INTO `cy_site_setting` VALUES (5, 'hero_subtitle', '刀斩妖邪，笔绘众生。于元界之中，寻你的神尊、功法与秘境。', '首屏副标题', '2026-11-20 15:37:49');
INSERT INTO `cy_site_setting` VALUES (6, 'search_placeholder', '输入神尊名号、功法、妖魔或秘境…', '首屏检索占位文案', '2026-11-20 15:37:49');
INSERT INTO `cy_site_setting` VALUES (7, 'hot_tags', '孟川,柳七月,元神淬体诀,水墨刀意,噬魂魔,不周遗墟', '热门检索标签（英文逗号分隔）', '2026-11-20 15:37:49');
INSERT INTO `cy_site_setting` VALUES (8, 'footer_text', '© 沧元图主题演示 · 仅作展示用途，非官方站点', '页脚文案', '2026-11-20 15:37:49');

-- ----------------------------
-- Table structure for spar_log
-- ----------------------------
DROP TABLE IF EXISTS `spar_log`;
CREATE TABLE `spar_log`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `initiator_id` int UNSIGNED NOT NULL COMMENT '发起者 user_id',
  `target_id` int UNSIGNED NOT NULL COMMENT '应战者 user_id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '切磋时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_initiator`(`initiator_id` ASC) USING BTREE,
  INDEX `idx_target`(`target_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元图·切磋记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of spar_log
-- ----------------------------

-- ----------------------------
-- Table structure for user_card_view
-- ----------------------------
DROP TABLE IF EXISTS `user_card_view`;
CREATE TABLE `user_card_view`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int UNSIGNED NOT NULL COMMENT 'avatar_foundation.id',
  `card_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '卡片唯一标识',
  `card_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '卡片名号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次浏览时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_card`(`user_id` ASC, `card_key` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元图·首页卡片首阅记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_card_view
-- ----------------------------
INSERT INTO `user_card_view` VALUES (8, 4, 'codex_3', '秦五', '2026-11-20 16:21:51');
INSERT INTO `user_card_view` VALUES (9, 4, 'codex_4', '晏烬', '2026-11-20 16:21:54');
INSERT INTO `user_card_view` VALUES (10, 4, 'codex_1', '孟川', '2026-11-20 16:22:19');
INSERT INTO `user_card_view` VALUES (11, 4, 'codex_13', '黑铁妖王', '2026-11-20 16:22:23');
INSERT INTO `user_card_view` VALUES (12, 4, 'codex_15', '元初山', '2026-11-20 16:22:25');
INSERT INTO `user_card_view` VALUES (13, 4, 'codex_2', '柳七月', '2026-11-20 16:22:31');
INSERT INTO `user_card_view` VALUES (14, 4, 'codex_5', '元神淬体诀', '2026-11-20 16:22:33');
INSERT INTO `user_card_view` VALUES (15, 4, 'codex_6', '水墨刀意', '2026-11-20 16:22:35');
INSERT INTO `user_card_view` VALUES (16, 4, 'codex_7', '九霄雷法', '2026-11-20 16:22:36');
INSERT INTO `user_card_view` VALUES (17, 4, 'codex_8', '不灭身', '2026-11-20 16:22:38');
INSERT INTO `user_card_view` VALUES (18, 4, 'codex_9', '心意刀', '2026-11-20 16:22:40');
INSERT INTO `user_card_view` VALUES (19, 4, 'codex_10', '噬魂魔', '2026-11-20 16:22:43');
INSERT INTO `user_card_view` VALUES (20, 4, 'codex_11', '赤焰妖将', '2026-11-20 16:22:44');
INSERT INTO `user_card_view` VALUES (21, 4, 'codex_12', '蚀骨蛛皇', '2026-11-20 16:22:46');
INSERT INTO `user_card_view` VALUES (22, 4, 'codex_14', '不周遗墟', '2026-11-20 16:22:48');
INSERT INTO `user_card_view` VALUES (23, 4, 'codex_16', '暗星界', '2026-11-20 16:22:50');
INSERT INTO `user_card_view` VALUES (24, 4, 'codex_17', '归海秘境', '2026-11-20 16:22:52');

-- ----------------------------
-- Table structure for user_cultivation
-- ----------------------------
DROP TABLE IF EXISTS `user_cultivation`;
CREATE TABLE `user_cultivation`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int UNSIGNED NOT NULL COMMENT 'avatar_foundation.id',
  `realm` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '筑基' COMMENT '当前境界（筑基起）',
  `power` int NOT NULL DEFAULT 0 COMMENT '神尊力（修炼经验）',
  `copper` int NOT NULL DEFAULT 200 COMMENT '铜币',
  `title` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '称号：封侯神尊/封王神尊',
  `secret_date` date NULL DEFAULT NULL COMMENT '秘境修炼计数日期（每日重置）',
  `secret_count` int NOT NULL DEFAULT 0 COMMENT '当日已入秘境次数（上限 20）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开辟时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近修炼时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元图·修炼状态' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_cultivation
-- ----------------------------
INSERT INTO `user_cultivation` VALUES (1, 4, '造化尊者', 10, 850, '封王神尊', '2026-11-20', 1, '2026-11-20 15:39:01', '2026-11-20 16:36:34');
INSERT INTO `user_cultivation` VALUES (3, 6, '洗髓', 0, 0, NULL, '2026-11-20', 4, '2026-11-20 16:07:21', '2026-11-20 16:36:34');
INSERT INTO `user_cultivation` VALUES (5, 12, '筑基', 0, 200, NULL, NULL, 0, '2026-11-20 16:10:11', '2026-11-20 16:36:34');
INSERT INTO `user_cultivation` VALUES (9, 1, '筑基', 0, 0, NULL, NULL, 0, '2026-11-20 16:36:00', '2026-11-20 16:36:34');
INSERT INTO `user_cultivation` VALUES (10, 2, '筑基', 0, 0, NULL, NULL, 0, '2026-11-20 16:36:00', '2026-11-20 16:36:34');
INSERT INTO `user_cultivation` VALUES (11, 3, '筑基', 0, 0, NULL, NULL, 0, '2026-11-20 16:36:00', '2026-11-20 16:36:34');

-- ----------------------------
-- Table structure for user_gongfa
-- ----------------------------
DROP TABLE IF EXISTS `user_gongfa`;
CREATE TABLE `user_gongfa`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int UNSIGNED NOT NULL COMMENT 'avatar_foundation.id',
  `gongfa_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '功法名（cy_codex.name）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '购得时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_gongfa`(`user_id` ASC, `gongfa_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元图·已购功法' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_gongfa
-- ----------------------------
INSERT INTO `user_gongfa` VALUES (1, 4, '心意刀', '2026-11-20 15:49:02');

-- ----------------------------
-- Table structure for user_register_record
-- ----------------------------
DROP TABLE IF EXISTS `user_register_record`;
CREATE TABLE `user_register_record`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键（一次注册一条）',
  `avatar_id` int UNSIGNED NULL DEFAULT NULL COMMENT '关联的 avatar_foundation.id，用户被后台删除后此值仅作历史留痕',
  `account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '注册账号（deity_name）',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '注册密码（spell 召唤语，按现有约定明文存储）',
  `region` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '注册时填写的地域',
  `myth_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '觉醒神尊名号（展示用）',
  `myth_story` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '神话原型简介',
  `agreed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否立下心魔誓约',
  `permission` tinyint(1) NOT NULL DEFAULT 1 COMMENT '注册时权限快照：1=默认用户，0=管理员',
  `register_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '注册来源 IP',
  `source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'forge' COMMENT '来源：forge=前端注册 / migrate=历史数据回填',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account`(`account` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  INDEX `idx_avatar_id`(`avatar_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '沧元界·每次注册的账号留痕' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_register_record
-- ----------------------------
INSERT INTO `user_register_record` VALUES (1, 1, '九天应元雷声普化天尊', '诸般雷霆，遵吾号令', '东宁府', '孟川', '沧元图主角孟川觉醒之神尊相，雷霆加护，万物臣服。', 1, 1, NULL, 'migrate', '2026-11-20 15:37:49');
INSERT INTO `user_register_record` VALUES (2, 2, '烛龙真君', '烛龙衔烛，昼夜由心', '幽都山', '烛龙', '衔烛而照幽都，睁眼为昼，闭眼为夜。', 1, 1, NULL, 'migrate', '2026-11-20 15:37:49');
INSERT INTO `user_register_record` VALUES (3, 3, '西王母元君', '西王母临，瑶池生辉', '昆仑山', '西王母', '昆仑之主，瑶池生辉，掌不死之药。', 1, 1, NULL, 'migrate', '2026-11-20 15:37:49');
INSERT INTO `user_register_record` VALUES (4, 4, '沧元祖师', 'admin123', '天枢殿', '元界天帝', '元界天帝：执掌天枢，统御元界后台。', 1, 0, NULL, 'migrate', '2026-11-20 15:37:49');
INSERT INTO `user_register_record` VALUES (8, 5, 'PersistenceTestTmp001', 'tmp123456', '测试府', '验证散修', '端到端验证用', 1, 1, '127.0.0.1', 'forge', '2026-11-20 15:45:20');
INSERT INTO `user_register_record` VALUES (9, 6, '111', '111111', '测试府', '女娲道尊·4657', '女娲：抟土造人，炼石补天，庇佑苍生。', 1, 1, '127.0.0.1', 'forge', '2026-11-20 16:06:14');
INSERT INTO `user_register_record` VALUES (10, 7, 'CardViewTestTmp001', 'tmp123456', '测试府', '验证散修', NULL, 1, 1, '127.0.0.1', 'forge', '2026-11-20 16:08:14');
INSERT INTO `user_register_record` VALUES (11, 12, '12345', '123456', '测试府', '神农道尊·2755', '神农：尝百草、播五谷，惠泽万民。', 1, 1, '127.0.0.1', 'forge', '2026-11-20 16:09:03');
INSERT INTO `user_register_record` VALUES (12, 13, 'ViewedCardTestTmp', 'tmp123456', '测试府', '验证散修', NULL, 1, 1, '127.0.0.1', 'forge', '2026-11-20 16:10:55');
INSERT INTO `user_register_record` VALUES (13, 14, 'CascadeTestTmp', 'tmp123456', '测试府', '验证散修', NULL, 1, 1, '127.0.0.1', 'forge', '2026-11-20 16:12:45');
INSERT INTO `user_register_record` VALUES (14, 15, 'CascadeTestTmp2', 'tmp123456', '测试府', '验证散修', NULL, 1, 1, '127.0.0.1', 'forge', '2026-11-20 16:12:52');
INSERT INTO `user_register_record` VALUES (15, 16, 'BoardCascadeTmp', 'tmp123456', '测试府', '验证散修', NULL, 1, 1, '127.0.0.1', 'forge', '2026-11-20 16:14:42');

SET FOREIGN_KEY_CHECKS = 1;
