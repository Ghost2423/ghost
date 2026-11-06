package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResult;
import com.fenglin.springboottest.entity.CodexEntry;
import com.fenglin.springboottest.entity.HomeNews;
import com.fenglin.springboottest.entity.HomeSection;
import com.fenglin.springboottest.service.CodexService;
import com.fenglin.springboottest.service.HomeContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 沧元图 · 首页后端 API（公开，无需登录）
 *
 * - GET  /api/codex        首页图鉴全量（仅上架：神尊/功法/妖魔/秘境），驱动分类卡片
 * - POST /api/search       跨四类模糊检索（仅上架）
 * - GET  /api/home/config  首页展示配置：四大展示框 + 元界动态 + 站点文案 + 热门标签 + 收录数
 *
 * 数据来源均为后台管理系统（/api/admin/*）维护的数据库表。
 */
@RestController
@RequestMapping("/api")
public class HomeController {

    @Autowired
    private CodexService codexService;
    @Autowired
    private HomeContentService contentService;

    /** 将库实体映射为前端卡片所需形状 */
    private static Map<String, Object> toItem(CodexEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("type", e.getCategory());
        m.put("name", e.getName());
        m.put("tag", e.getTitle());
        m.put("desc", e.getDescription());
        m.put("flavor", e.getFlavor());
        m.put("color", e.getColor());
        m.put("price", e.getPrice());
        m.put("power_gain", e.getPowerGain());
        return m;
    }

    /** 首页图鉴全量（仅上架条目） */
    @GetMapping("/codex")
    public ResponseEntity<Map<String, Object>> codex() {
        List<Map<String, Object>> items = contentService.listCodex(null, null, true).stream()
                .map(HomeController::toItem)
                .collect(Collectors.toList());
        Map<String, Object> r = ApiResult.ok();
        r.put("items", items);
        r.put("counts", contentService.codexCounts());
        return ResponseEntity.ok(r);
    }

    /** 跨四类检索（仅上架条目） */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestBody(required = false) Map<String, String> body) {
        String keyword = (body == null) ? "" : (body.get("keyword") == null ? "" : body.get("keyword"));
        List<Map<String, Object>> results = codexService.search(keyword).stream()
                .map(HomeController::toItem)
                .collect(Collectors.toList());
        Map<String, Object> r = ApiResult.ok();
        r.put("results", results);
        return ResponseEntity.ok(r);
    }

    /** 首页展示配置（展示框 / 动态 / 文案），全部来自后台维护的数据 */
    @GetMapping("/home/config")
    public ResponseEntity<Map<String, Object>> config() {
        List<Map<String, Object>> sections = contentService.listSections(true).stream()
                .map(HomeController::sectionItem)
                .collect(Collectors.toList());
        List<Map<String, Object>> news = contentService.listNews(true).stream()
                .map(HomeController::newsItem)
                .collect(Collectors.toList());

        Map<String, String> settings = contentService.settingMap();
        List<String> hotTags = Arrays.stream(settings.getOrDefault("hot_tags", "").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        Map<String, Object> r = ApiResult.ok();
        r.put("sections", sections);
        r.put("news", news);
        r.put("settings", settings);
        r.put("hot_tags", hotTags);
        r.put("counts", contentService.codexCounts());
        return ResponseEntity.ok(r);
    }

    /** 展示框 → 前端形状 {type,title,icon,color,desc} */
    private static Map<String, Object> sectionItem(HomeSection s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", s.getSectionType());
        m.put("title", s.getTitle());
        m.put("icon", s.getIcon());
        m.put("color", s.getColor());
        m.put("desc", s.getDescription());
        return m;
    }

    /** 动态 → 前端形状 {tag,text,kw,color} */
    private static Map<String, Object> newsItem(HomeNews n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tag", n.getTag());
        m.put("text", n.getContent());
        m.put("kw", n.getKeyword());
        m.put("color", n.getColor());
        return m;
    }
}
