package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.AdminAuth;
import com.fenglin.springboottest.common.ApiResult;
import com.fenglin.springboottest.dto.AdminDataRequest;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.service.AdminDataSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 沧元图 · 后台「数据编辑」API（全部校验管理员凭证，全部写数据库并写操作日志）
 *
 *   POST /api/admin/user/update          编辑用户资料（账号/密码/地域/名号/故事/权限）
 *   POST /api/admin/user/batch-update    批量编辑账号资料（全部账号，非身份字段）
 *   POST /api/admin/cultivation/list     修炼数据列表（全部账号境界/神尊力/铜币/称号）
 *   POST /api/admin/cultivation/update   编辑修炼数据（target_all=true 改全部，否则按 target_id 改单个）
 *   POST /api/admin/register/update      编辑注册记录，并回写关联用户与论坛作者
 *   POST /api/admin/forum/edit           编辑论题（板块/标题/正文）
 *   POST /api/admin/forum/reply/edit     编辑回帖正文
 *
 * 每次编辑不只改主表，还会由 AdminDataSyncService 同步数据库中保存同一份数据的
 * 其它表（注册留痕、论坛作者、共鸣记录、操作日志等），全部在同一事务内完成。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminDataController {

    @Autowired private AdminAuth adminAuth;
    @Autowired private AdminDataSyncService syncService;

    /** 修炼数据列表：全部账号的境界/神尊力/铜币/称号，供后台表格展示 */
    @PostMapping("/cultivation/list")
    public ResponseEntity<Map<String, Object>> cultivationList(@RequestBody AdminDataRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) return denied("查看修炼数据");
        Map<String, Object> r = ApiResult.ok();
        r.put("list", syncService.listCultivations());
        return ResponseEntity.ok(r);
    }

    /**
     * 编辑修炼数据：target_all=true 作用于全部账号，否则按 target_id 修改单个账号。
     * 支持字段：realm / power / copper / title；境界变更未给 title 时按规则自动授予称号。
     */
    @PostMapping("/cultivation/update")
    public ResponseEntity<Map<String, Object>> updateCultivation(@RequestBody AdminDataRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) return denied("修改修炼数据");
        boolean all = Boolean.TRUE.equals(req.getTargetAll());
        if (!all && req.getTargetId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 target_id"));
        }
        if (req.getItem() == null || req.getItem().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 item 数据"));
        }
        try {
            int n = syncService.updateCultivation(req.getTargetId(), all, req.getItem());
            String scope = all ? "全部账号" : ("账号 " + req.getTargetId());
            syncService.log(admin, "修改", "修炼数据", all ? "ALL" : String.valueOf(req.getTargetId()),
                    scope + " · " + req.getItem() + " · 影响 " + n + " 人");
            Map<String, Object> r = ApiResult.ok();
            r.put("updated", n);
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResult.fail(e.getMessage()));
        }
    }

    /**
     * 批量编辑账号资料（全部账号）：仅支持地域 / 权限 / 心魔誓约 / 神话简介等非身份字段，
     * 避免账号名/密码/名号批量改成相同值。每个账号同步回写注册留痕。
     */
    @PostMapping("/user/batch-update")
    public ResponseEntity<Map<String, Object>> batchUpdateUsers(@RequestBody AdminDataRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) return denied("批量编辑账号资料");
        if (req.getItem() == null || req.getItem().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 item 数据"));
        }
        try {
            int n = syncService.updateUsersBatch(req.getItem());
            syncService.log(admin, "修改", "用户", "ALL", "批量资料 " + req.getItem() + " · 影响 " + n + " 人");
            Map<String, Object> r = ApiResult.ok();
            r.put("updated", n);
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResult.fail(e.getMessage()));
        }
    }

    /** 编辑用户资料 */
    @PostMapping("/user/update")
    public ResponseEntity<Map<String, Object>> updateUser(@RequestBody AdminDataRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) return denied("编辑用户资料");
        if (req.getTargetId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 target_id"));
        }
        if (req.getItem() == null || req.getItem().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 item 数据"));
        }
        try {
            AvatarFoundation updated = syncService.updateUser(req.getTargetId(), req.getItem());
            syncService.log(admin, "修改", "用户", String.valueOf(updated.getId()),
                    updated.getDeityName() + " · 资料与关联数据已同步");
            Map<String, Object> r = ApiResult.ok();
            r.put("user", toUserItem(updated));
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResult.fail(e.getMessage()));
        }
    }

    /** 编辑注册记录（同步回写现存用户与论坛作者） */
    @PostMapping("/register/update")
    public ResponseEntity<Map<String, Object>> updateRegister(@RequestBody AdminDataRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) return denied("编辑注册记录");
        if (req.getId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 id"));
        }
        if (req.getItem() == null || req.getItem().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 item 数据"));
        }
        try {
            var record = syncService.updateRegisterRecord(req.getId(), req.getItem());
            syncService.log(admin, "修改", "注册记录", String.valueOf(record.getId()),
                    record.getAccount() + " · 已同步用户主表与论坛作者");
            Map<String, Object> r = ApiResult.ok();
            r.put("record", record);
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResult.fail(e.getMessage()));
        }
    }

    /** 编辑论题 */
    @PostMapping("/forum/edit")
    public ResponseEntity<Map<String, Object>> editPost(@RequestBody AdminDataRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) return denied("编辑论题");
        if (req.getId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 id"));
        }
        if (req.getItem() == null || req.getItem().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 item 数据"));
        }
        try {
            var post = syncService.updateForumPost(req.getId(), req.getItem());
            syncService.log(admin, "修改", "论道帖", String.valueOf(post.getId()),
                    post.getCategory() + " · " + post.getTitle());
            Map<String, Object> r = ApiResult.ok();
            r.put("post", post);
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResult.fail(e.getMessage()));
        }
    }

    /** 编辑回帖正文 */
    @PostMapping("/forum/reply/edit")
    public ResponseEntity<Map<String, Object>> editReply(@RequestBody AdminDataRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) return denied("编辑回帖");
        if (req.getReplyId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 reply_id"));
        }
        if (req.getItem() == null || req.getItem().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 item 数据"));
        }
        try {
            var reply = syncService.updateForumReply(req.getReplyId(), req.getItem());
            syncService.log(admin, "修改", "论道回帖", String.valueOf(reply.getId()),
                    reply.getAuthorName() + " · " + reply.getFloorNo() + " 楼");
            Map<String, Object> r = ApiResult.ok();
            r.put("reply", reply);
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResult.fail(e.getMessage()));
        }
    }

    private static ResponseEntity<Map<String, Object>> denied(String action) {
        return ResponseEntity.status(403).body(ApiResult.fail("无权" + action + "：仅管理员可操作"));
    }

    /** 用户实体 → 后台表格行（与 /api/admin/users 输出形状一致） */
    private static Map<String, Object> toUserItem(AvatarFoundation af) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", af.getId());
        m.put("deity_name", af.getDeityName());
        m.put("region", af.getRegion());
        m.put("myth_name", af.getMythName());
        m.put("myth_story", af.getMythStory());
        m.put("spell", af.getSpell());
        m.put("permission", af.getPermission());
        m.put("agreed", af.getAgreed());
        m.put("created_at", af.getCreatedAt() == null ? null : af.getCreatedAt().toString());
        return m;
    }
}
