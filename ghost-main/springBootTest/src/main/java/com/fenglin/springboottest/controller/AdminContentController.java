package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.common.AdminAuth;
import com.fenglin.springboottest.common.ApiResult;
import com.fenglin.springboottest.dto.AdminContentRequest;
import com.fenglin.springboottest.entity.AdminLog;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.entity.CodexEntry;
import com.fenglin.springboottest.entity.HomeNews;
import com.fenglin.springboottest.entity.HomeSection;
import com.fenglin.springboottest.entity.SiteSetting;
import com.fenglin.springboottest.mapper.CodexEntryMapper;
import com.fenglin.springboottest.service.AvatarFoundationService;
import com.fenglin.springboottest.service.HomeContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 沧元图 · 后台管理系统 —— 首页数据管理 API
 *
 * 所有接口均需管理员凭证（name + spell，permission=0），与 /api/admin/users 同一套权限。
 *
 * 图鉴（首页四大界域卡片 + 检索源）
 *   POST /api/admin/codex/list     列表（可按 category / keyword 过滤）
 *   POST /api/admin/codex/save     新增或修改
 *   POST /api/admin/codex/status   上架 / 下架
 *   POST /api/admin/codex/delete   删除
 *
 * 核心界域展示框
 *   POST /api/admin/section/list | save | delete
 *
 * 元界动态
 *   POST /api/admin/news/list | save | delete
 *
 * 站点文案
 *   POST /api/admin/setting/list | save
 *
 * 概览与日志
 *   POST /api/admin/overview | logs
 */
@RestController
@RequestMapping("/api/admin")
public class AdminContentController {

    private static final Set<String> CATEGORIES = Set.of("神尊", "功法", "妖魔", "秘境");

    @Autowired
    private AdminAuth adminAuth;
    @Autowired
    private HomeContentService contentService;
    @Autowired
    private AvatarFoundationService avatarService;
    @Autowired
    private CodexEntryMapper codexMapper;

    /* ============================ 图鉴 cy_codex ============================ */

