package com.fenglin.springboottest.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 沧元图 · 太虚弈局（桌游）规则（唯一事实源）
 *
 * 玩法：掷骰走格子。花铜币报名 -> 逐次掷骰(1~6)前进 -> 落到不同格子结算收益 ->
 *       抵达/越过终点「登临神位」即通关，领取通关大奖；掷骰次数用尽仍未到终点则本局结束。
 * 产出：铜币与神尊力会在落格 / 通关时直接写入 user_cultivation（真实入库）。
 *
 * 格子类型：
 *   START  起点（太极门，无结算）
 *   COPPER 铜币格（得铜币）
 *   POWER  神尊力格（得神尊力）
 *   BOON   秘境格（铜币 + 神尊力双得）
 *   FORTUNE 福缘格（随机：铜币或神尊力）
 *   PERIL  劫难格（损失铜币或神尊力，但不低于 0）
 *   ITEM   道具格（随机获得一件道具，存入背包）
 *   FINISH 终点（登临神位，通关大奖）
 *
 * 防刷：需报名费 30 铜币；每日最多 10 局；每局最多 20 次掷骰。
 *
 * PVP 擂台：报名费 50 铜币/方，奖池归胜者；先到终点者胜，途中落格收益各自入库。
 * 道具：leap 缩地符（前进3格）、coin 聚宝符（+60铜币）、rain 甘霖符（+25神尊力）、
 *       ward 镇厄符（免疫下一次劫难）。落道具格随机得一件。
 */
public final class BoardGameRules {

    private BoardGameRules() {}

    /** 报名费（铜币） */
    public static final int ENTRY_FEE_COPPER = 30;
    /** 每日最多对局数 */
    public static final int DAILY_LIMIT = 10;
    /** 每局最多掷骰次数 */
    public static final int MAX_ROLLS = 20;
    /** 棋盘格数（0..BOARD_SIZE-1，最后一格为终点） */
    public static final int BOARD_SIZE = 16;

    /* ===== PVP 擂台规则 ===== */
    /** PVP 报名费（铜币，双方各出） */
    public static final int PVP_ENTRY_FEE = 50;
    /** PVP 每方最多掷骰次数 */
    public static final int PVP_MAX_ROLLS = 25;
    /** PVP 每日最多场次（按发起方计） */
    public static final int PVP_DAILY_LIMIT = 5;
    /** PVP 获胜额外奖励 */
    public static final int PVP_WIN_COPPER = 100, PVP_WIN_POWER = 50;

    /* ===== 格子类型 ===== */
    public static final String T_START = "START";
    public static final String T_COPPER = "COPPER";
    public static final String T_POWER = "POWER";
    public static final String T_BOON = "BOON";
    public static final String T_FORTUNE = "FORTUNE";
    public static final String T_PERIL = "PERIL";
    public static final String T_ITEM = "ITEM";
    public static final String T_FINISH = "FINISH";

