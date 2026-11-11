package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResult;
import com.fenglin.springboottest.dto.ForgeRequest;
import com.fenglin.springboottest.dto.LoginRequest;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.service.AvatarFoundationService;
import com.fenglin.springboottest.service.RegisterRecordService;
import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private RegisterRecordService registerRecordService;

    /** 锻造神尊根基（注册）：同一事务写入 avatar_foundation + user_register_record */
    @PostMapping("/forge")
    public ResponseEntity<Map<String, Object>> forge(@RequestBody ForgeRequest req,
                                                     HttpServletRequest request) {
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
        // 注册用户一律为默认权限（1=默认用户）；管理员权限(0)仅可由后台指定
        af.setPermission(1);

        // 两表同事务落库：神尊根基 + 每次注册的账号留痕
        registerRecordService.saveRegistration(af, clientIp(request));

        Map<String, Object> result = ApiResult.ok("id", af.getId());
        result.put("register_record", true); // 明确告知前端：账号已单独存入注册表
        return ResponseEntity.ok(result);
    }

    /** 取注册来源 IP（兼容反向代理的 X-Forwarded-For） */
    private static String clientIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        return ip == null ? null : (ip.length() > 64 ? ip.substring(0, 64) : ip);
    }

    /**
     * 查询神尊根基（登录页按名号查询）。
     * 注意：口令（spell）在库中按需求明文存储，但对外查询接口不下发口令，
     * 仅在 POST /api/login 内部做比对，避免任何人凭名号就能拉取明文密码。
     */
    @GetMapping("/avatar")
    public ResponseEntity<Map<String, Object>> avatar(@RequestParam("name") String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 name"));
        }
        AvatarFoundation af = avatarService.getByDeityName(name);
        if (af == null) {
            return ResponseEntity.status(404).body(ApiResult.fail("未寻得此神尊根基"));
        }
        af.setSpell(null);
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