    @PostMapping("/codex/list")
    public ResponseEntity<Map<String, Object>> codexList(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("查看图鉴");
        }
        // 分页查询（page/pageSize 缺省 1/20；status 为空表示不限上下架）
        Map<String, Object> page = contentService.listCodexPage(
                req.getCategory(), req.getKeyword(), req.getStatus(), req.getPage(), req.getPageSize());
        Map<String, Object> r = ApiResult.ok();
        r.putAll(page);
        r.put("counts", contentService.codexCounts());        // 各分类上架数
        r.put("counts_all", contentService.codexAllCounts()); // 各分类总数（含下架）
        return ResponseEntity.ok(r);
    }

    @PostMapping("/codex/save")
    public ResponseEntity<Map<String, Object>> codexSave(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("编辑图鉴");
        }
        Map<String, Object> item = req.getItem();
        if (item == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 item 数据"));
        }
        String category = str(item.get("category"));
        if (!CATEGORIES.contains(category)) {
            return ResponseEntity.badRequest().body(ApiResult.fail("分类仅允许：神尊 / 功法 / 妖魔 / 秘境"));
        }
        String name = str(item.get("name"));
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("名号不能为空"));
        }
        // 同分类下名号唯一（数据库亦有唯一索引兜底）
        Long id = asLong(item.get("id"));
        QueryWrapper<CodexEntry> qw = new QueryWrapper<>();
        qw.eq("category", category).eq("name", name);
        CodexEntry dup = codexMapper.selectOne(qw);
        if (dup != null && (id == null || !dup.getId().equals(id))) {
            return ResponseEntity.status(409).body(ApiResult.fail("该分类下已存在同名条目「" + name + "」"));
        }

        boolean create = (id == null);
        CodexEntry saved = contentService.saveCodex(item);
        contentService.log(admin.getId(), admin.getDeityName(), create ? "新增" : "修改", "图鉴",
                String.valueOf(saved.getId()), category + " · " + saved.getName());
        Map<String, Object> r = ApiResult.ok();
        r.put("item", saved);
        return ResponseEntity.ok(r);
    }

    @PostMapping("/codex/status")
    public ResponseEntity<Map<String, Object>> codexStatus(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("上架/下架图鉴");
        }
        if (req.getId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 id"));
        }
        CodexEntry e = codexMapper.selectById(req.getId());
        if (e == null) {
            return ResponseEntity.status(404).body(ApiResult.fail("条目不存在"));
        }
        int target = (req.getStatus() == null || req.getStatus() == HomeContentService.STATUS_ON)
                ? HomeContentService.STATUS_ON : HomeContentService.STATUS_OFF;
        e.setStatus(target);
        codexMapper.updateById(e);
        contentService.log(admin.getId(), admin.getDeityName(), target == 1 ? "上架" : "下架", "图鉴",
                String.valueOf(e.getId()), e.getCategory() + " · " + e.getName());
        return ResponseEntity.ok(ApiResult.ok());
    }

    @PostMapping("/codex/status/batch")
    public ResponseEntity<Map<String, Object>> codexStatusBatch(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("批量上架/下架图鉴");
        }
        List<Long> ids = req.getIds();
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 ids（要操作的目标条目）"));
        }
        if (ids.size() > 200) {
            return ResponseEntity.badRequest().body(ApiResult.fail("单次批量操作最多 200 条"));
        }
        int target = (req.getStatus() == null || req.getStatus() == HomeContentService.STATUS_ON)
                ? HomeContentService.STATUS_ON : HomeContentService.STATUS_OFF;
        int n = contentService.updateCodexStatus(ids, target);
        contentService.log(admin.getId(), admin.getDeityName(), target == 1 ? "批量上架" : "批量下架", "图鉴",
                "", "共 " + n + " 条");
        Map<String, Object> r = ApiResult.ok();
        r.put("updated", n);
        r.put("status", target);
        return ResponseEntity.ok(r);
    }

    @PostMapping("/codex/delete")
    public ResponseEntity<Map<String, Object>> codexDelete(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("删除图鉴");
        }
        if (req.getId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 id"));
        }
        CodexEntry e = codexMapper.selectById(req.getId());
        if (e == null) {
            return ResponseEntity.status(404).body(ApiResult.fail("条目不存在"));
        }
        contentService.deleteCodex(req.getId());
        contentService.log(admin.getId(), admin.getDeityName(), "删除", "图鉴",
                String.valueOf(e.getId()), e.getCategory() + " · " + e.getName());
        return ResponseEntity.ok(ApiResult.ok());
    }

    /* ======================== 核心界域展示框 ======================== */

    @PostMapping("/section/list")
    public ResponseEntity<Map<String, Object>> sectionList(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("查看展示框");
        }
        Map<String, Object> r = ApiResult.ok();
        r.put("items", contentService.listSections(false));
        r.put("counts", contentService.codexCounts());
        return ResponseEntity.ok(r);
    }

    @PostMapping("/section/save")
    public ResponseEntity<Map<String, Object>> sectionSave(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("编辑展示框");
        }
        Map<String, Object> item = req.getItem();
        if (item == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 item 数据"));
        }
        if (!CATEGORIES.contains(str(item.get("section_type")))) {
            return ResponseEntity.badRequest().body(ApiResult.fail("界域类型仅允许：神尊 / 功法 / 妖魔 / 秘境"));
        }
        HomeSection s = contentService.saveSection(item);
        contentService.log(admin.getId(), admin.getDeityName(), item.get("id") == null ? "新增" : "修改",
                "展示框", String.valueOf(s.getId()), s.getSectionType() + " · " + s.getTitle());
        Map<String, Object> r = ApiResult.ok();
        r.put("item", s);
        return ResponseEntity.ok(r);
    }

    @PostMapping("/section/delete")
    public ResponseEntity<Map<String, Object>> sectionDelete(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("删除展示框");
        }
        if (req.getId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 id"));
        }
        contentService.deleteSection(req.getId());
        contentService.log(admin.getId(), admin.getDeityName(), "删除", "展示框",
                String.valueOf(req.getId()), "");
        return ResponseEntity.ok(ApiResult.ok());
    }

    /* ========================== 元界动态 ========================== */

    @PostMapping("/news/list")
    public ResponseEntity<Map<String, Object>> newsList(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("查看元界动态");
        }
        Map<String, Object> r = ApiResult.ok();
        r.put("items", contentService.listNews(false));
        return ResponseEntity.ok(r);
    }

    @PostMapping("/news/save")
    public ResponseEntity<Map<String, Object>> newsSave(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("编辑元界动态");
        }
        Map<String, Object> item = req.getItem();
        if (item == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 item 数据"));
        }
        if (str(item.get("content")).isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResult.fail("动态文案不能为空"));
        }
        HomeNews n = contentService.saveNews(item);
        contentService.log(admin.getId(), admin.getDeityName(), item.get("id") == null ? "新增" : "修改",
                "动态", String.valueOf(n.getId()), n.getTag() + " · " + n.getContent());
        Map<String, Object> r = ApiResult.ok();
        r.put("item", n);
        return ResponseEntity.ok(r);
    }

    @PostMapping("/news/delete")
    public ResponseEntity<Map<String, Object>> newsDelete(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("删除元界动态");
        }
        if (req.getId() == null) {
            return ResponseEntity.badRequest().body(ApiResult.fail("缺少 id"));
        }
        contentService.deleteNews(req.getId());
        contentService.log(admin.getId(), admin.getDeityName(), "删除", "动态",
                String.valueOf(req.getId()), "");
        return ResponseEntity.ok(ApiResult.ok());
    }

    /* ========================== 站点文案 ========================== */

    @PostMapping("/setting/list")
    public ResponseEntity<Map<String, Object>> settingList(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("查看站点文案");
        }
        List<SiteSetting> items = contentService.listSettings();
        Map<String, Object> r = ApiResult.ok();
        r.put("items", items);
        r.put("settings", contentService.settingMap());
        return ResponseEntity.ok(r);
    }

    @PostMapping("/setting/save")
    public ResponseEntity<Map<String, Object>> settingSave(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("编辑站点文案");
        }
        int n = contentService.saveSettings(req.getSettings());
        contentService.log(admin.getId(), admin.getDeityName(), "修改", "文案", "", "更新 " + n + " 项站点文案");
        Map<String, Object> r = ApiResult.ok();
        r.put("updated", n);
        return ResponseEntity.ok(r);
    }

    /* ========================== 概览 / 日志 ========================== */

    @PostMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("查看概览");
        }
        long total = codexMapper.selectCount(null);
        long online = codexMapper.selectCount(
                new QueryWrapper<CodexEntry>().eq("status", HomeContentService.STATUS_ON));
        List<AvatarFoundation> users = avatarService.list();
        String today = java.time.LocalDate.now().toString();
        long todayNew = users.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().toString().equals(today))
                .count();
        long admins = users.stream().filter(u -> u.getPermission() != null && u.getPermission() == 0).count();

        Map<String, Object> r = ApiResult.ok();
        r.put("codex_total", total);
        r.put("codex_online", online);
        r.put("codex_offline", total - online);
        r.put("codex_counts", contentService.codexCounts());
        r.put("codex_counts_all", contentService.codexAllCounts());
        r.put("sections", contentService.listSections(false).size());
        r.put("news", contentService.listNews(false).size());
        r.put("users", users.size());
        r.put("admins", admins);
        r.put("today_new", todayNew);
        return ResponseEntity.ok(r);
    }

    @PostMapping("/logs")
    public ResponseEntity<Map<String, Object>> logs(@RequestBody AdminContentRequest req) {
        AvatarFoundation admin = adminAuth.requireAdmin(req.getName(), req.getSpell());
        if (admin == null) {
            return denied("查看操作日志");
        }
        List<AdminLog> items = contentService.recentLogs(50);
        Map<String, Object> r = ApiResult.ok();
        r.put("items", items);
        return ResponseEntity.ok(r);
    }

    /* ============================ 工具 ============================ */

    private static ResponseEntity<Map<String, Object>> denied(String action) {
        return ResponseEntity.status(403).body(ApiResult.fail("无权" + action + "：仅管理员可操作"));
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static Long asLong(Object o) {
        if (o == null || String.valueOf(o).isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(o).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
