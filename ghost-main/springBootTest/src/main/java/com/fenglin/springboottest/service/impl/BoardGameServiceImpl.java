package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.common.BoardGameRules;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.entity.BoardGameLog;
import com.fenglin.springboottest.entity.BoardItem;
import com.fenglin.springboottest.entity.BoardPvpMatch;
import com.fenglin.springboottest.entity.UserCultivation;
import com.fenglin.springboottest.mapper.AvatarFoundationMapper;
import com.fenglin.springboottest.mapper.BoardGameLogMapper;
import com.fenglin.springboottest.mapper.BoardItemMapper;
import com.fenglin.springboottest.mapper.BoardPvpMatchMapper;
import com.fenglin.springboottest.mapper.UserCultivationMapper;
import com.fenglin.springboottest.service.BoardGameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 太虚弈局（桌游）服务实现。
 * 所有数值规则取自 {@link BoardGameRules}；掷骰由服务端权威随机，客户端仅展示。
 * 落格 / 通关 / 道具收益会真实写入 user_cultivation.copper / power。
 */
@Service
public class BoardGameServiceImpl implements BoardGameService {

    @Autowired private BoardGameLogMapper gameMapper;
    @Autowired private UserCultivationMapper cultivationMapper;
    @Autowired private BoardItemMapper itemMapper;
    @Autowired private BoardPvpMatchMapper pvpMapper;
    @Autowired private AvatarFoundationMapper avatarMapper;

    /* ================= 内部工具 ================= */

    private UserCultivation getOrInit(Long userId) {
        UserCultivation uc = cultivationMapper.selectOne(
                new QueryWrapper<UserCultivation>().eq("user_id", userId));
        if (uc != null) return uc;
        uc = new UserCultivation();
        uc.setUserId(userId);
        uc.setRealm("筑基");
        uc.setPower(0);
        uc.setCopper(200);
        uc.setSecretCount(0);
        try {
            cultivationMapper.insert(uc);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            uc = cultivationMapper.selectOne(
                    new QueryWrapper<UserCultivation>().eq("user_id", userId));
        }
        return uc;
    }

    private int todayCount(Long userId) {
        return Math.toIntExact(gameMapper.selectCount(
                new QueryWrapper<BoardGameLog>()
                        .eq("user_id", userId)
                        .ge("created_at", LocalDate.now().atStartOfDay())));
    }

    private BoardGameLog playingGame(Long userId) {
        return gameMapper.selectOne(new QueryWrapper<BoardGameLog>()
                .eq("user_id", userId).eq("status", "playing")
                .orderByDesc("id").last("LIMIT 1"));
    }

