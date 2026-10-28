package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResult;
import com.fenglin.springboottest.dto.AdminRequest;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.service.AvatarFoundationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 沧元界 · 后台管理系统 API（仅管理员可用）
 *
 * 权限规则：用户表 permission 字段 —— 1=默认用户（注册默认），0=管理员。
 * 只有 permission=0 的用户才能登录后台管理系统；所有管理接口均校验操作者必须为管理员。
 *
 * - POST /api/admin/login       管理员登录（校验账号密码 + 权限必须为 0）
 * - POST /api/admin/users       用户列表（全量，含权限/密码等）
 * - POST /api/admin/permission  变更用户权限（0=设为管理员，1=恢复默认）
 * - POST /api/admin/delete      删除用户
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /** 默认用户权限 */
    private static final int PERM_USER = 1;
    /** 管理员权限 */
    private static final int PERM_ADMIN = 0;

    @Autowired
    private AvatarFoundationService avatarService;

    /**
     * 管理员凭证校验：账号存在、密码（明文比对）、权限为 0（管理员）。
     * @return 校验通过返回该管理员实体，否则返回 null
     */
    private AvatarFoundation requireAdmin(String name, String spell) {
        if (name == null || name.isBlank() || spell == null) {
            return null;
        }
        AvatarFoundation af = avatarService.getByDeityName(name.trim());
        if (af == null) {
            return null;
        }
        // 密码（召唤语）明文比对
        if (!spell.equals(af.getSpell())) {
            return null;
        }
        // 权限为 1（默认用户）则无法进入后台管理系统
        if (af.getPermission() == null || af.getPermission() != PERM_ADMIN) {
            return null;
        }
        return af;
    }

    /** 管理员登录 */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AdminRequest req) {
        String name = req.getName() == null ? "" : req.getName().trim();
        String spell = req.getSpell() == null ? "" : req.getSpell();

        if (name.isEmpty() || spell.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("请输入账号与密码"));
        }

        AvatarFoundation af = avatarService.getByDeityName(name);
        if (af == null) {
            return ResponseEntity.status(404).body(ApiResult.fail("未寻得根基，请先前往锻造神尊根基"));
        }
        if (!spell.equals(af.getSpell())) {
            return ResponseEntity.status(401).body(ApiResult.fail("密码不符，神尊未应"));
        }
        if (af.getPermission() == null || af.getPermission() != PERM_ADMIN) {
            // 权限为 1（默认用户）：拒绝登录后台管理系统
            return ResponseEntity.status(403).body(ApiResult.fail("权限不足：默认用户无法进入后台管理系统"));
        }
        return ResponseEntity.ok(ApiResult.ok("admin", af));
    }

    /** 用户列表（需管理员凭证） */
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> users(@RequestBody AdminRequest req) {
        AvatarFoundation admin = requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return ResponseEntity.status(403).body(ApiResult.fail("无权访问：仅管理员可查看用户列表"));
        }
        List<Map<String, Object>> users = avatarService.list().stream()
                .sorted((a, b) -> Long.compare(
                        b.getId() == null ? 0 : b.getId(),
                        a.getId() == null ? 0 : a.getId()))
                .map(AdminController::toItem)
                .toList();
        Map<String, Object> r = ApiResult.ok();
        r.put("users", users);
        return ResponseEntity.ok(r);
    }

    /** 变更用户权限（需管理员凭证；不可操作自己，避免唯一管理员自降） */
    @PostMapping("/permission")
    public ResponseEntity<Map<String, Object>> permission(@RequestBody AdminRequest req) {
        AvatarFoundation admin = requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return ResponseEntity.status(403).body(ApiResult.fail("无权操作：仅管理员可变更权限"));
        }
        if (req.getTargetId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 target_id"));
        }
        Integer target = req.getPermission();
        if (target == null || (target != PERM_ADMIN && target != PERM_USER)) {
            return ResponseEntity.badRequest().body(ApiResult.fail("permission 仅允许 0（管理员）或 1（默认用户）"));
        }
        if (req.getTargetId().equals(admin.getId())) {
            return ResponseEntity.status(400).body(ApiResult.fail("不能变更自己的权限"));
        }
        AvatarFoundation af = avatarService.getById(req.getTargetId());
        if (af == null) {
            return ResponseEntity.status(404).body(ApiResult.fail("目标用户不存在"));
        }
        af.setPermission(target);
        avatarService.updateById(af);
        return ResponseEntity.ok(ApiResult.ok());
    }

    /** 删除用户（需管理员凭证；不可删除自己） */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> delete(@RequestBody AdminRequest req) {
        AvatarFoundation admin = requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return ResponseEntity.status(403).body(ApiResult.fail("无权操作：仅管理员可删除用户"));
        }
        if (req.getTargetId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 target_id"));
        }
        if (req.getTargetId().equals(admin.getId())) {
            return ResponseEntity.status(400).body(ApiResult.fail("不能删除自己"));
        }
        AvatarFoundation af = avatarService.getById(req.getTargetId());
        if (af == null) {
            return ResponseEntity.status(404).body(ApiResult.fail("目标用户不存在"));
        }
        avatarService.removeById(req.getTargetId());
        return ResponseEntity.ok(ApiResult.ok());
    }

    /** 实体 → 后台表格行 */
    private static Map<String, Object> toItem(AvatarFoundation af) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", af.getId());
        m.put("deity_name", af.getDeityName());
        m.put("region", af.getRegion());
        m.put("myth_name", af.getMythName());
        m.put("spell", af.getSpell());
        m.put("permission", af.getPermission() == null ? PERM_USER : af.getPermission());
        m.put("agreed", af.getAgreed());
        m.put("created_at", af.getCreatedAt() == null ? null : af.getCreatedAt().toString());
        return m;
    }
}
