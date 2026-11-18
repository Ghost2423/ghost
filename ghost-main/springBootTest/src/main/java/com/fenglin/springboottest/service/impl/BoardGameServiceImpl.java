package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.common.BoardGameRules;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.entity.BoardGameLog;
import com.fenglin.springboottest.entity.UserCultivation;
import com.fenglin.springboottest.mapper.BoardGameLogMapper;
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
 * 落格 / 通关收益会真实写入 user_cultivation.copper / power。
 */
@Service
public class BoardGameServiceImpl implements BoardGameService {

    @Autowired
    private BoardGameLogMapper gameMapper;

    @Autowired
    private UserCultivationMapper cultivationMapper;

    /* ================= 内部工具 ================= */

    /** 取用户修炼状态；无则按默认（筑基 / 0 神尊力 / 200 铜币）惰性初始化 */
    private UserCultivation getOrInit(Long userId) {
        UserCultivation uc = cultivationMapper.selectOne(
                new QueryWrapper<UserCultivation>().eq("user_id", userId));
        if (uc != null) {
            return uc;
        }
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

    /** 今日已开局数 */
    private int todayCount(Long userId) {
        return Math.toIntExact(gameMapper.selectCount(
                new QueryWrapper<BoardGameLog>()
                        .eq("user_id", userId)
                        .ge("created_at", LocalDate.now().atStartOfDay())));
    }

    /** 当前进行中的对局（最近一条） */
    private BoardGameLog playingGame(Long userId) {
        return gameMapper.selectOne(new QueryWrapper<BoardGameLog>()
                .eq("user_id", userId)
                .eq("status", "playing")
                .orderByDesc("id")
                .last("LIMIT 1"));
    }

    /** 对局 -> 前端展示视图 */
    private Map<String, Object> gameView(BoardGameLog g) {
        if (g == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("status", g.getStatus());
        m.put("position", g.getPosition());
        m.put("rolls", g.getRolls());
        m.put("dice_log", g.getDiceLog());
        m.put("gain_copper", g.getGainCopper());
        m.put("gain_power", g.getGainPower());
        m.put("pass", g.getPass());
        m.put("created_at", g.getCreatedAt() == null ? null : g.getCreatedAt().toString());
        m.put("finished_at", g.getFinishedAt() == null ? null : g.getFinishedAt().toString());
        return m;
    }

    /** 用户资产摘要 */
    private Map<String, Object> assetView(UserCultivation uc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("copper", uc.getCopper());
        m.put("power", uc.getPower());
        m.put("realm", uc.getRealm());
        return m;
    }

    /* ================= 对外接口 ================= */

    @Override
    public Map<String, Object> status(AvatarFoundation af) {
        UserCultivation uc = getOrInit(af.getId());
        int used = todayCount(af.getId());
        List<Map<String, Object>> recent = new ArrayList<>();
        for (BoardGameLog g : gameMapper.selectList(new QueryWrapper<BoardGameLog>()
                .eq("user_id", af.getId()).eq("status", "finished")
                .orderByDesc("id").last("LIMIT 10"))) {
            recent.add(gameView(g));
        }
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
        return m;
    }

    /** 成就徽章：按累计登顶次数 / 总局数解锁 */
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
        m.put("name", name);
        m.put("desc", desc);
        m.put("unlocked", unlocked);
        list.add(m);
    }

    @Override
    @Transactional
    public Map<String, Object> start(AvatarFoundation af) {
        // 幂等：已有进行中对局则直接返回，不重复扣报名费
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
        g.setUserId(af.getId());
        g.setStatus("playing");
        g.setPosition(0);
        g.setRolls(0);
        g.setDiceLog("");
        g.setGainCopper(0);
        g.setGainPower(0);
        g.setPass(0);
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
        if (g == null) {
            throw new BoardGameException("当前无进行中的弈局，请先入局");
        }
        if (g.getRolls() >= BoardGameRules.MAX_ROLLS) {
            throw new BoardGameException("本局掷骰次数已用尽");
        }

        UserCultivation uc = getOrInit(af.getId());

        // 服务端权威掷骰 1~6
        int dice = ThreadLocalRandom.current().nextInt(1, 7);
        int from = g.getPosition() == null ? 0 : g.getPosition();
        int to = Math.min(from + dice, BoardGameRules.BOARD_SIZE - 1);
        BoardGameRules.Cell cell = BoardGameRules.cellAt(to);

        // 结算落格收益（含随机福缘/劫难），返回实际变动量（已保证资产不为负）
        int[] delta = settle(cell, uc);
        int dCopper = delta[0];
        int dPower = delta[1];
        cultivationMapper.updateById(uc);

        // 更新对局记录
        String diceLog = (g.getDiceLog() == null ? "" : g.getDiceLog()) + dice + ",";
        g.setPosition(to);
        g.setRolls((g.getRolls() == null ? 0 : g.getRolls()) + 1);
        g.setDiceLog(diceLog);
        g.setGainCopper((g.getGainCopper() == null ? 0 : g.getGainCopper()) + dCopper);
        g.setGainPower((g.getGainPower() == null ? 0 : g.getGainPower()) + dPower);

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
        r.put("dice", dice);
        r.put("from", from);
        r.put("to", to);
        r.put("cell_name", cell.name);
        r.put("cell_icon", cell.icon);
        r.put("cell_type", cell.type);
        r.put("cell_desc", cell.desc);
        r.put("gain_copper", dCopper);
        r.put("gain_power", dPower);
        r.put("pass", g.getPass());
        r.put("finished", finished);
        r.put("left_rolls", Math.max(0, BoardGameRules.MAX_ROLLS - g.getRolls()));
        r.put("game", gameView(g));
        r.put("asset", assetView(uc));
        return r;
    }

    @Override
    public Map<String, Object> history(AvatarFoundation af) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BoardGameLog g : gameMapper.selectList(new QueryWrapper<BoardGameLog>()
                .eq("user_id", af.getId()).orderByDesc("id").last("LIMIT 20"))) {
            list.add(gameView(g));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("history", list);
        return m;
    }