    private Map<String, Object> gameView(BoardGameLog g) {
        if (g == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("status", g.getStatus());
        m.put("position", g.getPosition());
        // 飞行棋状态：4 架飞机位置 + 劫妖存活 + 当前选中机 + 再掷标记
        int[] planes = BoardGameRules.parsePlanes(g.getPlaneStates());
        List<Integer> planeList = new ArrayList<>();
        for (int p : planes) planeList.add(p);
        m.put("planes", planeList);
        m.put("active_plane", g.getActivePlane() == null ? 0 : g.getActivePlane());
        m.put("extra_roll", g.getExtraRoll() != null && g.getExtraRoll() == 1);
        m.put("guards", parseGuards(g.getGuards()));
        m.put("rolls", g.getRolls());
        m.put("dice_log", g.getDiceLog());
        m.put("gain_copper", g.getGainCopper());
        m.put("gain_power", g.getGainPower());
        m.put("pass", g.getPass());
        m.put("ward", g.getWard() != null && g.getWard() == 1);
        m.put("created_at", g.getCreatedAt() == null ? null : g.getCreatedAt().toString());
        m.put("finished_at", g.getFinishedAt() == null ? null : g.getFinishedAt().toString());
        return m;
    }

    /** 劫妖状态串解析为布尔数组（1=存活） */
    private boolean[] parseGuards(String s) {
        boolean[] g = new boolean[BoardGameRules.GUARD_CELLS.length];
        for (int i = 0; i < g.length; i++) g[i] = true;
        if (s == null || s.isBlank()) return g;
        String[] parts = s.split(",");
        for (int i = 0; i < Math.min(parts.length, g.length); i++) {
            g[i] = !"0".equals(parts[i].trim());
        }
        return g;
    }

    private String formatGuards(boolean[] g) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < g.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(g[i] ? '1' : '0');
        }
        return sb.toString();
    }

    /** 飞行棋一步的结算明细 */
    private static class FlightMove {
        int plane, dice, from, to;
        boolean tookOff;        // 从停机坪起飞
        boolean jumped;         // 同色飞跃
        boolean finished;       // 该机登临终点
        boolean bounce;         // 超出终点反弹
        boolean guardHit;       // 撞破劫妖
        String special;         // 特殊说明
        SettleResult settle;    // 落格收益
    }

    /** 取可操作的飞机编号（优先当前选中，否则第一架未登顶的） */
    private int resolvePlane(int[] planes, Integer requested, int fallback) {
        if (requested != null && requested >= 0 && requested < BoardGameRules.PLANES
                && planes[requested] != BoardGameRules.FINISH_POS) return requested;
        if (fallback >= 0 && fallback < BoardGameRules.PLANES
                && planes[fallback] != BoardGameRules.FINISH_POS) return fallback;
        for (int i = 0; i < planes.length; i++) {
            if (planes[i] != BoardGameRules.FINISH_POS) return i;
        }
        return 0;
    }

    private Map<String, Object> assetView(UserCultivation uc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("copper", uc.getCopper());
        m.put("power", uc.getPower());
        m.put("realm", uc.getRealm());
        return m;
    }

    /* ================= 道具背包工具 ================= */

    private Map<String, Object> itemDef(String key) {
        for (Map<String, Object> it : BoardGameRules.ITEMS) {
            if (key.equals(it.get("key"))) return it;
        }
        return BoardGameRules.ITEMS.get(0);
    }

    private void grantItem(Long userId, String key) {
        Integer uid = userId.intValue();
        BoardItem bi = itemMapper.selectOne(new QueryWrapper<BoardItem>()
                .eq("user_id", uid).eq("item_key", key));
        if (bi == null) {
            bi = new BoardItem();
            bi.setUserId(uid);
            bi.setItemKey(key);
            bi.setCnt(1);
            itemMapper.insert(bi);
        } else {
            bi.setCnt((bi.getCnt() == null ? 0 : bi.getCnt()) + 1);
            itemMapper.updateById(bi);
        }
    }

    private boolean consumeItem(Long userId, String key) {
        Integer uid = userId.intValue();
        BoardItem bi = itemMapper.selectOne(new QueryWrapper<BoardItem>()
                .eq("user_id", uid).eq("item_key", key));
        if (bi == null || bi.getCnt() == null || bi.getCnt() <= 0) return false;
        bi.setCnt(bi.getCnt() - 1);
        itemMapper.updateById(bi);
        return true;
    }

    private int itemCount(Long userId, String key) {
        Integer uid = userId.intValue();
        BoardItem bi = itemMapper.selectOne(new QueryWrapper<BoardItem>()
                .eq("user_id", uid).eq("item_key", key));
        return bi == null || bi.getCnt() == null ? 0 : bi.getCnt();
    }

    /** 道具背包视图 */
    private List<Map<String, Object>> itemBag(Long userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String[] keys = {BoardGameRules.ITEM_LEAP, BoardGameRules.ITEM_COIN,
                         BoardGameRules.ITEM_RAIN, BoardGameRules.ITEM_WARD};
        for (String key : keys) {
            Map<String, Object> def = itemDef(key);
            Map<String, Object> m = new LinkedHashMap<>(def);
            m.put("cnt", itemCount(userId, key));
            list.add(m);
        }
        return list;
    }

    /** 道具图鉴（去重后的定义） */
    private List<Map<String, Object>> itemCatalog() {
        List<Map<String, Object>> list = new ArrayList<>();
        boolean[] seen = {false, false, false, false};
        for (Map<String, Object> it : BoardGameRules.ITEMS) {
            String key = (String) it.get("key");
            int idx = key.equals(BoardGameRules.ITEM_LEAP) ? 0
                    : key.equals(BoardGameRules.ITEM_COIN) ? 1
                    : key.equals(BoardGameRules.ITEM_RAIN) ? 2 : 3;
            if (!seen[idx]) { seen[idx] = true; list.add(it); }
        }
        return list;
    }

    /* ================= 落格结算（单人 & PVP 共用） ================= */

    /** 结算结果 */
    private static class SettleResult {
        int copper, power;          // 实际变动
        boolean wardUsed;           // 护盾是否抵消了劫难
        String itemGained;          // 获得的道具 key（道具格）
        String itemName;
        String itemIcon;
        SettleResult(int c, int p) { this.copper = c; this.power = p; }
    }

    /**
     * 落格结算到指定用户，返回实际变动。
     * ward=true 时劫难格被抵消且不扣资产；道具格随机掉落一件道具（发到背包）。
     */
    private SettleResult settle(BoardGameRules.Cell cell, UserCultivation uc, boolean ward) {
        int dCopper = 0, dPower = 0;
        boolean wardUsed = false;
        String itemKey = null, itemName = null, itemIcon = null;
        int copper = uc.getCopper() == null ? 0 : uc.getCopper();
        int power = uc.getPower() == null ? 0 : uc.getPower();

        switch (cell.type) {
            case BoardGameRules.T_COPPER:
                dCopper = cell.copper; break;
            case BoardGameRules.T_FINISH:
            case BoardGameRules.T_BOON:
                dCopper = cell.copper; dPower = cell.power; break;
            case BoardGameRules.T_POWER:
                dPower = cell.power; break;
            case BoardGameRules.T_FORTUNE:
                if (ThreadLocalRandom.current().nextBoolean()) {
                    dCopper = ThreadLocalRandom.current().nextInt(
                            BoardGameRules.FORTUNE_COPPER_MIN, BoardGameRules.FORTUNE_COPPER_MAX + 1);
                } else {
                    dPower = ThreadLocalRandom.current().nextInt(
                            BoardGameRules.FORTUNE_POWER_MIN, BoardGameRules.FORTUNE_POWER_MAX + 1);
                }
                break;
            case BoardGameRules.T_PERIL:
                if (ward) {
                    wardUsed = true;
                } else if (ThreadLocalRandom.current().nextBoolean()) {
                    dCopper = -ThreadLocalRandom.current().nextInt(
                            BoardGameRules.PERIL_COPPER_MIN, BoardGameRules.PERIL_COPPER_MAX + 1);
                } else {
                    dPower = -ThreadLocalRandom.current().nextInt(
                            BoardGameRules.PERIL_POWER_MIN, BoardGameRules.PERIL_POWER_MAX + 1);
                }
                break;
            case BoardGameRules.T_ITEM:
                Map<String, Object> drop = BoardGameRules.randomItem();
                itemKey = (String) drop.get("key");
                itemName = (String) drop.get("name");
                itemIcon = (String) drop.get("icon");
                grantItem(uc.getUserId(), itemKey);
                break;
            default: break;
        }

        int newCopper = Math.max(0, copper + dCopper);
        int newPower = Math.max(0, power + dPower);
        SettleResult r = new SettleResult(newCopper - copper, newPower - power);
        r.wardUsed = wardUsed;
        r.itemGained = itemKey; r.itemName = itemName; r.itemIcon = itemIcon;
        uc.setCopper(newCopper);
        uc.setPower(newPower);
        return r;
    }

    /* ================= 单人弈局接口 ================= */

    @Override
    public Map<String, Object> status(AvatarFoundation af) {
        UserCultivation uc = getOrInit(af.getId());
        int used = todayCount(af.getId());
        List<Map<String, Object>> recent = new ArrayList<>();
        for (BoardGameLog g : gameMapper.selectList(new QueryWrapper<BoardGameLog>()
                .eq("user_id", af.getId()).eq("status", "finished")
                .orderByDesc("id").last("LIMIT 10"))) recent.add(gameView(g));
        int played = Math.toIntExact(gameMapper.selectCount(
                new QueryWrapper<BoardGameLog>().eq("user_id", af.getId())));
        int won = Math.toIntExact(gameMapper.selectCount(
                new QueryWrapper<BoardGameLog>().eq("user_id", af.getId()).eq("pass", 1)));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", af.getDeityName());
        m.put("board", BoardGameRules.boardView());
        m.put("board_size", BoardGameRules.BOARD_SIZE);
        m.put("entry_fee", BoardGameRules.ENTRY_FEE_COPPER);
        m.put("daily_limit", BoardGameRules.DAILY_LIMIT);
        m.put("daily_used", used);
        m.put("daily_left", Math.max(0, BoardGameRules.DAILY_LIMIT - used));
        m.put("max_rolls", BoardGameRules.MAX_ROLLS);
        // 飞行棋规则展示
        m.put("flight", flightRuleView());
        m.put("asset", assetView(uc));
        m.put("current", gameView(playingGame(af.getId())));
        m.put("history", recent);
        m.put("total_played", played);
        m.put("total_won", won);
        m.put("achievements", buildAchievements(played, won));
        // 道具
        m.put("items", itemBag(af.getId()));
        m.put("item_catalog", itemCatalog());
        // PVP 概要
        m.put("pvp", pvpSummary(af));
        return m;
    }

    /** 飞行棋规则视图（前端展示用） */
    private Map<String, Object> flightRuleView() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("planes", BoardGameRules.PLANES);
        r.put("hangar", BoardGameRules.HANGAR);
        r.put("finish_pos", BoardGameRules.FINISH_POS);
        r.put("takeoff_roll", BoardGameRules.TAKEOFF_ROLL);
        r.put("jump_steps", BoardGameRules.SAME_COLOR_JUMP);
        r.put("guard_bonus", BoardGameRules.GUARD_BOUNTY_COPPER);
        r.put("guard_cells", Arrays.stream(BoardGameRules.GUARD_CELLS).boxed().collect(Collectors.toList()));
        r.put("plane_meta", BoardGameRules.planeMetaView());
        return r;
    }

    private java.util.List<Map<String, Object>> buildAchievements(int played, int won) {
        java.util.List<Map<String, Object>> list = new ArrayList<>();
        addBadge(list, "初入太虚", "完成首局弈局", played >= 1);
        addBadge(list, "登临神位", "首次通关登顶", won >= 1);
        addBadge(list, "三登神位", "累计通关 3 次", won >= 3);
        addBadge(list, "十全十美", "累计通关 10 次", won >= 10);
        addBadge(list, "弈林高手", "累计对局 20 局", played >= 20);
        return list;
    }

    private void addBadge(java.util.List<Map<String, Object>> list, String name, String desc, boolean unlocked) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name); m.put("desc", desc); m.put("unlocked", unlocked);
        list.add(m);
    }

    @Override
    @Transactional
    public Map<String, Object> start(AvatarFoundation af) {
        BoardGameLog existing = playingGame(af.getId());
        if (existing != null) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("resumed", true);
            r.put("game", gameView(existing));
            r.put("asset", assetView(getOrInit(af.getId())));
            return r;
        }
        int used = todayCount(af.getId());
        if (used >= BoardGameRules.DAILY_LIMIT) {
            throw new BoardGameException("今日弈局次数已用尽（每日 "
                    + BoardGameRules.DAILY_LIMIT + " 局），明日再来");
        }
        UserCultivation uc = getOrInit(af.getId());
        if (uc.getCopper() < BoardGameRules.ENTRY_FEE_COPPER) {
            throw new BoardGameException("铜币不足：入局需 " + BoardGameRules.ENTRY_FEE_COPPER
                    + " 铜币，可先切磋或走秘境攒钱");
        }
        uc.setCopper(uc.getCopper() - BoardGameRules.ENTRY_FEE_COPPER);
        cultivationMapper.updateById(uc);

        BoardGameLog g = new BoardGameLog();
        g.setUserId(af.getId()); g.setStatus("playing"); g.setPosition(BoardGameRules.HANGAR); g.setRolls(0);
        // 飞行棋：4 架飞机停机坪待命、劫妖守军就位、默认选中青鸾
        g.setPlaneStates(BoardGameRules.formatPlanes(new int[]{
                BoardGameRules.HANGAR, BoardGameRules.HANGAR,
                BoardGameRules.HANGAR, BoardGameRules.HANGAR}));
        g.setGuards(BoardGameRules.initialGuards());
        g.setActivePlane(0); g.setExtraRoll(0);
        g.setDiceLog(""); g.setGainCopper(0); g.setGainPower(0); g.setPass(0); g.setWard(0);
        g.setCreatedAt(LocalDateTime.now());
        gameMapper.insert(g);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("resumed", false);
        r.put("cost_copper", BoardGameRules.ENTRY_FEE_COPPER);
        r.put("game", gameView(g));
        r.put("asset", assetView(uc));
        return r;
    }

    @Override
    @Transactional
    public Map<String, Object> roll(AvatarFoundation af, Integer planeIndex) {
        BoardGameLog g = playingGame(af.getId());
        if (g == null) throw new BoardGameException("当前无进行中的弈局，请先入局");
        if (g.getRolls() >= BoardGameRules.MAX_ROLLS) throw new BoardGameException("本局掷骰次数已用尽");

        int[] planes = BoardGameRules.parsePlanes(g.getPlaneStates());
        boolean[] guards = parseGuards(g.getGuards());
        int plane = resolvePlane(planes, planeIndex, g.getActivePlane() == null ? 0 : g.getActivePlane());
        if (planes[plane] == BoardGameRules.FINISH_POS) {
            throw new BoardGameException("第 " + (plane + 1) + " 架飞机已登临神位，请另选一架");
        }

        UserCultivation uc = getOrInit(af.getId());
        boolean ward = g.getWard() != null && g.getWard() == 1;
        int dice = ThreadLocalRandom.current().nextInt(1, 7);
        FlightMove fm = flightMove(planes, guards, plane, dice, uc, ward);
        if (fm.settle != null && fm.settle.wardUsed) g.setWard(0);
        cultivationMapper.updateById(uc);

        int extraCopper = 0;
        StringBuilder special = new StringBuilder();
        if (fm.tookOff) special.append("掷出六点，").append(planeName(plane)).append("起飞");
        if (fm.bounce) special.append(special.length() > 0 ? "；" : "").append("越过神位，原路反弹");
        if (fm.jumped) special.append(special.length() > 0 ? "；" : "").append("同色格飞跃四步");
        if (fm.guardHit) {
            extraCopper = BoardGameRules.GUARD_BOUNTY_COPPER;
            uc.setCopper((uc.getCopper() == null ? 0 : uc.getCopper()) + extraCopper);
            cultivationMapper.updateById(uc);
            special.append(special.length() > 0 ? "；" : "").append("撞破劫妖，赏赐 ◉").append(extraCopper);
        }
        if (fm.finished) special.append(special.length() > 0 ? "；" : "").append(planeName(plane)).append("登临神位");
        if (special.length() == 0 && fm.special != null) special.append(fm.special);

        String diceLog = (g.getDiceLog() == null ? "" : g.getDiceLog())
                + plane + ":" + dice + "→" + fm.to + ",";
        // 防御：单局最多 60 次掷骰，日志本身只用于追溯。限制长度避免旧库 varchar(255) 列溢出导致
        // UPDATE 失败、整局卡死；保留最近记录（从最后一个完整条目起），既省空间又不影响玩法。
        if (diceLog.length() > 4000) {
            int cut = diceLog.length() - 4000;
            int comma = diceLog.indexOf(',', cut);
            diceLog = comma >= 0 ? diceLog.substring(comma + 1) : diceLog.substring(cut);
        }
        g.setPlaneStates(BoardGameRules.formatPlanes(planes));
        g.setGuards(formatGuards(guards));
        g.setActivePlane(plane);
        g.setPosition(maxPlane(planes));
        g.setExtraRoll(dice == BoardGameRules.TAKEOFF_ROLL ? 1 : 0);
        g.setRolls((g.getRolls() == null ? 0 : g.getRolls()) + 1);
        g.setDiceLog(diceLog);
        g.setGainCopper((g.getGainCopper() == null ? 0 : g.getGainCopper())
                + (fm.settle == null ? 0 : fm.settle.copper) + extraCopper);
        g.setGainPower((g.getGainPower() == null ? 0 : g.getGainPower())
                + (fm.settle == null ? 0 : fm.settle.power));

        boolean allDone = BoardGameRules.allFinished(planes);
        boolean outOfRolls = g.getRolls() >= BoardGameRules.MAX_ROLLS;
        boolean finished = allDone || outOfRolls;
        if (finished) {
            g.setStatus("finished");
            g.setPass(allDone ? 1 : 0);
            g.setFinishedAt(LocalDateTime.now());
        }
        gameMapper.updateById(g);

        BoardGameRules.Cell cell = fm.to >= 0 ? BoardGameRules.cellAt(fm.to) : null;
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("dice", dice); r.put("from", fm.from); r.put("to", fm.to);
        r.put("plane", plane); r.put("plane_name", planeName(plane));
        r.put("took_off", fm.tookOff); r.put("jumped", fm.jumped);
        r.put("bounce", fm.bounce); r.put("guard_hit", fm.guardHit);
        r.put("guard_copper", extraCopper);
        r.put("cell_name", cell == null ? "停机坪" : cell.name);
        r.put("cell_icon", cell == null ? "◇" : cell.icon);
        r.put("cell_type", cell == null ? "HANGAR" : cell.type);
        r.put("cell_desc", cell == null ? "尚未起飞" : cell.desc);
        r.put("gain_copper", (fm.settle == null ? 0 : fm.settle.copper) + extraCopper);
        r.put("gain_power", fm.settle == null ? 0 : fm.settle.power);
        r.put("ward_used", fm.settle != null && fm.settle.wardUsed);
        r.put("item_gained", fm.settle == null ? null : fm.settle.itemGained);
        r.put("item_name", fm.settle == null ? null : fm.settle.itemName);
        r.put("item_icon", fm.settle == null ? null : fm.settle.itemIcon);
        r.put("special", special.toString());
        r.put("extra_roll", dice == BoardGameRules.TAKEOFF_ROLL);
        r.put("planes", java.util.Arrays.stream(planes).boxed().collect(Collectors.toList()));
        r.put("pass", g.getPass()); r.put("finished", finished);
        r.put("left_rolls", Math.max(0, BoardGameRules.MAX_ROLLS - g.getRolls()));
        r.put("game", gameView(g));
        r.put("asset", assetView(uc));
        r.put("items", itemBag(af.getId()));
        return r;
    }

    /** 执行飞行棋一步：起飞 / 行棋 / 同色飞跃 / 撞妖 / 落格结算 */
    private FlightMove flightMove(int[] planes, boolean[] guards, int plane, int dice,
                                  UserCultivation uc, boolean ward) {
        FlightMove fm = new FlightMove();
        fm.plane = plane; fm.dice = dice;
        int from = planes[plane];
        fm.from = from;

        // 停机坪：只有掷出 6 点可起飞到起点 0
        if (from == BoardGameRules.HANGAR) {
            if (dice != BoardGameRules.TAKEOFF_ROLL) {
                fm.to = BoardGameRules.HANGAR;
                fm.special = "未掷出六点，留在停机坪";
                return fm;
            }
            fm.to = 0;
            fm.tookOff = true;
            planes[plane] = fm.to;
            return fm; // 起点不结算收益
        }

        // 行棋：超过终点则反弹
        int to = BoardGameRules.movedTo(from, dice);
        fm.bounce = from + dice > BoardGameRules.FINISH_POS;
        // 同色格飞跃（终点不再飞跃）
        if (to < BoardGameRules.FINISH_POS) {
            int jumped = BoardGameRules.jumpTarget(to, plane);
            if (jumped != to) { fm.jumped = true; to = jumped; }
        }
        fm.to = to;
        planes[plane] = to;

        // 撞破劫妖（单人局守军）
        for (int i = 0; i < BoardGameRules.GUARD_CELLS.length; i++) {
            if (to >= 0 && to == BoardGameRules.GUARD_CELLS[i] && guards[i]) {
                guards[i] = false;
                fm.guardHit = true;
                break;
            }
        }

        // 落格结算（终点格也结算通关大奖）
        fm.settle = settle(BoardGameRules.cellAt(to), uc, ward);
        fm.finished = to == BoardGameRules.FINISH_POS;
        return fm;
    }

    private String planeName(int plane) {
        String[] names = {"青鸾", "金乌", "赤凰", "白鹤"};
        return plane >= 0 && plane < names.length ? names[plane] : ("第" + (plane + 1) + "机");
    }

    private int maxPlane(int[] planes) {
        int max = BoardGameRules.HANGAR;
        for (int p : planes) if (p > max) max = p;
        return max;
    }

    @Override
    public Map<String, Object> history(AvatarFoundation af) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BoardGameLog g : gameMapper.selectList(new QueryWrapper<BoardGameLog>()
                .eq("user_id", af.getId()).orderByDesc("id").last("LIMIT 20"))) list.add(gameView(g));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("history", list);
        return m;
    }

    @Override
    public Map<String, Object> rank() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("by_wins", gameMapper.rankByWins());
        m.put("by_copper", gameMapper.rankByCopper());
        m.put("by_pvp", pvpMapper.rankPvp());
        return m;
    }

    /* ================= 道具接口 ================= */

    @Override
    public Map<String, Object> items(AvatarFoundation af) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", itemBag(af.getId()));
        m.put("catalog", itemCatalog());
        return m;
    }

    @Override
    @Transactional
    public Map<String, Object> useItem(AvatarFoundation af, String itemKey, Long matchId) {
        if (itemKey == null || itemKey.isBlank()) throw new BoardGameException("未指定道具");
        if (itemCount(af.getId(), itemKey) <= 0) throw new BoardGameException("背包中没有此道具");

        // 聚宝符 / 甘霖符：即时到账，无需对局
        if (BoardGameRules.ITEM_COIN.equals(itemKey)) {
            consumeItem(af.getId(), itemKey);
            UserCultivation uc = getOrInit(af.getId());
            uc.setCopper((uc.getCopper() == null ? 0 : uc.getCopper()) + BoardGameRules.ITEM_COIN_GAIN);
            cultivationMapper.updateById(uc);
            return okItem("使用聚宝符，铜币 +" + BoardGameRules.ITEM_COIN_GAIN, uc, af);
        }
        if (BoardGameRules.ITEM_RAIN.equals(itemKey)) {
            consumeItem(af.getId(), itemKey);
            UserCultivation uc = getOrInit(af.getId());
            uc.setPower((uc.getPower() == null ? 0 : uc.getPower()) + BoardGameRules.ITEM_RAIN_GAIN);
            cultivationMapper.updateById(uc);
            return okItem("使用甘霖符，神尊力 +" + BoardGameRules.ITEM_RAIN_GAIN, uc, af);
        }

        // 镇厄符 / 缩地符：需作用于进行中的对局
        if (matchId != null) {
            return useItemInPvp(af, itemKey, matchId);
        }
        BoardGameLog g = playingGame(af.getId());
        if (g == null) throw new BoardGameException("请先入局，再使用此道具");

        UserCultivation uc = getOrInit(af.getId());
        if (BoardGameRules.ITEM_WARD.equals(itemKey)) {
            consumeItem(af.getId(), itemKey);
            g.setWard(1);
            gameMapper.updateById(g);
            return okItem("镇厄符护体，本局免疫下一次劫难", uc, af);
        }
        if (BoardGameRules.ITEM_LEAP.equals(itemKey)) {
            int[] planes = BoardGameRules.parsePlanes(g.getPlaneStates());
            int plane = resolvePlane(planes, null, g.getActivePlane() == null ? 0 : g.getActivePlane());
            if (planes[plane] == BoardGameRules.HANGAR) {
                throw new BoardGameException("该飞机尚在停机坪，请先掷出六点起飞");
            }
            if (planes[plane] == BoardGameRules.FINISH_POS) {
                throw new BoardGameException("该飞机已登临神位，请另选一架");
            }
            consumeItem(af.getId(), itemKey);
            int from = planes[plane];
            int to = BoardGameRules.movedTo(from, BoardGameRules.ITEM_LEAP_STEPS);
            boolean jumped = false;
            if (to < BoardGameRules.FINISH_POS) {
                int j = BoardGameRules.jumpTarget(to, plane);
                if (j != to) { to = j; jumped = true; }
            }
            BoardGameRules.Cell cell = BoardGameRules.cellAt(to);
            boolean ward = g.getWard() != null && g.getWard() == 1;
            SettleResult sr = settle(cell, uc, ward);
            if (sr.wardUsed) g.setWard(0);
            cultivationMapper.updateById(uc);
            planes[plane] = to;
            g.setPlaneStates(BoardGameRules.formatPlanes(planes));
            g.setActivePlane(plane);
            g.setPosition(maxPlane(planes));
            g.setGainCopper((g.getGainCopper() == null ? 0 : g.getGainCopper()) + sr.copper);
            g.setGainPower((g.getGainPower() == null ? 0 : g.getGainPower()) + sr.power);
            boolean reachedFinish = to == BoardGameRules.FINISH_POS;
            boolean allDone = BoardGameRules.allFinished(planes);
            if (allDone || reachedFinish && g.getRolls() >= BoardGameRules.MAX_ROLLS) {
                g.setStatus("finished"); g.setPass(allDone ? 1 : 0); g.setFinishedAt(LocalDateTime.now());
            }
            gameMapper.updateById(g);
            Map<String, Object> r = okItem("缩地符：" + planeName(plane) + "前进 "
                    + BoardGameRules.ITEM_LEAP_STEPS + " 格" + (jumped ? "并同色飞跃" : "") + "，落「" + cell.name + "」", uc, af);
            r.put("to", to); r.put("cell_name", cell.name);
            r.put("gain_copper", sr.copper); r.put("gain_power", sr.power);
            r.put("item_gained", sr.itemGained); r.put("item_name", sr.itemName);
            r.put("pass", allDone ? 1 : 0);
            r.put("game", gameView(g));
            return r;
        }
        throw new BoardGameException("该道具暂不支持在单人局使用");
    }

    private Map<String, Object> okItem(String msg, UserCultivation uc, AvatarFoundation af) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true); r.put("msg", msg);
        r.put("asset", assetView(uc));
        r.put("items", itemBag(af.getId()));
        return r;
    }

    /* ================= PVP 擂台 ================= */

    private Map<String, Object> pvpSummary(AvatarFoundation af) {
        int uid = af.getId().intValue();
        List<BoardPvpMatch> all = pvpMapper.selectList(new QueryWrapper<BoardPvpMatch>()
                .and(w -> w.eq("challenger_id", uid).or().eq("defender_id", uid))
                .orderByDesc("id"));
        int played = all.size();
        int wins = 0;
        for (BoardPvpMatch m : all) {
            if ("finished".equals(m.getStatus())) {
                if ((m.getWinner() != null && m.getWinner().equals("challenger") && uid == m.getChallengerId())
                        || (m.getWinner() != null && m.getWinner().equals("defender") && uid == m.getDefenderId())) wins++;
            }
        }
        BoardPvpMatch playing = null;
        for (BoardPvpMatch m : all) {
            if ("playing".equals(m.getStatus())) { playing = m; break; }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("played", played); r.put("wins", wins);
        r.put("current", pvpView(playing));
        return r;
    }

    private Map<String, Object> pvpView(BoardPvpMatch m) {
        if (m == null) return null;
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", m.getId());
        r.put("challenger_id", m.getChallengerId());
        r.put("defender_id", m.getDefenderId());
        r.put("defender_ai", m.getDefenderAi() != null && m.getDefenderAi() == 1);
        r.put("status", m.getStatus());
        r.put("challenger_pos", m.getChallengerPos());
        r.put("defender_pos", m.getDefenderPos());
        // 飞行棋：双方飞机位置 / 当前选中机 / 再掷标记
        r.put("challenger_planes", Arrays.stream(BoardGameRules.parsePlanes(m.getChallengerPlanes()))
                .boxed().collect(Collectors.toList()));
        r.put("defender_planes", Arrays.stream(BoardGameRules.parsePlanes(m.getDefenderPlanes()))
                .boxed().collect(Collectors.toList()));
        r.put("challenger_active", m.getChallengerActive() == null ? 0 : m.getChallengerActive());
        r.put("defender_active", m.getDefenderActive() == null ? 0 : m.getDefenderActive());
        r.put("extra_roll", m.getExtraRoll() != null && m.getExtraRoll() == 1);
        r.put("challenger_rolls", m.getChallengerRolls());
        r.put("defender_rolls", m.getDefenderRolls());
        r.put("turn", m.getTurn());
        r.put("pot", m.getPot());
        r.put("winner", m.getWinner());
        r.put("challenger_gain_copper", m.getChallengerGainCopper());
        r.put("challenger_gain_power", m.getChallengerGainPower());
        r.put("defender_gain_copper", m.getDefenderGainCopper());
        r.put("defender_gain_power", m.getDefenderGainPower());
        r.put("challenger_ward", m.getChallengerWard() != null && m.getChallengerWard() == 1);
        r.put("defender_ward", m.getDefenderWard() != null && m.getDefenderWard() == 1);
        r.put("dice_log", m.getDiceLog());
        AvatarFoundation ca = avatarMapper.selectById(m.getChallengerId());
        AvatarFoundation da = m.getDefenderId() == null ? null : avatarMapper.selectById(m.getDefenderId());
        r.put("challenger_name", ca == null ? "?" : ca.getDeityName());
        r.put("defender_name", m.getDefenderAi() != null && m.getDefenderAi() == 1
                ? "天道·无极" : (da == null ? "待应战" : da.getDeityName()));
        r.put("created_at", m.getCreatedAt() == null ? null : m.getCreatedAt().toString());
        r.put("finished_at", m.getFinishedAt() == null ? null : m.getFinishedAt().toString());
        return r;
    }

    @Override
    public Map<String, Object> pvpStatus(AvatarFoundation af) {
        Map<String, Object> r = new LinkedHashMap<>(pvpSummary(af));
        int uid = af.getId().intValue();
        List<Map<String, Object>> matches = new ArrayList<>();
        for (BoardPvpMatch m : pvpMapper.selectList(new QueryWrapper<BoardPvpMatch>()
                .and(w -> w.eq("challenger_id", uid).or().eq("defender_id", uid))
                .orderByDesc("id").last("LIMIT 20"))) matches.add(pvpView(m));
        r.put("matches", matches);
        r.put("entry_fee", BoardGameRules.PVP_ENTRY_FEE);
        r.put("daily_limit", BoardGameRules.PVP_DAILY_LIMIT);
        r.put("max_rolls", BoardGameRules.PVP_MAX_ROLLS);
        r.put("win_copper", BoardGameRules.PVP_WIN_COPPER);
        r.put("win_power", BoardGameRules.PVP_WIN_POWER);
        r.put("flight", flightRuleView());
        r.put("asset", assetView(getOrInit(af.getId())));
        r.put("items", itemBag(af.getId()));
        return r;
    }

    @Override
    public Map<String, Object> pvpList(AvatarFoundation af) {
        int uid = af.getId().intValue();
        List<Map<String, Object>> list = new ArrayList<>();
        for (BoardPvpMatch m : pvpMapper.selectList(new QueryWrapper<BoardPvpMatch>()
                .eq("status", "waiting").ne("challenger_id", uid)
                .orderByDesc("id").last("LIMIT 20"))) list.add(pvpView(m));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("list", list);
        return r;
    }

    @Override
    @Transactional
    public Map<String, Object> pvpCreate(AvatarFoundation af, String opponentName, boolean versusAi) {
        int uid = af.getId().intValue();
        // 限制进行中对局
        Long playing = pvpMapper.selectCount(new QueryWrapper<BoardPvpMatch>()
                .eq("status", "playing")
                .and(w -> w.eq("challenger_id", uid).or().eq("defender_id", uid)));
        if (playing != null && playing > 0) throw new BoardGameException("你有一场PVP对战尚未结束");

        // 每日场次（按发起方）
        Long todayCnt = pvpMapper.selectCount(new QueryWrapper<BoardPvpMatch>()
                .eq("challenger_id", uid).ge("created_at", LocalDate.now().atStartOfDay()));
        if (todayCnt != null && todayCnt >= BoardGameRules.PVP_DAILY_LIMIT)
            throw new BoardGameException("今日擂台场次已用尽（每日 " + BoardGameRules.PVP_DAILY_LIMIT + " 场）");

        UserCultivation uc = getOrInit(af.getId());
        if (uc.getCopper() == null || uc.getCopper() < BoardGameRules.PVP_ENTRY_FEE)
            throw new BoardGameException("铜币不足：擂台报名需 " + BoardGameRules.PVP_ENTRY_FEE + " 铜币");

        Integer defenderId = null;
        Integer defenderAi = 0;
        String status = "waiting";
        String turn = "challenger";
        if (versusAi || opponentName == null || opponentName.isBlank()) {
            defenderAi = 1; status = "playing";  // AI 直接接战
        } else {
            AvatarFoundation opp = avatarMapper.selectOne(new QueryWrapper<AvatarFoundation>()
                    .eq("deity_name", opponentName.trim()));
            if (opp == null) throw new BoardGameException("未寻得应战者：" + opponentName);
            if (opp.getId().intValue() == uid) throw new BoardGameException("不可挑战自己");
            defenderId = opp.getId().intValue();
        }

        uc.setCopper(uc.getCopper() - BoardGameRules.PVP_ENTRY_FEE);
        cultivationMapper.updateById(uc);

        BoardPvpMatch m = new BoardPvpMatch();
        m.setChallengerId(uid); m.setDefenderId(defenderId); m.setDefenderAi(defenderAi);
        m.setStatus(status); m.setChallengerPos(BoardGameRules.HANGAR); m.setDefenderPos(BoardGameRules.HANGAR);
        String initialPlanes = BoardGameRules.formatPlanes(new int[]{
                BoardGameRules.HANGAR, BoardGameRules.HANGAR,
                BoardGameRules.HANGAR, BoardGameRules.HANGAR});
        m.setChallengerPlanes(initialPlanes); m.setDefenderPlanes(initialPlanes);
        m.setChallengerActive(0); m.setDefenderActive(0); m.setExtraRoll(0);
        m.setChallengerRolls(0); m.setDefenderRolls(0); m.setTurn(turn);
        m.setPot(BoardGameRules.PVP_ENTRY_FEE);
        m.setChallengerGainCopper(0); m.setChallengerGainPower(0);
        m.setDefenderGainCopper(0); m.setDefenderGainPower(0);
        m.setChallengerWard(0); m.setDefenderWard(0);
        m.setDiceLog(""); m.setCreatedAt(LocalDateTime.now());
        pvpMapper.insert(m);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("match", pvpView(m));
        r.put("asset", assetView(uc));
        r.put("msg", defenderAi == 1 ? "天道应战，擂台开鼓！" : "挑战已发出，静候 " + opponentName + " 应战");
        return r;
    }

    @Override
    @Transactional
    public Map<String, Object> pvpAccept(AvatarFoundation af, Long matchId) {
        if (matchId == null) throw new BoardGameException("未指定对局");
        int uid = af.getId().intValue();
        BoardPvpMatch m = pvpMapper.selectById(matchId);
        if (m == null) throw new BoardGameException("对局不存在");
        if (!"waiting".equals(m.getStatus())) throw new BoardGameException("该挑战不可应战");
        if (m.getDefenderId() != null && m.getDefenderId() != uid)
            throw new BoardGameException("此挑战并非向你发出");
        if (m.getChallengerId() == uid) throw new BoardGameException("不可应战自己的挑战");

        UserCultivation uc = getOrInit(af.getId());
        if (uc.getCopper() == null || uc.getCopper() < BoardGameRules.PVP_ENTRY_FEE)
            throw new BoardGameException("铜币不足：应战需 " + BoardGameRules.PVP_ENTRY_FEE + " 铜币");
        uc.setCopper(uc.getCopper() - BoardGameRules.PVP_ENTRY_FEE);
        cultivationMapper.updateById(uc);

        m.setDefenderId(uid); m.setStatus("playing"); m.setTurn("challenger");
        m.setPot((m.getPot() == null ? 0 : m.getPot()) + BoardGameRules.PVP_ENTRY_FEE);
        pvpMapper.updateById(m);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("match", pvpView(m)); r.put("asset", assetView(uc));
        r.put("msg", "应战成功，擂台开鼓！");
        return r;
    }

    @Override
    @Transactional
    public Map<String, Object> pvpRoll(AvatarFoundation af, Long matchId, Integer planeIndex) {
        if (matchId == null) throw new BoardGameException("未指定对局");
        int uid = af.getId().intValue();
        BoardPvpMatch m = pvpMapper.selectById(matchId);
        if (m == null) throw new BoardGameException("对局不存在");
        if (!"playing".equals(m.getStatus())) throw new BoardGameException("对局未在进行中");
        boolean isChallenger = uid == m.getChallengerId();
        boolean isDefender = m.getDefenderId() != null && uid == m.getDefenderId();
        if (!isChallenger && !isDefender) throw new BoardGameException("你不是此局的对战者");
        if (!m.getTurn().equals(isChallenger ? "challenger" : "defender"))
            throw new BoardGameException("尚未轮到你行棋");

        StringBuilder trace = new StringBuilder();
        // 玩家掷一步。掷 6 时 pvpStep 保持回合不换手并置 extra_roll，
        // 由前端提示"可再掷"，玩家可重新选飞机再掷（不在此处自动连掷）。
        trace.append(pvpStep(m, isChallenger ? "challenger" : "defender", planeIndex));

        // 天道AI 自动代掷：掷 6 也连续掷，直到换回玩家或结束
        int guard = 0;
        while ("playing".equals(m.getStatus())
                && "defender".equals(m.getTurn())
                && m.getDefenderAi() != null && m.getDefenderAi() == 1
                && guard++ < BoardGameRules.PVP_MAX_ROLLS + 4) {
            trace.append(" | ").append(pvpStep(m, "defender", null));
        }
        pvpMapper.updateById(m);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("match", pvpView(m));
        r.put("asset", assetView(getOrInit(af.getId())));
        r.put("items", itemBag(af.getId()));
        r.put("trace", trace.toString());
        r.put("finished", "finished".equals(m.getStatus()));
        if ("finished".equals(m.getStatus())) {
            boolean iWon = (isChallenger && "challenger".equals(m.getWinner()))
                    || (isDefender && "defender".equals(m.getWinner()));
            r.put("win", iWon);
            r.put("msg", iWon ? "擂台得胜！奖池与赏赐已入囊" : "惜败擂台，来日再战");
        } else {
            r.put("win", false);
            r.put("msg", "行棋落定，静待回合");
        }
        return r;
    }

    /** PVP 单方飞行棋一步：起飞 / 行棋 / 同色飞跃 / 撞子 / 落格结算 / 胜利判定 */
    private String pvpStep(BoardPvpMatch m, String side, Integer requestedPlane) {
        boolean challenger = "challenger".equals(side);
        int[] myPlanes = BoardGameRules.parsePlanes(challenger ? m.getChallengerPlanes() : m.getDefenderPlanes());
        int[] foePlanes = BoardGameRules.parsePlanes(challenger ? m.getDefenderPlanes() : m.getChallengerPlanes());
        int fallback = challenger
                ? (m.getChallengerActive() == null ? 0 : m.getChallengerActive())
                : (m.getDefenderActive() == null ? 0 : m.getDefenderActive());
        int plane = resolvePlane(myPlanes, requestedPlane, fallback);
        if (myPlanes[plane] == BoardGameRules.FINISH_POS) {
            // 当前选中机已登顶，换一架
            plane = resolvePlane(myPlanes, null, -1);
        }

        int dice = ThreadLocalRandom.current().nextInt(1, 7);
        int from = myPlanes[plane];
        int to;
        boolean tookOff = false, jumped = false, bounce = false;
        if (from == BoardGameRules.HANGAR) {
            if (dice == BoardGameRules.TAKEOFF_ROLL) {
                to = 0; tookOff = true; myPlanes[plane] = to;
            } else {
                to = BoardGameRules.HANGAR;
            }
        } else {
            to = BoardGameRules.movedTo(from, dice);
            bounce = from + dice > BoardGameRules.FINISH_POS;
            if (to < BoardGameRules.FINISH_POS) {
                int j = BoardGameRules.jumpTarget(to, plane);
                if (j != to) { to = j; jumped = true; }
            }
            myPlanes[plane] = to;
        }

        // 撞子：落点与敌机重合，敌机回停机坪
        int bumped = -1;
        if (to >= 0 && to < BoardGameRules.FINISH_POS) {
            for (int i = 0; i < foePlanes.length; i++) {
                if (foePlanes[i] == to) { foePlanes[i] = BoardGameRules.HANGAR; bumped = i; }
            }
            // 同色己机可叠机，不互撞
        }

        StringBuilder detail = new StringBuilder();
        detail.append(challenger ? "挑战者" : "对手").append("掷").append(dice)
                .append("·").append(planeName(plane));
        if (tookOff) detail.append("起飞");
        else if (to == BoardGameRules.HANGAR) detail.append("留坪");
        else detail.append("→格").append(to);
        if (jumped) detail.append("✈飞跃");
        if (bounce) detail.append("↩反弹");
        if (bumped >= 0) detail.append("⚔撞落敌机").append(bumped + 1);
        if (to == BoardGameRules.FINISH_POS) detail.append("登神位");

        // 落格结算（仅真实玩家入库；AI 虚拟计展示）
        SettleResult sr = null;
        if (to >= 0) {
            BoardGameRules.Cell cell = BoardGameRules.cellAt(to);
            detail.append("「").append(cell.name).append("」");
            if (!tookOff) {
                if (challenger) {
                    UserCultivation uc = getOrInit(m.getChallengerId().longValue());
                    boolean ward = m.getChallengerWard() != null && m.getChallengerWard() == 1;
                    sr = settle(cell, uc, ward);
                    if (sr.wardUsed) m.setChallengerWard(0);
                    cultivationMapper.updateById(uc);
                    m.setChallengerGainCopper((m.getChallengerGainCopper() == null ? 0 : m.getChallengerGainCopper()) + sr.copper);
                    m.setChallengerGainPower((m.getChallengerGainPower() == null ? 0 : m.getChallengerGainPower()) + sr.power);
                } else if (m.getDefenderId() != null) {
                    UserCultivation uc = getOrInit(m.getDefenderId().longValue());
                    boolean ward = m.getDefenderWard() != null && m.getDefenderWard() == 1;
                    sr = settle(cell, uc, ward);
                    if (sr.wardUsed) m.setDefenderWard(0);
                    cultivationMapper.updateById(uc);
                    m.setDefenderGainCopper((m.getDefenderGainCopper() == null ? 0 : m.getDefenderGainCopper()) + sr.copper);
                    m.setDefenderGainPower((m.getDefenderGainPower() == null ? 0 : m.getDefenderGainPower()) + sr.power);
                } else {
                    int gainC = BoardGameRules.T_COPPER.equals(cell.type) || BoardGameRules.T_BOON.equals(cell.type)
                            || BoardGameRules.T_FINISH.equals(cell.type) ? cell.copper : 0;
                    int gainP = BoardGameRules.T_POWER.equals(cell.type) || BoardGameRules.T_BOON.equals(cell.type)
                            || BoardGameRules.T_FINISH.equals(cell.type) ? cell.power : 0;
                    m.setDefenderGainCopper((m.getDefenderGainCopper() == null ? 0 : m.getDefenderGainCopper()) + gainC);
                    m.setDefenderGainPower((m.getDefenderGainPower() == null ? 0 : m.getDefenderGainPower()) + gainP);
                }
            }
        }

        if (challenger) {
            m.setChallengerPlanes(BoardGameRules.formatPlanes(myPlanes));
            m.setChallengerPos(maxPlane(myPlanes));
            m.setChallengerActive(plane);
            m.setChallengerRolls((m.getChallengerRolls() == null ? 0 : m.getChallengerRolls()) + 1);
            m.setDefenderPlanes(BoardGameRules.formatPlanes(foePlanes));
        } else {
            m.setDefenderPlanes(BoardGameRules.formatPlanes(myPlanes));
            m.setDefenderPos(maxPlane(myPlanes));
            m.setDefenderActive(plane);
            m.setDefenderRolls((m.getDefenderRolls() == null ? 0 : m.getDefenderRolls()) + 1);
            m.setChallengerPlanes(BoardGameRules.formatPlanes(foePlanes));
        }

        String log = (m.getDiceLog() == null ? "" : m.getDiceLog()) + detail + ";";
        m.setDiceLog(log.length() > 900 ? log.substring(log.length() - 900) : log);

        boolean allDone = BoardGameRules.allFinished(myPlanes);
        boolean outOfRolls = (challenger ? m.getChallengerRolls() : m.getDefenderRolls()) >= BoardGameRules.PVP_MAX_ROLLS;
        if (allDone || outOfRolls) {
            m.setStatus("finished");
            m.setWinner(side);
            m.setFinishedAt(LocalDateTime.now());
            Long winnerId = null;
            if (challenger) winnerId = m.getChallengerId().longValue();
            else if (m.getDefenderId() != null) winnerId = m.getDefenderId().longValue();
            if (winnerId != null) {
                UserCultivation wu = getOrInit(winnerId);
                wu.setCopper((wu.getCopper() == null ? 0 : wu.getCopper())
                        + (m.getPot() == null ? 0 : m.getPot()) + BoardGameRules.PVP_WIN_COPPER);
                wu.setPower((wu.getPower() == null ? 0 : wu.getPower()) + BoardGameRules.PVP_WIN_POWER);
                cultivationMapper.updateById(wu);
            }
            detail.append(" ★").append(challenger ? "挑战者" : "对手").append("四机登顶获胜");
        } else if (dice == BoardGameRules.TAKEOFF_ROLL) {
            // 掷 6 再掷：不换手
            m.setExtraRoll(1);
            m.setTurn(side);
        } else {
            m.setExtraRoll(0);
            m.setTurn(challenger ? "defender" : "challenger");
        }
        return detail.toString();
    }

    /** 在 PVP 局中使用镇厄符 / 缩地符 */
    private Map<String, Object> useItemInPvp(AvatarFoundation af, String itemKey, Long matchId) {
        int uid = af.getId().intValue();
        BoardPvpMatch m = pvpMapper.selectById(matchId);
        if (m == null) throw new BoardGameException("对局不存在");
        if (!"playing".equals(m.getStatus())) throw new BoardGameException("对局未在进行中");
        boolean challenger = uid == m.getChallengerId();
        boolean defender = m.getDefenderId() != null && uid == m.getDefenderId();
        if (!challenger && !defender) throw new BoardGameException("你不是此局的对战者");

        UserCultivation uc = getOrInit(af.getId());
        if (BoardGameRules.ITEM_WARD.equals(itemKey)) {
            consumeItem(af.getId(), itemKey);
            if (challenger) m.setChallengerWard(1); else m.setDefenderWard(1);
            pvpMapper.updateById(m);
            return okItem("镇厄符护体，本场免疫下一次劫难", uc, af);
        }
        if (BoardGameRules.ITEM_LEAP.equals(itemKey)) {
            int[] planes = BoardGameRules.parsePlanes(challenger ? m.getChallengerPlanes() : m.getDefenderPlanes());
            int fallback = challenger
                    ? (m.getChallengerActive() == null ? 0 : m.getChallengerActive())
                    : (m.getDefenderActive() == null ? 0 : m.getDefenderActive());
            int plane = resolvePlane(planes, null, fallback);
            if (planes[plane] == BoardGameRules.HANGAR) {
                throw new BoardGameException("该飞机尚在停机坪，请先掷出六点起飞");
            }
            consumeItem(af.getId(), itemKey);
            int from = planes[plane];
            int to = BoardGameRules.movedTo(from, BoardGameRules.ITEM_LEAP_STEPS);
            if (to < BoardGameRules.FINISH_POS) {
                int j = BoardGameRules.jumpTarget(to, plane);
                if (j != to) to = j;
            }
            BoardGameRules.Cell cell = BoardGameRules.cellAt(to);
            boolean ward = challenger ? (m.getChallengerWard() != null && m.getChallengerWard() == 1)
                                       : (m.getDefenderWard() != null && m.getDefenderWard() == 1);
            if (challenger) {
                SettleResult sr = settle(cell, uc, ward);
                if (sr.wardUsed) m.setChallengerWard(0);
                cultivationMapper.updateById(uc);
                m.setChallengerGainCopper((m.getChallengerGainCopper() == null ? 0 : m.getChallengerGainCopper()) + sr.copper);
                m.setChallengerGainPower((m.getChallengerGainPower() == null ? 0 : m.getChallengerGainPower()) + sr.power);
            }
            planes[plane] = to;
            if (challenger) {
                m.setChallengerPlanes(BoardGameRules.formatPlanes(planes));
                m.setChallengerPos(maxPlane(planes)); m.setChallengerActive(plane);
            } else {
                m.setDefenderPlanes(BoardGameRules.formatPlanes(planes));
                m.setDefenderPos(maxPlane(planes)); m.setDefenderActive(plane);
            }
            boolean allDone = BoardGameRules.allFinished(planes);
            if (allDone) { m.setStatus("finished"); m.setWinner(challenger ? "challenger" : "defender"); m.setFinishedAt(LocalDateTime.now()); }
            else m.setTurn(challenger ? "defender" : "challenger");
            pvpMapper.updateById(m);

            // AI 自动应手
            int guard = 0;
            while ("playing".equals(m.getStatus()) && "defender".equals(m.getTurn())
                    && m.getDefenderAi() != null && m.getDefenderAi() == 1
                    && guard++ < BoardGameRules.PVP_MAX_ROLLS + 4) pvpStep(m, "defender", null);
            pvpMapper.updateById(m);

            Map<String, Object> r = okItem("缩地符：" + planeName(plane) + "前进，落「" + cell.name + "」", uc, af);
            r.put("match", pvpView(m));
            r.put("asset", assetView(getOrInit(af.getId())));
            return r;
        }
        throw new BoardGameException("该道具需在弈局中使用");
    }

    /** 桌游业务异常（由全局异常处理器转为 {ok:false,msg}） */
    public static class BoardGameException extends RuntimeException {
        public BoardGameException(String msg) { super(msg); }
    }
}
