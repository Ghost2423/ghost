package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.AdminAuth;
import com.fenglin.springboottest.common.ApiResult;
import com.fenglin.springboottest.dto.ForumRequest;
import com.fenglin.springboottest.entity.AdminLog;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.mapper.AdminLogMapper;
import com.fenglin.springboottest.service.ForumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 沧元图 · 后台「论道管理」API（全部校验管理员凭证）
 *
 *   POST /api/admin/forum/list          帖子列表（含已隐藏）{keyword, page, page_size}
 *   POST /api/admin/forum/update        置顶 / 屏蔽 / 恢复 {id, pinned, status}
 *   POST /api/admin/forum/replies       查看某帖全部回帖 {post_id}
 *   POST /api/admin/forum/reply/delete 隐藏回帖 {reply_id}
 */
@RestController
@RequestMapping("/api/admin/forum")
public class ForumAdminController {

    @Autowired
    private ForumService forumService;
    @Autowired
    private AdminAuth adminAuth;
    @Autowired
    private AdminLogMapper logMapper;

    private static ResponseEntity<Map<String, Object>> denied() {
        return ResponseEntity.status(403).body(ApiResult.fail("无权管理论道坛：仅管理员可操作"));
    }

    private static int intOf(Integer v, int def) { return v == null ? def : v; }

    private void log(AvatarFoundation admin, String action, String targetId, String detail) {
        AdminLog l = new AdminLog();
        l.setAdminId(admin.getId() == null ? null : admin.getId().longValue());
        l.setAdminName(admin.getDeityName());
        l.setAction(action);
        l.setTargetType("论道帖");
        l.setTargetId(targetId);
        l.setDetail(detail);
        l.setCreatedAt(LocalDateTime.now());
        logMapper.insert(l);
    }

    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestBody ForumRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) return denied();
        return ResponseEntity.ok(forumService.adminList(
                req.getKeyword(), intOf(req.getPage(), 1), intOf(req.getPageSize(), 20)));
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@RequestBody ForumRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) return denied();
        if (req.getId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 id"));
        }
        boolean ok = forumService.adminUpdate(req.getId(), req.getPinned(), req.getStatus());
        if (!ok) return ResponseEntity.status(404).body(ApiResult.fail("未寻得此论题"));
        log(admin, "论道帖调整", String.valueOf(req.getId()),
                "置顶=" + req.getPinned() + " 状态=" + req.getStatus());
        return ResponseEntity.ok(ApiResult.ok("updated", true));
    }

    @PostMapping("/replies")
    public ResponseEntity<Map<String, Object>> replies(@RequestBody ForumRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) return denied();
        if (req.getPostId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 post_id"));
        }
        return ResponseEntity.ok(ApiResult.ok("items", forumService.adminReplies(req.getPostId())));
    }

    @PostMapping("/reply/delete")
    public ResponseEntity<Map<String, Object>> deleteReply(@RequestBody ForumRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) return denied();
        if (req.getReplyId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 reply_id"));
        }
        boolean ok = forumService.adminHideReply(req.getReplyId());
        if (!ok) return ResponseEntity.status(404).body(ApiResult.fail("未寻得此回帖"));
        log(admin, "回帖隐藏", String.valueOf(req.getReplyId()), "回帖已隐藏");
        return ResponseEntity.ok(ApiResult.ok("deleted", true));
    }
}