    @Override
    public Map<String, Object> rank() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("by_wins", gameMapper.rankByWins());
        m.put("by_copper", gameMapper.rankByCopper());
        return m;
    }

    /** 落格结算：返回 {铜币变动, 神尊力变动}；资产扣减不会低于 0 */
    private int[] settle(BoardGameRules.Cell cell, UserCultivation uc) {
        int dCopper = 0;
        int dPower = 0;
        int copper = uc.getCopper() == null ? 0 : uc.getCopper();
        int power = uc.getPower() == null ? 0 : uc.getPower();

        switch (cell.type) {
            case BoardGameRules.T_COPPER:
                dCopper = cell.copper;
                break;
            case BoardGameRules.T_FINISH:   // 终点通关大奖：铜币 + 神尊力双得
            case BoardGameRules.T_BOON:
                dCopper = cell.copper;
                dPower = cell.power;
                break;
            case BoardGameRules.T_POWER:
                dPower = cell.power;
                break;
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
                if (ThreadLocalRandom.current().nextBoolean()) {
                    dCopper = -ThreadLocalRandom.current().nextInt(
                            BoardGameRules.PERIL_COPPER_MIN, BoardGameRules.PERIL_COPPER_MAX + 1);
                } else {
                    dPower = -ThreadLocalRandom.current().nextInt(
                            BoardGameRules.PERIL_POWER_MIN, BoardGameRules.PERIL_POWER_MAX + 1);
                }
                break;
            default:
                break;
        }

        // 应用变动，扣减不低于 0
        int newCopper = Math.max(0, copper + dCopper);
        int newPower = Math.max(0, power + dPower);
        dCopper = newCopper - copper;   // 实际变动（若触底则绝对值变小）
        dPower = newPower - power;
        uc.setCopper(newCopper);
        uc.setPower(newPower);
        return new int[]{dCopper, dPower};
    }

    /** 桌游业务异常（由全局异常处理器转为 {ok:false,msg}） */
    public static class BoardGameException extends RuntimeException {
        public BoardGameException(String msg) { super(msg); }
    }
}
