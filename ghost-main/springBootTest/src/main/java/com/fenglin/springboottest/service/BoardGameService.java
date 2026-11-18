package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.AvatarFoundation;

import java.util.Map;

/**
 * 沧元图 · 太虚弈局（桌游）服务
 *
 * 单人：status / start / roll / history / rank
 * 道具：items 背包 / useItem 使用
 * PVP ：pvpStatus / pvpCreate / pvpAccept / pvpRoll / pvpList / pvpRank
 */
public interface BoardGameService {

    /* ===== 单人弈局 ===== */
    /** 桌游状态总览 */
    Map<String, Object> status(AvatarFoundation af);
    /** 开局 */
    Map<String, Object> start(AvatarFoundation af);
    /** 掷骰一步 */
    Map<String, Object> roll(AvatarFoundation af);
    /** 历史战绩 */
    Map<String, Object> history(AvatarFoundation af);
    /** 排行榜（登顶榜 + 财富榜 + PVP名将榜） */
    Map<String, Object> rank();

    /* ===== 道具 ===== */
    /** 道具背包 + 道具图鉴 */
    Map<String, Object> items(AvatarFoundation af);
    /** 使用道具（作用于当前单人局或PVP局） */
    Map<String, Object> useItem(AvatarFoundation af, String itemKey, Long matchId);

    /* ===== PVP 擂台 ===== */
    /** PVP 状态总览（我的对局 / 可应战列表 / 战绩） */
    Map<String, Object> pvpStatus(AvatarFoundation af);
    /** 发起挑战（opponent 为空或 versusAi=true 则由天道AI接战） */
    Map<String, Object> pvpCreate(AvatarFoundation af, String opponentName, boolean versusAi);
    /** 应战（领取等待中的挑战） */
    Map<String, Object> pvpAccept(AvatarFoundation af, Long matchId);
    /** PVP 掷骰一步（若轮到天道AI，服务端自动代掷至回到玩家或结束） */
    Map<String, Object> pvpRoll(AvatarFoundation af, Long matchId);
    /** 可应战的挑战列表 */
    Map<String, Object> pvpList(AvatarFoundation af);
}
