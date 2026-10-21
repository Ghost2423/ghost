package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResult;
import com.fenglin.springboottest.entity.CodexEntry;
import com.fenglin.springboottest.service.CodexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 沧元图 · 首页后端 API
 *
 * - GET  /api/codex    首页图鉴全量（神尊/功法/妖魔/秘境），驱动「首页展示框」与分类卡片
 * - POST /api/search   跨四类模糊检索（神尊名号 / 功法 / 妖魔 / 秘境）
 *
 * 返回字段统一映射为前端所需形状：{type, name, tag, desc, flavor}
 */
@RestController
@RequestMapping("/api")
public class HomeController {

    @Autowired
    private CodexService codexService;

    /** 将库实体映射为前端卡片所需形状 */
    private static Map<String, Object> toItem(CodexEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", e.getCategory());
        m.put("name", e.getName());
        m.put("tag", e.getTitle());
        m.put("desc", e.getDescription());
        m.put("flavor", e.getFlavor());
        return m;
    }

    /** 首页图鉴全量 */
    @GetMapping("/codex")
    public ResponseEntity<Map<String, Object>> codex() {
        List<Map<String, Object>> items = codexService.list().stream()
                .map(HomeController::toItem)
                .collect(Collectors.toList());
        Map<String, Object> r = ApiResult.ok();
        r.put("items", items);
        return ResponseEntity.ok(r);
    }

    /** 跨四类检索 */
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
}
