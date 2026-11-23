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
    /** 飞行棋掷骰行棋（planeIndex 为本次操作的飞机 0..3，null 则自动选） */
    Map<String, Object> roll(AvatarFoundation af, Integer planeIndex);
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
    /** PVP 飞行棋掷骰（planeIndex 为本次操作的飞机，null 自动选；天道AI自动代掷） */
    Map<String, Object> pvpRoll(AvatarFoundation af, Long matchId, Integer planeIndex);
    /** 可应战的挑战列表 */
    Map<String, Object> pvpList(AvatarFoundation af);
}
