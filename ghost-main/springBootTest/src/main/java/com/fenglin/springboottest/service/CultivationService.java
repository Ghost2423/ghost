package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.AvatarFoundation;

import java.util.List;
import java.util.Map;

/**
 * 沧元图 · 修炼系统服务
 *
 * - status       修炼状态总览（境界/神尊力/铜币/称号/秘境次数/功法阁/境界阶梯）
 * - secret       进入秘境修炼（耗铜币，得神尊力，每日 20 次）
 * - spar         切磋（双方各得神尊力与铜币）
 * - buy          购买功法（耗铜币，得神尊力，永久持有）
 * - breakthrough 突破（耗神尊力，晋升下一境界，可获称号）
 * - viewCard    首页卡片首次浏览（每张每账号仅一次，+50 铜币）
 * - checkIn     每日签到（每天一次，+50 铜币，连续 3/7 天有额外奖励）
 */
public interface CultivationService {

    /**
     * 幂等确保账号已有修炼根基：已存在则绝不改动（完整保留玩家进度），
     * 不存在才写入初始值（筑基 / 0 神尊力 / 200 铜币）。
     * 注册与登录时各调用一次，避免新账号或历史账号缺失修炼数据而出现「进度被重置 / 看不到」。
     */
    void ensureInitialized(Long userId);

    /** 修炼状态总览 */
    Map<String, Object> status(AvatarFoundation af);

    /** 进入秘境修炼 */
    Map<String, Object> secret(AvatarFoundation af);

    /** 切磋 */
    Map<String, Object> spar(AvatarFoundation af, String targetName);

    /** 购买功法 */
    Map<String, Object> buy(AvatarFoundation af, String gongfaName);

    /** 突破境界 */
    Map<String, Object> breakthrough(AvatarFoundation af);

    /** 首页卡片首次浏览奖励（每张每账号仅一次） */
    Map<String, Object> viewCard(AvatarFoundation af, String cardKey, String cardName);

    /** 某账号已浏览过的卡片标识列表（供首页标记「已阅」） */
    List<String> viewedCards(Long userId);

    /** 每日签到（每账号每天一次，奖励铜币） */
    Map<String, Object> checkIn(AvatarFoundation af);
}
