package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.common.CultivationRules;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.entity.CodexEntry;
import com.fenglin.springboottest.entity.SparLog;
import com.fenglin.springboottest.entity.UserCultivation;
import com.fenglin.springboottest.entity.UserGongfa;
import com.fenglin.springboottest.mapper.SparLogMapper;
import com.fenglin.springboottest.mapper.UserCultivationMapper;
import com.fenglin.springboottest.mapper.UserGongfaMapper;
import com.fenglin.springboottest.service.AvatarFoundationService;
import com.fenglin.springboottest.service.CodexService;
import com.fenglin.springboottest.service.CultivationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 修炼系统服务实现。
 * 所有数值规则（境界阶梯 / 突破消耗 / 称号 / 秘境与切磋收益）统一取自 {@link CultivationRules}。
 */
@Service
public class CultivationServiceImpl implements CultivationService {

    @Autowired
    private UserCultivationMapper cultivationMapper;

    @Autowired
    private UserGongfaMapper gongfaMapper;

    @Autowired
    private SparLogMapper sparLogMapper;

    @Autowired
    private AvatarFoundationService avatarService;

    @Autowired
    private CodexService codexService;

    /* ================= 内部工具 ================= */

    /** 取用户修炼状态；无则按默认值（筑基 / 0 神尊力 / 200 铜币）惰性初始化 */
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
        uc.setCopper(CultivationRules.INIT_COPPER);
        uc.setSecretCount(0);
        try {
            cultivationMapper.insert(uc);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发初始化撞唯一键：取已存在行
            uc = cultivationMapper.selectOne(
                    new QueryWrapper<UserCultivation>().eq("user_id", userId));
        }
        return uc;
    }

    /** 当日有效的秘境已用次数（跨日自动按 0 计） */
    private int secretUsedToday(UserCultivation uc) {
        if (uc.getSecretDate() == null || !uc.getSecretDate().equals(LocalDate.now())) {
            return 0;
        }
        return uc.getSecretCount() == null ? 0 : uc.getSecretCount();
    }

    /** 已购功法名集合 */
    private Set<String> ownedGongfa(Long userId) {
        return gongfaMapper.selectList(
                        new QueryWrapper<UserGongfa>().eq("user_id", userId))
                .stream().map(UserGongfa::getGongfaName).collect(Collectors.toSet());
    }

    /** 状态总览（含功法阁与境界阶梯） */
    private Map<String, Object> buildStatus(AvatarFoundation af, UserCultivation uc) {
        Set<String> owned = ownedGongfa(af.getId());
        String realm = uc.getRealm();
        String next = CultivationRules.nextRealm(realm);
        Integer cost = CultivationRules.breakCost(realm);
        int used = secretUsedToday(uc);

        List<Map<String, Object>> shop = new ArrayList<>();
        for (CodexEntry e : codexService.list(new QueryWrapper<CodexEntry>()
                .eq("category", "功法").orderByAsc("price"))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getName());
            m.put("tag", e.getTitle());
            m.put("desc", e.getDescription());
            m.put("price", e.getPrice() == null ? 100 : e.getPrice());
            m.put("power_gain", e.getPowerGain() == null ? 40 : e.getPowerGain());
            m.put("owned", owned.contains(e.getName()));
            shop.add(m);
        }

        Map<String, Object> secret = new LinkedHashMap<>();
        secret.put("used", used);
        secret.put("limit", CultivationRules.SECRET_DAILY_LIMIT);
        secret.put("left", Math.max(0, CultivationRules.SECRET_DAILY_LIMIT - used));
        secret.put("cost", CultivationRules.SECRET_COST_COPPER);
        secret.put("gain", CultivationRules.secretGain(realm));

        Map<String, Object> spar = new LinkedHashMap<>();
        spar.put("gain_power", CultivationRules.SPAR_GAIN_POWER);
        spar.put("gain_copper", CultivationRules.SPAR_GAIN_COPPER);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", af.getDeityName());
        m.put("entry_name", af.getMythName() == null || af.getMythName().isBlank()
                ? af.getDeityName() : af.getMythName());
        m.put("realm", realm);
        m.put("realm_index", CultivationRules.indexOf(realm));
        m.put("stage", CultivationRules.stageOf(realm));
        m.put("title", uc.getTitle());
        m.put("power", uc.getPower());
        m.put("copper", uc.getCopper());
        m.put("next_realm", next);
        m.put("break_cost", cost);
        m.put("apex", CultivationRules.REALM_APEX.equals(realm));
        m.put("secret", secret);
        m.put("spar", spar);
        m.put("owned_gongfa", new ArrayList<>(owned));
        m.put("gongfa_shop", shop);
        m.put("ladder", CultivationRules.ladderView());
        return m;
    }

    /* ================= 对外接口 ================= */

    @Override
    public Map<String, Object> status(AvatarFoundation af) {
        return buildStatus(af, getOrInit(af.getId()));
    }

    @Override
    public Map<String, Object> secret(AvatarFoundation af) {
        UserCultivation uc = getOrInit(af.getId());

        int used = secretUsedToday(uc);
        if (used >= CultivationRules.SECRET_DAILY_LIMIT) {
            throw new CultivationException("今日秘境修炼次数已用尽（每日 "
                    + CultivationRules.SECRET_DAILY_LIMIT + " 次），明日再来");
        }
        if (uc.getCopper() < CultivationRules.SECRET_COST_COPPER) {
            throw new CultivationException("铜币不足：入秘境需 "
                    + CultivationRules.SECRET_COST_COPPER + " 铜币，可通过切磋或先卖力攒钱");
        }

        int gain = CultivationRules.secretGain(uc.getRealm());
        uc.setCopper(uc.getCopper() - CultivationRules.SECRET_COST_COPPER);
        uc.setPower(uc.getPower() + gain);
        uc.setSecretDate(LocalDate.now());
        uc.setSecretCount(used + 1);
        cultivationMapper.updateById(uc);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("gain_power", gain);
        r.put("cost_copper", CultivationRules.SECRET_COST_COPPER);
        r.put("left", CultivationRules.SECRET_DAILY_LIMIT - (used + 1));
        r.put("cultivation", buildStatus(af, uc));
        return r;
    }

    @Override
    @Transactional
    public Map<String, Object> spar(AvatarFoundation af, String targetName) {
        if (targetName == null || targetName.isBlank()) {
            throw new CultivationException("请输入切磋对象的名号");
        }
        String target = targetName.trim();
        if (target.equals(af.getDeityName())) {
            throw new CultivationException("不可与自己切磋");
        }
        AvatarFoundation targetAf = avatarService.getByDeityName(target);
        if (targetAf == null) {
            throw new CultivationException("未寻得此神尊根基，切磋对象须为已注册神尊");
        }

        UserCultivation me = getOrInit(af.getId());
        UserCultivation other = getOrInit(targetAf.getId());

        me.setPower(me.getPower() + CultivationRules.SPAR_GAIN_POWER);
        me.setCopper(me.getCopper() + CultivationRules.SPAR_GAIN_COPPER);
        other.setPower(other.getPower() + CultivationRules.SPAR_GAIN_POWER);
        other.setCopper(other.getCopper() + CultivationRules.SPAR_GAIN_COPPER);
        cultivationMapper.updateById(me);
        cultivationMapper.updateById(other);

        SparLog log = new SparLog();
        log.setInitiatorId(af.getId());
        log.setTargetId(targetAf.getId());
        sparLogMapper.insert(log);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("target", targetAf.getDeityName());
        r.put("gain_power", CultivationRules.SPAR_GAIN_POWER);
        r.put("gain_copper", CultivationRules.SPAR_GAIN_COPPER);
        r.put("cultivation", buildStatus(af, me));
        return r;
    }

    @Override
    public Map<String, Object> buy(AvatarFoundation af, String gongfaName) {
        if (gongfaName == null || gongfaName.isBlank()) {
            throw new CultivationException("请指定欲购功法");
        }
        String name = gongfaName.trim();
        CodexEntry entry = codexService.getOne(new QueryWrapper<CodexEntry>()
                .eq("category", "功法").eq("name", name));
        if (entry == null) {
            throw new CultivationException("元界图鉴中无此功法：" + name);
        }

        UserCultivation uc = getOrInit(af.getId());
        if (ownedGongfa(af.getId()).contains(name)) {
            throw new CultivationException("已修得此功法，不必重复参悟");
        }
        int price = entry.getPrice() == null ? 100 : entry.getPrice();
        if (uc.getCopper() < price) {
            throw new CultivationException("铜币不足：此功法需 " + price + " 铜币，可先切磋攒钱");
        }
        int gain = entry.getPowerGain() == null ? 40 : entry.getPowerGain();

        uc.setCopper(uc.getCopper() - price);
        uc.setPower(uc.getPower() + gain);
        cultivationMapper.updateById(uc);

        UserGongfa ug = new UserGongfa();
        ug.setUserId(af.getId());
        ug.setGongfaName(name);
        try {
            gongfaMapper.insert(ug);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new CultivationException("已修得此功法，不必重复参悟");
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("gongfa", name);
        r.put("gain_power", gain);
        r.put("cost_copper", price);
        r.put("cultivation", buildStatus(af, uc));
        return r;
    }

    @Override
    public Map<String, Object> breakthrough(AvatarFoundation af) {
        UserCultivation uc = getOrInit(af.getId());
        String realm = uc.getRealm();

        if (CultivationRules.REALM_APEX.equals(realm)) {
            throw new CultivationException("已达永恒之巅，沧元界再无更高境界");
        }
        String next = CultivationRules.nextRealm(realm);
        Integer cost = CultivationRules.breakCost(realm);
        if (next == null || cost == null) {
            throw new CultivationException("境界数据异常，无法突破");
        }
        if (uc.getPower() < cost) {
            throw new CultivationException("神尊力不足：突破「" + next + "」需 " + cost
                    + " 神尊力（当前 " + uc.getPower() + "），可入秘境、购功法或切磋获取");
        }

        uc.setPower(uc.getPower() - cost);
        uc.setRealm(next);
        // 称号：到达暗星 → 封侯神尊；到达无间 → 封王神尊（覆盖封侯）
        String title = CultivationRules.REALM_TITLES.get(next);
        if (title != null) {
            uc.setTitle(title);
        }
        cultivationMapper.updateById(uc);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("from", realm);
        r.put("to", next);
        r.put("cost_power", cost);
        r.put("title", uc.getTitle());
        r.put("cultivation", buildStatus(af, uc));
        return r;
    }

    /** 修炼系统业务异常（由全局异常处理器转为 {ok:false,msg}） */
    public static class CultivationException extends RuntimeException {
        public CultivationException(String msg) { super(msg); }
    }
}
