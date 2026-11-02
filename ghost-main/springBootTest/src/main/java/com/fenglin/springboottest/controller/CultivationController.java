package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResult;
import com.fenglin.springboottest.dto.CultivationRequest;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.service.AvatarFoundationService;
import com.fenglin.springboottest.service.CultivationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 沧元图 · 修炼系统 API
 *
 * - GET  /api/cultivation/status?name= 修炼状态总览（境界/神尊力/铜币/秘境/功法阁/阶梯）
 * - POST /api/cultivation/secret       进入秘境修炼（耗 50 铜币得神尊力，每日 20 次）
 * - POST /api/cultivation/spar         切磋 {name, spell, target}（双方各得神尊力与铜币）
 * - POST /api/cultivation/buy          购功法 {name, spell, gongfa}（耗铜币得神尊力，限购一次）
 * - POST /api/cultivation/break        突破 {name, spell}（耗神尊力晋升境界，可获称号）
 *
 * 写操作均以 name+spell 为凭证（与登录/后台管理系统一致，明文比对）。
 */
@RestController
@RequestMapping("/api/cultivation")
public class CultivationController {

    @Autowired
    private CultivationService cultivationService;

    @Autowired
    private AvatarFoundationService avatarService;

    /** 凭证校验：账号存在且召唤语相符，返回用户实体，否则 null */
    private AvatarFoundation requireUser(String name, String spell) {
        if (name == null || name.isBlank() || spell == null) {
            return null;
        }
        AvatarFoundation af = avatarService.getByDeityName(name.trim());
        if (af == null || !spell.equals(af.getSpell())) {
            return null;
        }
        return af;
    }

    /** 修炼状态总览（按名号查询，与 /api/avatar 一致的读接口） */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestParam("name") String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 name"));
        }
        AvatarFoundation af = avatarService.getByDeityName(name.trim());
        if (af == null) {
            return ResponseEntity.status(404).body(ApiResult.fail("未寻得此神尊根基"));
        }
        return ResponseEntity.ok(ApiResult.ok("cultivation", cultivationService.status(af)));
    }

    /** 进入秘境修炼 */
    @PostMapping("/secret")
    public ResponseEntity<Map<String, Object>> secret(@RequestBody CultivationRequest req) {
        AvatarFoundation af = requireUser(req.getName(), req.getSpell());
        if (af == null) {
            return ResponseEntity.status(401).body(ApiResult.fail("凭证不符，神尊未应"));
        }
        return ResponseEntity.ok(wrap(cultivationService.secret(af)));
    }

    /** 切磋 */
    @PostMapping("/spar")
    public ResponseEntity<Map<String, Object>> spar(@RequestBody CultivationRequest req) {
        AvatarFoundation af = requireUser(req.getName(), req.getSpell());
        if (af == null) {
            return ResponseEntity.status(401).body(ApiResult.fail("凭证不符，神尊未应"));
        }
        return ResponseEntity.ok(wrap(cultivationService.spar(af, req.getTarget())));
    }

    /** 购买功法 */
    @PostMapping("/buy")
    public ResponseEntity<Map<String, Object>> buy(@RequestBody CultivationRequest req) {
        AvatarFoundation af = requireUser(req.getName(), req.getSpell());
        if (af == null) {
            return ResponseEntity.status(401).body(ApiResult.fail("凭证不符，神尊未应"));
        }
        return ResponseEntity.ok(wrap(cultivationService.buy(af, req.getGongfa())));
    }

    /** 突破境界 */
    @PostMapping("/break")
    public ResponseEntity<Map<String, Object>> breakthrough(@RequestBody CultivationRequest req) {
        AvatarFoundation af = requireUser(req.getName(), req.getSpell());
        if (af == null) {
            return ResponseEntity.status(401).body(ApiResult.fail("凭证不符，神尊未应"));
        }
        return ResponseEntity.ok(wrap(cultivationService.breakthrough(af)));
    }

    /** 在服务结果之上补 ok:true，保持统一返回结构 */
    private static Map<String, Object> wrap(Map<String, Object> result) {
        Map<String, Object> r = ApiResult.ok();
        r.putAll(result);
        return r;
    }
}
