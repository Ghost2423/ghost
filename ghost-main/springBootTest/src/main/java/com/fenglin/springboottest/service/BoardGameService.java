package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.AvatarFoundation;

import java.util.Map;

/**
 * 沧元图 · 太虚弈局（桌游）服务
 *
 * - status  桌游状态总览（棋盘 / 报名费 / 每日次数 / 当前进行中对局 / 历史战绩）
 * - start   开局（扣报名费，建对局记录）
 * - roll    掷骰一步（服务端权威随机，落格结算，收益真实写入 user_cultivation）
 * - history 历史战绩
 */
public interface BoardGameService {

    /** 桌游状态总览 */
    Map<String, Object> status(AvatarFoundation af);

    /** 开局 */
    Map<String, Object> start(AvatarFoundation af);

    /** 掷骰一步 */
    Map<String, Object> roll(AvatarFoundation af);

    /** 历史战绩 */
    Map<String, Object> history(AvatarFoundation af);

    /** 排行榜（登顶榜 + 财富榜） */
    Map<String, Object> rank();
}
