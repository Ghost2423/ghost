package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResult;
import com.fenglin.springboottest.dto.BoardGameRequest;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.service.AvatarFoundationService;
import com.fenglin.springboottest.service.BoardGameService;
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
 * 沧元图 · 太虚弈局（桌游）API
 *
 * - GET  /api/boardgame/status?name=   桌游状态总览（棋盘/报名费/每日次数/当前对局/战绩）
 * - POST /api/boardgame/start  {name, spell}  开局（扣 30 铜币报名费）
 * - POST /api/boardgame/roll   {name, spell}  掷骰一步（服务端权威结算，收益入库）
 * - GET  /api/boardgame/history?name=  历史战绩
 * - GET  /api/boardgame/rank           排行榜（登顶榜 / 财富榜 / PVP名将榜）
 * - GET  /api/boardgame/items?name=    道具背包与图鉴
 * - POST /api/boardgame/use-item       {name,spell,item_key,match_id}  使用道具
 * - GET  /api/boardgame/pvp/status?name= PVP 状态总览
 * - GET  /api/boardgame/pvp/list?name=   可应战挑战列表
 * - POST /api/boardgame/pvp/create     {name,spell,opponent_name,versus_ai}  发起挑战
 * - POST /api/boardgame/pvp/accept     {name,spell,match_id}  应战
 * - POST /api/boardgame/pvp/roll       {name,spell,match_id}  PVP 掷骰
 *
 * 写操作以 name+spell 为凭证（与登录/修炼一致，明文比对）。
 */
@RestController
@RequestMapping("/api/boardgame")
public class BoardGameController {

    @Autowired
    private BoardGameService boardGameService;

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

    /** 桌游状态总览（读接口，按名号查询） */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestParam("name") String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 name"));
        }
        AvatarFoundation af = avatarService.getByDeityName(name.trim());
        if (af == null) {
            return ResponseEntity.status(404).body(ApiResult.fail("未寻得此神尊根基"));
        }
        return ResponseEntity.ok(ApiResult.ok("boardgame", boardGameService.status(af)));
    }

    /** 历史战绩（读接口） */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> history(@RequestParam("name") String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 name"));
        }
        AvatarFoundation af = avatarService.getByDeityName(name.trim());
        if (af == null) {
            return ResponseEntity.status(404).body(ApiResult.fail("未寻得此神尊根基"));
        }
        return ResponseEntity.ok(wrap(boardGameService.history(af)));
    }

    /** 排行榜（读接口，无需凭证） */
    @GetMapping("/rank")
    public ResponseEntity<Map<String, Object>> rank() {
        return ResponseEntity.ok(wrap(boardGameService.rank()));
    }

    /** 开局 */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestBody BoardGameRequest req) {
        AvatarFoundation af = requireUser(req.getName(), req.getSpell());
        if (af == null) {
            return ResponseEntity.status(401).body(ApiResult.fail("凭证不符，神尊未应"));
        }
        return ResponseEntity.ok(wrap(boardGameService.start(af)));
    }

    /** 掷骰 */
    @PostMapping("/roll")
    public ResponseEntity<Map<String, Object>> roll(@RequestBody BoardGameRequest req) {
        AvatarFoundation af = requireUser(req.getName(), req.getSpell());
        if (af == null) {
            return ResponseEntity.status(401).body(ApiResult.fail("凭证不符，神尊未应"));
        }
        return ResponseEntity.ok(wrap(boardGameService.roll(af)));
    }

    /* ================= 道具 ================= */

    /** 道具背包（读接口） */
    @GetMapping("/items")
    public ResponseEntity<Map<String, Object>> items(@RequestParam("name") String name) {
        AvatarFoundation af = avatarService.getByDeityName(name == null ? "" : name.trim());
        if (af == null) return ResponseEntity.status(404).body(ApiResult.fail("未寻得此神尊根基"));
        return ResponseEntity.ok(wrap(boardGameService.items(af)));
    }

    /** 使用道具 */
    @PostMapping("/use-item")
    public ResponseEntity<Map<String, Object>> useItem(@RequestBody BoardGameRequest req) {
        AvatarFoundation af = requireUser(req.getName(), req.getSpell());
        if (af == null) return ResponseEntity.status(401).body(ApiResult.fail("凭证不符，神尊未应"));
        return ResponseEntity.ok(wrap(boardGameService.useItem(af, req.getItemKey(), req.getMatchId())));
    }

    /* ================= PVP 擂台 ================= */

    /** PVP 状态总览（读接口） */
    @GetMapping("/pvp/status")
    public ResponseEntity<Map<String, Object>> pvpStatus(@RequestParam("name") String name) {
        AvatarFoundation af = avatarService.getByDeityName(name == null ? "" : name.trim());
        if (af == null) return ResponseEntity.status(404).body(ApiResult.fail("未寻得此神尊根基"));
        return ResponseEntity.ok(wrap(boardGameService.pvpStatus(af)));
    }

    /** 可应战列表（读接口） */
    @GetMapping("/pvp/list")
    public ResponseEntity<Map<String, Object>> pvpList(@RequestParam("name") String name) {
        AvatarFoundation af = avatarService.getByDeityName(name == null ? "" : name.trim());
        if (af == null) return ResponseEntity.status(404).body(ApiResult.fail("未寻得此神尊根基"));
        return ResponseEntity.ok(wrap(boardGameService.pvpList(af)));
    }

    /** 发起挑战 */
    @PostMapping("/pvp/create")
    public ResponseEntity<Map<String, Object>> pvpCreate(@RequestBody BoardGameRequest req) {
        AvatarFoundation af = requireUser(req.getName(), req.getSpell());
        if (af == null) return ResponseEntity.status(401).body(ApiResult.fail("凭证不符，神尊未应"));
        return ResponseEntity.ok(wrap(boardGameService.pvpCreate(af, req.getOpponentName(),
                Boolean.TRUE.equals(req.getVersusAi()))));
    }

    /** 应战 */
    @PostMapping("/pvp/accept")
    public ResponseEntity<Map<String, Object>> pvpAccept(@RequestBody BoardGameRequest req) {
        AvatarFoundation af = requireUser(req.getName(), req.getSpell());
        if (af == null) return ResponseEntity.status(401).body(ApiResult.fail("凭证不符，神尊未应"));
        return ResponseEntity.ok(wrap(boardGameService.pvpAccept(af, req.getMatchId())));
    }

    /** PVP 掷骰 */
    @PostMapping("/pvp/roll")
    public ResponseEntity<Map<String, Object>> pvpRoll(@RequestBody BoardGameRequest req) {
        AvatarFoundation af = requireUser(req.getName(), req.getSpell());
        if (af == null) return ResponseEntity.status(401).body(ApiResult.fail("凭证不符，神尊未应"));
        return ResponseEntity.ok(wrap(boardGameService.pvpRoll(af, req.getMatchId())));
    }

    private static Map<String, Object> wrap(Map<String, Object> result) {
        Map<String, Object> r = ApiResult.ok();
        r.putAll(result);
        return r;
    }
}
