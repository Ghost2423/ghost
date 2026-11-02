package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.AvatarFoundation;

import java.util.Map;

/**
 * 沧元图 · 修炼系统服务
 *
 * - status       修炼状态总览（境界/神尊力/铜币/称号/秘境次数/功法阁/境界阶梯）
 * - secret       进入秘境修炼（耗铜币，得神尊力，每日 20 次）
 * - spar         切磋（双方各得神尊力与铜币）
 * - buy          购买功法（耗铜币，得神尊力，永久持有）
 * - breakthrough 突破（耗神尊力，晋升下一境界，可获称号）
 */
public interface CultivationService {

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
}