    /* ===== 道具 ===== */
    public static final String ITEM_LEAP = "leap";   // 缩地符：前进 3 格
    public static final String ITEM_COIN = "coin";   // 聚宝符：+60 铜币
    public static final String ITEM_RAIN = "rain";   // 甘霖符：+25 神尊力
    public static final String ITEM_WARD = "ward";   // 镇厄符：免疫下一次劫难
    /** 道具表（key -> 名称/图标/说明），顺序即随机掉落权重（重复项提高概率） */
    public static final List<Map<String, Object>> ITEMS = buildItems();
    private static List<Map<String, Object>> buildItems() {
        List<Map<String, Object>> l = new ArrayList<>();
        l.add(item(ITEM_LEAP, "缩地符", "⇥", "立即前进 3 格"));
        l.add(item(ITEM_COIN, "聚宝符", "◉", "立即获得 60 铜币"));
        l.add(item(ITEM_RAIN, "甘霖符", "⚡", "立即获得 25 神尊力"));
        l.add(item(ITEM_WARD, "镇厄符", "🛡", "免疫下一次劫难损失"));
        l.add(item(ITEM_COIN, "聚宝符", "◉", "立即获得 60 铜币"));  // 提高铜币符权重
        return List.copyOf(l);
    }
    private static Map<String, Object> item(String key, String name, String icon, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key); m.put("name", name); m.put("icon", icon); m.put("desc", desc);
        return m;
    }
    /** 随机掉落一件道具 */
    public static Map<String, Object> randomItem() {
        return ITEMS.get(ThreadLocalRandom.current().nextInt(ITEMS.size()));
    }
    /** 道具数值规则 */
    public static final int ITEM_COIN_GAIN = 60, ITEM_RAIN_GAIN = 25, ITEM_LEAP_STEPS = 3;

    /** 福缘格随机范围（含端点） */
    public static final int FORTUNE_COPPER_MIN = 20, FORTUNE_COPPER_MAX = 60;
    public static final int FORTUNE_POWER_MIN = 10, FORTUNE_POWER_MAX = 30;
    /** 劫难格随机范围（含端点） */
    public static final int PERIL_COPPER_MIN = 10, PERIL_COPPER_MAX = 25;
    public static final int PERIL_POWER_MIN = 5, PERIL_POWER_MAX = 12;

    /** 棋盘：索引即格号。name/icon 供前端展示，copper/power 为该格基础收益。 */
    public static final List<Cell> BOARD = buildBoard();

    private static List<Cell> buildBoard() {
        List<Cell> b = new ArrayList<>();
        b.add(new Cell(0,  T_START,   "太极门",   "☯", "太虚弈局始于此，掷骰踏入轮回",        0,   0));
        b.add(new Cell(1,  T_COPPER,  "灵铜铺",   "◉", "拾得灵铜",                           20,  0));
        b.add(new Cell(2,  T_POWER,   "淬神台",   "⚡", "神魂受淬，神尊力涨",                 0,  10));
        b.add(new Cell(3,  T_FORTUNE, "福缘井",   "✦", "机缘所致，或铜或力",                 0,   0));
        b.add(new Cell(4,  T_COPPER,  "聚宝窟",   "◉", "洞天聚宝，铜币盈囊",                 30,  0));
        b.add(new Cell(5,  T_PERIL,   "噬魂渊",   "☠", "妖气侵体，神尊力受损",               0,   0));
        b.add(new Cell(6,  T_POWER,   "问心石",   "⚡", "叩问本心，神尊力涨",                 0,  15));
        b.add(new Cell(7,  T_BOON,    "太虚秘境", "❖", "秘境机缘，铜币与神尊力双得",         40, 20));
        b.add(new Cell(8,  T_COPPER,  "万宝阁",   "◉", "万宝流通，铜币盈囊",                 25,  0));
        b.add(new Cell(9,  T_PERIL,   "劫火道",   "☠", "劫火焚身，铜币受损",                 0,   0));
        b.add(new Cell(10, T_POWER,   "观星台",   "⚡", "观星悟道，神尊力涨",                 0,  20));
        b.add(new Cell(11, T_ITEM,    "奇珍阁",   "🎁", "机缘得宝，随机获得一件道具",         0,   0));
        b.add(new Cell(12, T_COPPER,  "金鳞池",   "◉", "金鳞跃池，铜币大得",                 50,  0));
        b.add(new Cell(13, T_ITEM,    "元初宝匣", "🧧", "元初遗珍，随机获得一件道具",         0,   0));
        b.add(new Cell(14, T_POWER,   "化神崖",   "⚡", "崖前顿悟，神尊力大涨",               0,  25));
        b.add(new Cell(15, T_FINISH,  "登临神位", "♛", "登临神位，通关领取大奖",           150, 100));
        return List.copyOf(b);
    }

    /** 单格定义 */
    public static final class Cell {
        public final int index;
        public final String type;
        public final String name;
        public final String icon;
        public final String desc;
        public final int copper;
        public final int power;

        Cell(int index, String type, String name, String icon, String desc, int copper, int power) {
            this.index = index;
            this.type = type;
            this.name = name;
            this.icon = icon;
            this.desc = desc;
            this.copper = copper;
            this.power = power;
        }
    }

    public static Cell cellAt(int index) {
        if (index < 0 || index >= BOARD.size()) {
            return BOARD.get(BOARD.size() - 1);
        }
        return BOARD.get(index);
    }

    /** 前端展示用：完整棋盘 */
    public static List<Map<String, Object>> boardView() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Cell c : BOARD) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", c.index);
            m.put("type", c.type);
            m.put("name", c.name);
            m.put("icon", c.icon);
            m.put("desc", c.desc);
            m.put("copper", c.copper);
            m.put("power", c.power);
            list.add(m);
        }
        return list;
    }
}
