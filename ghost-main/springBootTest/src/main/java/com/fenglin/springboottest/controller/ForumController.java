package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResult;
import com.fenglin.springboottest.dto.ForumRequest;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.entity.ForumPost;
import com.fenglin.springboottest.entity.ForumReply;
import com.fenglin.springboottest.service.AvatarFoundationService;
import com.fenglin.springboottest.service.ForumService;
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
 * 沧元图 · 论道论坛 API
 *
 * 读（游客也可浏览）
 *   POST /api/forum/posts         帖子列表 {category, keyword, sort, page, page_size, name(可选，用于标记已共鸣)}
 *   GET  /api/forum/post?id=      帖子详情（含回帖，阅卷数 +1）
 *   GET  /api/forum/stats         论坛总览统计
 *
 * 写（需 name + spell 凭证，与登录/修炼系统一致）
 *   POST /api/forum/post/create   开坛立论 {name, spell, category, title, content}
 *   POST /api/forum/reply/create 回帖      {name, spell, post_id, content}
 *   POST /api/forum/like/toggle  共鸣切换  {name, spell, post_id}
 *   POST /api/forum/post/delete  撤下论题  {name, spell, post_id}
 */
@RestController
@RequestMapping("/api/forum")
public class ForumController {

    @Autowired
    private ForumService forumService;
    @Autowired
    private AvatarFoundationService avatarService;

    /** 凭证校验：账号存在且召唤语相符，返回用户实体，否则 null */
    private AvatarFoundation requireUser(String name, String spell) {
        if (name == null || name.isBlank() || spell == null) return null;
        AvatarFoundation af = avatarService.getByDeityName(name.trim());
        if (af == null || !spell.equals(af.getSpell())) return null;
        return af;
    }

    private static ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(401).body(ApiResult.fail("道友凭证已失效，请重新觉醒神尊根基"));
    }

    private static int intOf(Integer v, int def) { return v == null ? def : v; }

    /* ============================ 读 ============================ */

    @PostMapping("/posts")
    public ResponseEntity<Map<String, Object>> posts(@RequestBody ForumRequest req) {
        Map<String, Object> r = forumService.listPosts(
                req.getCategory(), req.getKeyword(), req.getSort(),
                intOf(req.getPage(), 1), intOf(req.getPageSize(), 10), req.getName());
        return ResponseEntity.ok(r);
    }

    @GetMapping("/post")
    public ResponseEntity<Map<String, Object>> post(@RequestParam("id") long id,
                                                    @RequestParam(value = "viewer", required = false) String viewer) {
        return ResponseEntity.ok(forumService.detail(id, viewer));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(forumService.stats());
    }

    /* ============================ 写 ============================ */

    @PostMapping("/post/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody ForumRequest req) {
        AvatarFoundation user = requireUser(req.getName(), req.getSpell());
        if (user == null) return unauthorized();
        ForumPost p = forumService.createPost(user, req.getCategory(), req.getTitle(), req.getContent());
        Map<String, Object> r = ApiResult.ok("post", p);
        r.put("id", p.getId());
        return ResponseEntity.ok(r);
    }

    @PostMapping("/reply/create")
    public ResponseEntity<Map<String, Object>> reply(@RequestBody ForumRequest req) {
        AvatarFoundation user = requireUser(req.getName(), req.getSpell());
        if (user == null) return unauthorized();
        if (req.getPostId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 post_id"));
        }
        ForumReply r = forumService.createReply(user, req.getPostId(), req.getContent());
        return ResponseEntity.ok(ApiResult.ok("reply", r));
    }

    @PostMapping("/like/toggle")
    public ResponseEntity<Map<String, Object>> toggleLike(@RequestBody ForumRequest req) {
        AvatarFoundation user = requireUser(req.getName(), req.getSpell());
        if (user == null) return unauthorized();
        if (req.getPostId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 post_id"));
        }
        boolean liked = forumService.toggleLike(user, req.getPostId());
        return ResponseEntity.ok(ApiResult.ok("liked", liked));
    }

    @PostMapping("/post/delete")
    public ResponseEntity<Map<String, Object>> delete(@RequestBody ForumRequest req) {
        AvatarFoundation user = requireUser(req.getName(), req.getSpell());
        if (user == null) return unauthorized();
        if (req.getPostId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 post_id"));
        }
        boolean ok = forumService.hidePost(user, req.getPostId());
        if (!ok) return ResponseEntity.status(404).body(ApiResult.fail("未寻得此论题"));
        return ResponseEntity.ok(ApiResult.ok("deleted", true));
    }
}
