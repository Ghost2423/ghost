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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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
        g.setUserId(af.getId()); g.setStatus("playing"); g.setPosition(0); g.setRolls(0);
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
    public Map<String, Object> roll(AvatarFoundation af) {
        BoardGameLog g = playingGame(af.getId());
        if (g == null) throw new BoardGameException("当前无进行中的弈局，请先入局");
        if (g.getRolls() >= BoardGameRules.MAX_ROLLS) throw new BoardGameException("本局掷骰次数已用尽");

        UserCultivation uc = getOrInit(af.getId());
        boolean ward = g.getWard() != null && g.getWard() == 1;

        int dice = ThreadLocalRandom.current().nextInt(1, 7);
        int from = g.getPosition() == null ? 0 : g.getPosition();
        int to = Math.min(from + dice, BoardGameRules.BOARD_SIZE - 1);
        BoardGameRules.Cell cell = BoardGameRules.cellAt(to);

        SettleResult sr = settle(cell, uc, ward);
        if (sr.wardUsed) g.setWard(0);
        cultivationMapper.updateById(uc);

        String diceLog = (g.getDiceLog() == null ? "" : g.getDiceLog()) + dice + ",";
        g.setPosition(to);
        g.setRolls((g.getRolls() == null ? 0 : g.getRolls()) + 1);
        g.setDiceLog(diceLog);
        g.setGainCopper((g.getGainCopper() == null ? 0 : g.getGainCopper()) + sr.copper);
        g.setGainPower((g.getGainPower() == null ? 0 : g.getGainPower()) + sr.power);

        boolean reachedFinish = to >= BoardGameRules.BOARD_SIZE - 1;
        boolean outOfRolls = g.getRolls() >= BoardGameRules.MAX_ROLLS;
        boolean finished = reachedFinish || outOfRolls;
        if (finished) {
            g.setStatus("finished");
            g.setPass(reachedFinish ? 1 : 0);
            g.setFinishedAt(LocalDateTime.now());
        }
        gameMapper.updateById(g);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("dice", dice); r.put("from", from); r.put("to", to);
        r.put("cell_name", cell.name); r.put("cell_icon", cell.icon);
        r.put("cell_type", cell.type); r.put("cell_desc", cell.desc);
        r.put("gain_copper", sr.copper); r.put("gain_power", sr.power);
        r.put("ward_used", sr.wardUsed);
        r.put("item_gained", sr.itemGained);
        r.put("item_name", sr.itemName);
        r.put("item_icon", sr.itemIcon);
        r.put("pass", g.getPass()); r.put("finished", finished);
        r.put("left_rolls", Math.max(0, BoardGameRules.MAX_ROLLS - g.getRolls()));
        r.put("game", gameView(g));
        r.put("asset", assetView(uc));
        r.put("items", itemBag(af.getId()));
        return r;
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
            consumeItem(af.getId(), itemKey);
            int from = g.getPosition() == null ? 0 : g.getPosition();
            int to = Math.min(from + BoardGameRules.ITEM_LEAP_STEPS, BoardGameRules.BOARD_SIZE - 1);
            BoardGameRules.Cell cell = BoardGameRules.cellAt(to);
            boolean ward = g.getWard() != null && g.getWard() == 1;
            SettleResult sr = settle(cell, uc, ward);
            if (sr.wardUsed) g.setWard(0);
            cultivationMapper.updateById(uc);
            g.setPosition(to);
            g.setGainCopper((g.getGainCopper() == null ? 0 : g.getGainCopper()) + sr.copper);
            g.setGainPower((g.getGainPower() == null ? 0 : g.getGainPower()) + sr.power);
            boolean reachedFinish = to >= BoardGameRules.BOARD_SIZE - 1;
            if (reachedFinish) {
                g.setStatus("finished"); g.setPass(1); g.setFinishedAt(LocalDateTime.now());
            }
            gameMapper.updateById(g);
            Map<String, Object> r = okItem("缩地符：前进 " + BoardGameRules.ITEM_LEAP_STEPS + " 格，落「" + cell.name + "」", uc, af);
            r.put("to", to); r.put("cell_name", cell.name);
            r.put("gain_copper", sr.copper); r.put("gain_power", sr.power);
            r.put("item_gained", sr.itemGained); r.put("item_name", sr.itemName);
            r.put("pass", reachedFinish ? 1 : 0);
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
        m.setStatus(status); m.setChallengerPos(0); m.setDefenderPos(0);
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
    public Map<String, Object> pvpRoll(AvatarFoundation af, Long matchId) {
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
        // 玩家掷一步
        String step = pvpStep(m, isChallenger ? "challenger" : "defender");
        trace.append(step);

        // 若对局仍进行，且接下来轮到天道AI，则服务端自动代掷直到回到玩家或结束
        int guard = 0;
        while ("playing".equals(m.getStatus())
                && "defender".equals(m.getTurn())
                && m.getDefenderAi() != null && m.getDefenderAi() == 1
                && guard++ < BoardGameRules.PVP_MAX_ROLLS + 2) {
            String aiStep = pvpStep(m, "defender");
            trace.append(" | ").append(aiStep);
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

    /** PVP 单方走一步（掷骰 + 落格结算 + 终点判定），直接改 match 对象 */
    private String pvpStep(BoardPvpMatch m, String side) {
        boolean challenger = "challenger".equals(side);
        int from = challenger ? (m.getChallengerPos() == null ? 0 : m.getChallengerPos())
                              : (m.getDefenderPos() == null ? 0 : m.getDefenderPos());
        int dice = ThreadLocalRandom.current().nextInt(1, 7);
        int to = Math.min(from + dice, BoardGameRules.BOARD_SIZE - 1);
        BoardGameRules.Cell cell = BoardGameRules.cellAt(to);

        String detail;
        if (challenger) {
            UserCultivation uc = getOrInit(m.getChallengerId().longValue());
            boolean ward = m.getChallengerWard() != null && m.getChallengerWard() == 1;
            SettleResult sr = settle(cell, uc, ward);
            if (sr.wardUsed) m.setChallengerWard(0);
            cultivationMapper.updateById(uc);
            m.setChallengerPos(to);
            m.setChallengerRolls((m.getChallengerRolls() == null ? 0 : m.getChallengerRolls()) + 1);
            m.setChallengerGainCopper((m.getChallengerGainCopper() == null ? 0 : m.getChallengerGainCopper()) + sr.copper);
            m.setChallengerGainPower((m.getChallengerGainPower() == null ? 0 : m.getChallengerGainPower()) + sr.power);
            detail = "挑战者掷" + dice + "→格" + to + "「" + cell.name + "」";
        } else {
            // AI 对手：收益只记入对局展示，不发放到真实账户（AI 无根基）
            boolean defenderReal = m.getDefenderId() != null;
            if (defenderReal) {
                UserCultivation uc = getOrInit(m.getDefenderId().longValue());
                boolean ward = m.getDefenderWard() != null && m.getDefenderWard() == 1;
                SettleResult sr = settle(cell, uc, ward);
                if (sr.wardUsed) m.setDefenderWard(0);
                cultivationMapper.updateById(uc);
                m.setDefenderGainCopper((m.getDefenderGainCopper() == null ? 0 : m.getDefenderGainCopper()) + sr.copper);
                m.setDefenderGainPower((m.getDefenderGainPower() == null ? 0 : m.getDefenderGainPower()) + sr.power);
            } else {
                // AI：虚拟结算，仅展示，不入库
                int gainC = BoardGameRules.T_COPPER.equals(cell.type) || BoardGameRules.T_BOON.equals(cell.type)
                        || BoardGameRules.T_FINISH.equals(cell.type) ? cell.copper : 0;
                int gainP = BoardGameRules.T_POWER.equals(cell.type) || BoardGameRules.T_BOON.equals(cell.type)
                        || BoardGameRules.T_FINISH.equals(cell.type) ? cell.power : 0;
                m.setDefenderGainCopper((m.getDefenderGainCopper() == null ? 0 : m.getDefenderGainCopper()) + gainC);
                m.setDefenderGainPower((m.getDefenderGainPower() == null ? 0 : m.getDefenderGainPower()) + gainP);
            }
            m.setDefenderPos(to);
            m.setDefenderRolls((m.getDefenderRolls() == null ? 0 : m.getDefenderRolls()) + 1);
            detail = "对手掷" + dice + "→格" + to + "「" + cell.name + "」";
        }

        String log = (m.getDiceLog() == null ? "" : m.getDiceLog()) + detail + ";";
        m.setDiceLog(log.length() > 900 ? log.substring(log.length() - 900) : log);

        boolean reachedFinish = to >= BoardGameRules.BOARD_SIZE - 1;
        if (reachedFinish) {
            m.setStatus("finished");
            m.setWinner(side);
            m.setFinishedAt(LocalDateTime.now());
            // 发放奖池 + 胜利奖励给胜者（AI 获胜则奖池沉没）
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
            detail += " ★" + (challenger ? "挑战者" : "对手") + "登神位获胜";
        } else {
            m.setTurn(challenger ? "defender" : "challenger");
        }
        return detail;
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
            consumeItem(af.getId(), itemKey);
            Integer from = challenger ? m.getChallengerPos() : m.getDefenderPos();
            int to = Math.min((from == null ? 0 : from) + BoardGameRules.ITEM_LEAP_STEPS, BoardGameRules.BOARD_SIZE - 1);
            BoardGameRules.Cell cell = BoardGameRules.cellAt(to);
            boolean ward = challenger ? (m.getChallengerWard() != null && m.getChallengerWard() == 1)
                                       : (m.getDefenderWard() != null && m.getDefenderWard() == 1);
            if (challenger) {
                SettleResult sr = settle(cell, uc, ward);
                if (sr.wardUsed) m.setChallengerWard(0);
                cultivationMapper.updateById(uc);
                m.setChallengerPos(to);
                m.setChallengerGainCopper((m.getChallengerGainCopper() == null ? 0 : m.getChallengerGainCopper()) + sr.copper);
                m.setChallengerGainPower((m.getChallengerGainPower() == null ? 0 : m.getChallengerGainPower()) + sr.power);
            } else {
                m.setDefenderPos(to);
            }
            boolean reach = to >= BoardGameRules.BOARD_SIZE - 1;
            if (reach) { m.setStatus("finished"); m.setWinner(challenger ? "challenger" : "defender"); m.setFinishedAt(LocalDateTime.now()); }
            else m.setTurn(challenger ? "defender" : "challenger");
            pvpMapper.updateById(m);

            // AI 自动应手
            int guard = 0;
            while ("playing".equals(m.getStatus()) && "defender".equals(m.getTurn())
                    && m.getDefenderAi() != null && m.getDefenderAi() == 1
                    && guard++ < BoardGameRules.PVP_MAX_ROLLS + 2) pvpStep(m, "defender");
            pvpMapper.updateById(m);

            Map<String, Object> r = okItem("缩地符：前进 " + BoardGameRules.ITEM_LEAP_STEPS + " 格，落「" + cell.name + "」", uc, af);
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
