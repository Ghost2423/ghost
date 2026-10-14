package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResult;
import com.fenglin.springboottest.dto.ForgeRequest;
import com.fenglin.springboottest.dto.LoginRequest;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.service.AvatarFoundationService;
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
 * 沧元界 · 神尊根基 API
 *
 * - POST /api/forge      注册锻造 -> 写入 MySQL cangyuan.avatar_foundation
 * - GET  /api/avatar?name=神尊相  登录校验 -> 返回该神尊根基（召唤语等）
 * - POST /api/login       觉醒（服务端校验召唤语，不回传明文规则）
 */
@RestController
@RequestMapping("/api")
public class AvatarController {

    @Autowired
    private AvatarFoundationService avatarService;

    /** 锻造神尊根基（注册） */
    @PostMapping("/forge")
    public ResponseEntity<Map<String, Object>> forge(@RequestBody ForgeRequest req) {
        String region = trim(req.getRegion());
        String deity = trim(req.getDeityName());
        String spell = trim(req.getSpell());

        if (region.isEmpty() || deity.isEmpty() || spell.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.fail("缺少必填字段 region/deity_name/spell"));
        }

        if (avatarService.getByDeityName(deity) != null) {
            return ResponseEntity.status(409)
                    .body(ApiResult.fail("该神尊相已存在于元界数据库"));
        }

        AvatarFoundation af = new AvatarFoundation();
        af.setRegion(region);
        af.setDeityName(deity);
        af.setMythName(trim(req.getMythName()));
        af.setSpell(spell);
        af.setMythStory(req.getMythStory() == null ? null : req.getMythStory().trim());
        af.setAgreed(req.getAgreed() != null && req.getAgreed() == 1 ? 1 : 0);

        avatarService.save(af);
        return ResponseEntity.ok(ApiResult.ok("id", af.getId()));
    }

    /** 查询神尊根基（登录页按名号查询） */
    @GetMapping("/avatar")
    public ResponseEntity<Map<String, Object>> avatar(@RequestParam("name") String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 name"));
        }
        AvatarFoundation af = avatarService.getByDeityName(name);
        if (af == null) {
            return ResponseEntity.status(404).body(ApiResult.fail("未寻得此神尊根基"));
        }
        return ResponseEntity.ok(ApiResult.ok("avatar", af));
    }

    /** 觉醒（登录）：服务端校验召唤语 */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req) {
        String name = trim(req.getName());
        String spell = req.getSpell() == null ? "" : req.getSpell();

        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("请输入神尊相"));
        }

        AvatarFoundation af = avatarService.getByDeityName(name);
        if (af == null) {
            return ResponseEntity.status(404).body(ApiResult.fail("未寻得根基，请先前往锻造神尊根基"));
        }
        if (!spell.equals(af.getSpell())) {
            return ResponseEntity.status(401).body(ApiResult.fail("召唤语不符，神尊未应"));
        }
        return ResponseEntity.ok(ApiResult.ok("avatar", af));
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
