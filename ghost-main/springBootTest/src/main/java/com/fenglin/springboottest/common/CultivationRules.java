package com.fenglin.springboottest.common;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 沧元图 · 修炼规则（唯一事实源）
 *
 * 境界阶梯（从低到高）：
 *   凡俗境界：筑基 → 内炼 → 洗髓 → 脱胎 → 无漏
 *   神尊境界：丹云 → 不灭 → 大日 → 暗星 → 无间 → 造化尊者 → 混洞 → 帝君 → 原初
 *             → 一劫 → 二劫 → … → 八劫 → 永恒
 *
 * 突破消耗（按当前境界，即"从该境界突破到下一境界"所需神尊力）：
 *   凡俗境界（筑基..无漏）       100
 *   神尊境界至无间（丹云..暗星） 500
 *   无间 → 造化尊者             1000
 *   造化尊者 → 混洞             1200
 *   混洞 → 帝君                 1500
 *   帝君 → 原初                 2000
 *   原初 → 一劫 2150，一劫 → 二劫 2300，… 每+一劫 +150，
 *     二劫 2450 / 三劫 2600 / 四劫 2750 / 五劫 2900 / 六劫 3050 / 七劫 3200（即七劫 → 八劫 3200）
 *   八劫 → 永恒                 4000
 *   永恒：已至巅峰，不可再突破
 *
 * 称号：突破到「暗星」境获得 封侯神尊；突破到「无间」境获得 封王神尊（更高覆盖）
 *
 * 秘境修炼：每次消耗 50 铜币；凡俗境界一次得 50 神尊力，神尊境界一次得 100 神尊力；每日 20 次
 * 切磋：双方各得 20 神尊力 + 15 铜币
 */
public final class CultivationRules {

    private CultivationRules() {}

    /* ===== 境界阶梯 ===== */
    public static final String STAGE_MORTAL = "凡俗境界";
    public static final String STAGE_DEITY = "神尊境界";

    public static final List<String> MORTAL_REALMS = List.of("筑基", "内炼", "洗髓", "脱胎", "无漏");

    public static final List<String> DEITY_REALMS = List.of(
            "丹云", "不灭", "大日", "暗星", "无间",
            "造化尊者", "混洞", "帝君", "原初",
            "一劫", "二劫", "三劫", "四劫", "五劫", "六劫", "七劫", "八劫",
            "永恒");

    /** 完整阶梯（低 → 高） */
    public static final List<String> LADDER =
            List.copyOf(java.util.stream.Stream.concat(MORTAL_REALMS.stream(), DEITY_REALMS.stream())
                    .toList());

    /** 巅峰境界 */
    public static final String REALM_APEX = "永恒";

    /* ===== 称号 ===== */
    /** 突破到暗星境获得 */
    public static final String TITLE_FENGHOU = "封侯神尊";
    /** 突破到无间境获得（覆盖封侯） */
    public static final String TITLE_FENGWANG = "封王神尊";
    /** 触发称号的境界（到达即授予） */
    public static final Map<String, String> REALM_TITLES = Map.of(
            "暗星", TITLE_FENGHOU,
            "无间", TITLE_FENGWANG);

    /* ===== 秘境修炼 ===== */
    public static final int SECRET_DAILY_LIMIT = 20;
    public static final int SECRET_COST_COPPER = 50;
    public static final int SECRET_GAIN_MORTAL = 50;
    public static final int SECRET_GAIN_DEITY = 100;

    /* ===== 切磋 ===== */
    public static final int SPAR_GAIN_POWER = 20;
    public static final int SPAR_GAIN_COPPER = 15;

    /* ===== 初始资产 ===== */
    public static final int INIT_COPPER = 200;

    /** 各境界突破到下一境界所需神尊力（key=当前境界；永恒不在表中，已巅峰） */
    private static final Map<String, Integer> BREAK_COST = buildBreakCost();

    private static Map<String, Integer> buildBreakCost() {
        Map<String, Integer> m = new HashMap<>();
        // 凡俗境界：100
        for (String r : MORTAL_REALMS) {
            m.put(r, 100);
        }
        // 神尊境界直至无间：500
        for (String r : List.of("丹云", "不灭", "大日", "暗星")) {
            m.put(r, 500);
        }
        // 无间之后逐境提高
        m.put("无间", 1000);
        m.put("造化尊者", 1200);
        m.put("混洞", 1500);
        m.put("帝君", 2000);
        m.put("原初", 2150);
        m.put("一劫", 2300);
        m.put("二劫", 2450);
        m.put("三劫", 2600);
        m.put("四劫", 2750);
        m.put("五劫", 2900);
        m.put("六劫", 3050);
        m.put("七劫", 3200);
        m.put("八劫", 4000);
        return Map.copyOf(m);
    }

    /** 境界所属大阶段 */
    public static String stageOf(String realm) {
        return MORTAL_REALMS.contains(realm) ? STAGE_MORTAL : STAGE_DEITY;
    }

    /** 境界在阶梯中的序号（0 起）；未知境界返回 -1 */
    public static int indexOf(String realm) {
        return LADDER.indexOf(realm);
    }

    /** 下一境界；永恒（巅峰）返回 null */
    public static String nextRealm(String realm) {
        int i = indexOf(realm);
        if (i < 0 || i >= LADDER.size() - 1) {
            return null;
        }
        return LADDER.get(i + 1);
    }

    /** 从当前境界突破所需神尊力；巅峰或未知境界返回 null */
    public static Integer breakCost(String realm) {
        return BREAK_COST.get(realm);
    }

    /** 秘境修炼一次获得的神尊力（按当前境界） */
    public static int secretGain(String realm) {
        return MORTAL_REALMS.contains(realm) ? SECRET_GAIN_MORTAL : SECRET_GAIN_DEITY;
    }

    /** 前端展示用：完整阶梯（含序号/大阶段/突破消耗/触发称号） */
    public static List<Map<String, Object>> ladderView() {
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (int i = 0; i < LADDER.size(); i++) {
            String r = LADDER.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", i);
            m.put("name", r);
            m.put("stage", stageOf(r));
            m.put("cost", breakCost(r)); // 从该境界突破所需；永恒为 null
            m.put("title", REALM_TITLES.get(r)); // 到达该境界触发的称号
            list.add(m);
        }
        return list;
    }
}
