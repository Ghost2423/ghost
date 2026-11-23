package com.fenglin.springboottest.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 沧元图 · 太虚弈局规则（唯一事实源）—— 飞行棋规则版
 *
 * 玩法：花铜币报名 -> 每方 4 架飞机停于机坪 -> 掷骰 6 点起飞至起点 ->
 *       同色格跳跃飞行 -> 撞子送回机坪 -> 精确点数进入终点 ->
 *       4 架飞机全部登临神位即通关，领取通关大奖。
 *
 * 飞行棋核心：
 *   - 停机坪 HANGAR(-1)，掷出 6 点方可起飞到起点 0，并可再掷一次（PVP 中不换手）；
 *   - 格子颜色 = index % 4，飞机颜色 = 自身编号；落同色格额外飞跃 4 格（不越过终点）；
 *   - 落点与敌方飞机重合 -> 敌机送回机坪（PVP）；单人局棋盘「劫妖」守军被撞破可得赏赐；
 *   - 点数超过终点则原路反弹，必须精确落在终点才算登临；
 *   - 4 架飞机全部登临即胜。掷骰次数用尽仍未全部登顶则本局结束。
 *
 * 收益：落地格 / 撞妖 / 通关的铜币与神尊力直接写入 user_cultivation（真实入库）。
 *
 * 防刷：需报名费 30 铜币；每日最多 10 局；每局最多 60 次掷骰。
 * PVP 擂台：报名费 50 铜币/方，奖池归胜者；4 架飞机先全部登顶者胜。
 * 道具：leap 缩地符（前进3格）、coin 聚宝符（+60铜币）、rain 甘霖符（+25神尊力）、
 *       ward 镇厄符（免疫下一次劫难）。落道具格随机得一件。
 */
public final class BoardGameRules {

    private BoardGameRules() {}

    /** 报名费（铜币） */
    public static final int ENTRY_FEE_COPPER = 30;
    /** 每日最多对局数 */
    public static final int DAILY_LIMIT = 10;
    /** 每局最多掷骰次数（4 架飞机需更多回合） */
    public static final int MAX_ROLLS = 60;
    /** 棋盘格数（0..BOARD_SIZE-1，最后一格为终点） */
    public static final int BOARD_SIZE = 16;

    /* ===== PVP 擂台规则 ===== */
    /** PVP 报名费（铜币，双方各出） */
    public static final int PVP_ENTRY_FEE = 50;
    /** PVP 每方最多掷骰次数 */
    public static final int PVP_MAX_ROLLS = 80;
    /** PVP 每日最多场次（按发起方计） */
    public static final int PVP_DAILY_LIMIT = 5;
    /** PVP 获胜额外奖励 */
    public static final int PVP_WIN_COPPER = 100, PVP_WIN_POWER = 50;

    /* ===== 飞行棋机制 ===== */
    /** 每方飞机数量 */
    public static final int PLANES = 4;
    /** 停机坪位置（未起飞） */
    public static final int HANGAR = -1;
    /** 终点格位置（棋盘最后一格） */
    public static final int FINISH_POS = BOARD_SIZE - 1;
    /** 起飞所需点数 */
    public static final int TAKEOFF_ROLL = 6;
    /** 同色格飞跃步数 */
    public static final int SAME_COLOR_JUMP = 4;
    /** 单人局「劫妖」守军所在格（落在这些格撞破守军得赏赐） */
    public static final int[] GUARD_CELLS = {5, 9};
    /** 撞破劫妖赏赐铜币 */
    public static final int GUARD_BOUNTY_COPPER = 25;

    /** 飞机外观（编号即颜色） */
    private static final List<Map<String, Object>> PLANE_META = buildPlaneMeta();
    private static List<Map<String, Object>> buildPlaneMeta() {
        String[][] defs = {
                {"0", "青鸾", "✦", "#2dd4bf", "青"},
                {"1", "金乌", "◈", "#e9c46a", "金"},
                {"2", "赤凰", "❖", "#ef4454", "赤"},
                {"3", "白鹤", "◇", "#e8eef7", "白"}
        };
        List<Map<String, Object>> l = new ArrayList<>();
        for (String[] d : defs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", Integer.parseInt(d[0]));
            m.put("name", d[1]); m.put("icon", d[2]);
            m.put("color", d[3]); m.put("color_name", d[4]);
            l.add(m);
        }
        return List.copyOf(l);
    }
    public static List<Map<String, Object>> planeMetaView() { return PLANE_META; }

    /** 飞机颜色 = 自身编号（0..3） */
    public static int planeColor(int planeIndex) { return planeIndex % 4; }
    /** 格子颜色 = 格号 % 4 */
    public static int cellColor(int index) { return index % 4; }
    /** 同色飞跃目标格；不越过终点，非同色或已在终点则返回原格 */
    public static int jumpTarget(int pos, int planeIndex) {
        if (pos < 0 || pos >= FINISH_POS) return pos;
        if (cellColor(pos) != planeColor(planeIndex)) return pos;
        int to = pos + SAME_COLOR_JUMP;
        return to > FINISH_POS ? FINISH_POS : to;
    }
    /** 行棋落点：超过终点则原路反弹 */
    public static int movedTo(int from, int dice) {
        int raw = from + dice;
        if (raw <= FINISH_POS) return raw;
        return Math.max(0, FINISH_POS - (raw - FINISH_POS));
    }
    /** 解析飞机位置串（如 "-1,-1,3,15"）为 4 个位置 */
    public static int[] parsePlanes(String s) {
        int[] a = new int[PLANES];
        for (int i = 0; i < PLANES; i++) a[i] = HANGAR;
        if (s == null || s.isBlank()) return a;
        String[] parts = s.split(",");
        for (int i = 0; i < Math.min(parts.length, PLANES); i++) {
            try { a[i] = Integer.parseInt(parts[i].trim()); } catch (NumberFormatException ignored) { }
        }
        return a;
    }
    /** 4 个位置序列化为逗号串 */
    public static String formatPlanes(int[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PLANES; i++) {
            if (i > 0) sb.append(',');
            sb.append(a[i]);
        }
        return sb.toString();
    }
    /** 是否全部飞机登临终点 */
    public static boolean allFinished(int[] a) {
        for (int p : a) if (p != FINISH_POS) return false;
        return true;
    }
    /** 劫妖守军初始状态串（1=存活），长度与 GUARD_CELLS 一致 */
    public static String initialGuards() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < GUARD_CELLS.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('1');
        }
        return sb.toString();
    }

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

    /** 前端展示用：完整棋盘（含格子颜色） */
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
            m.put("color", cellColor(c.index));
            list.add(m);
        }
        return list;
    }
}
