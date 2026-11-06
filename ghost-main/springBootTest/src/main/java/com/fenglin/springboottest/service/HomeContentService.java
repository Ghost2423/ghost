package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.entity.AdminLog;
import com.fenglin.springboottest.entity.CodexEntry;
import com.fenglin.springboottest.entity.HomeNews;
import com.fenglin.springboottest.entity.HomeSection;
import com.fenglin.springboottest.entity.SiteSetting;
import com.fenglin.springboottest.mapper.AdminLogMapper;
import com.fenglin.springboottest.mapper.CodexEntryMapper;
import com.fenglin.springboottest.mapper.HomeNewsMapper;
import com.fenglin.springboottest.mapper.HomeSectionMapper;
import com.fenglin.springboottest.mapper.SiteSettingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 沧元图 · 首页内容管理 Service
 *
 * 统一承载首页「展示功能」的数据读写：
 *   图鉴 cy_codex / 核心界域 cy_home_section / 元界动态 cy_home_news / 文案 cy_site_setting / 日志 cy_admin_log
 * 公开读取（首页）只取 status=1 的内容，后台读取可取全量。
 */
@Service
public class HomeContentService {

    /** 状态：上架/展示 */
    public static final int STATUS_ON = 1;
    /** 状态：下架/隐藏 */
    public static final int STATUS_OFF = 0;

    @Autowired
    private CodexEntryMapper codexMapper;
    @Autowired
    private HomeSectionMapper sectionMapper;
    @Autowired
    private HomeNewsMapper newsMapper;
    @Autowired
    private SiteSettingMapper settingMapper;
    @Autowired
    private AdminLogMapper logMapper;

    /* ================= 图鉴 cy_codex ================= */

    /** 图鉴列表：category 为空表示全部；onlineOnly=true 仅取上架条目 */
    public List<CodexEntry> listCodex(String category, String keyword, boolean onlineOnly) {
        QueryWrapper<CodexEntry> qw = codexQuery(category, keyword, onlineOnly ? STATUS_ON : null);
        // 分类固定顺序：神尊 → 功法 → 妖魔 → 秘境，再按人工排序
        qw.last("ORDER BY FIELD(category,'神尊','功法','妖魔','秘境'), sort_no ASC, id ASC");
        return codexMapper.selectList(qw);
    }

    /**
     * 图鉴分页（后台管理用）。
     * @param statusFilter null=不限，1=仅上架，0=仅下架
     * @return items / total / page / pageSize / pages
     */
    public Map<String, Object> listCodexPage(String category, String keyword, Integer statusFilter,
                                             Integer page, Integer pageSize) {
        int size = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        int p = (page == null || page < 1) ? 1 : page;

        long total = codexMapper.selectCount(codexQuery(category, keyword, statusFilter));
        int pages = (int) ((total + size - 1) / size);
        if (pages > 0 && p > pages) {
            p = pages;   // 删除最后一页最后一条后，自动回到有效页
        }

        List<CodexEntry> items;
        if (total == 0) {
            items = List.of();
        } else {
            QueryWrapper<CodexEntry> qw = codexQuery(category, keyword, statusFilter);
            qw.last("ORDER BY FIELD(category,'神尊','功法','妖魔','秘境'), sort_no ASC, id ASC "
                    + "LIMIT " + size + " OFFSET " + (long) (p - 1) * size);
            items = codexMapper.selectList(qw);
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("items", items);
        r.put("total", total);
        r.put("page", p);
        r.put("page_size", size);   // 与全局 Jackson 下划线命名保持一致
        r.put("pages", pages);
        return r;
    }

    /** 批量上架 / 下架，返回实际影响条数 */
    public int updateCodexStatus(List<Long> ids, int status) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        CodexEntry patch = new CodexEntry();
        patch.setStatus(status);
        QueryWrapper<CodexEntry> qw = new QueryWrapper<>();
        qw.in("id", ids);
        return codexMapper.update(patch, qw);
    }

    /** 构造图鉴查询条件：statusFilter 为 null 表示不限状态 */
    private QueryWrapper<CodexEntry> codexQuery(String category, String keyword, Integer statusFilter) {
        QueryWrapper<CodexEntry> qw = new QueryWrapper<>();
        if (statusFilter != null) {
            qw.eq("status", statusFilter);
        }
        if (category != null && !category.isBlank()) {
            qw.eq("category", category.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.trim() + "%";
            qw.and(w -> w.like("name", kw)
                    .or().like("title", kw)
                    .or().like("description", kw)
                    .or().like("flavor", kw));
        }
        return qw;
    }

    /** 新增或更新图鉴条目（item 来自后台表单，键为 snake_case） */
    public CodexEntry saveCodex(Map<String, Object> item) {
        Long id = asLong(item.get("id"));
        CodexEntry e = (id == null) ? new CodexEntry() : codexMapper.selectById(id);
        if (e == null) {
            e = new CodexEntry();
        }
        e.setCategory(str(item.get("category"), "神尊"));
        e.setName(str(item.get("name"), ""));
        e.setTitle(str(item.get("title"), ""));
        e.setDescription(str(item.get("description"), ""));
        e.setFlavor(str(item.get("flavor"), ""));
        e.setColor(str(item.get("color"), defaultColor(e.getCategory())));
        e.setPrice(asInt(item.get("price"), 0));
        e.setPowerGain(asInt(item.get("power_gain"), 0));
        e.setSortNo(asInt(item.get("sort_no"), 0));
        e.setStatus(asInt(item.get("status"), STATUS_ON));
        if (e.getId() == null) {
            codexMapper.insert(e);
        } else {
            codexMapper.updateById(e);
        }
        return e;
    }

    public boolean deleteCodex(Long id) {
        return codexMapper.deleteById(id) > 0;
    }

    /** 图鉴分类收录数（仅上架，首页卡片上的「收录 N」） */
    public Map<String, Integer> codexCounts() {
        return countByCategory(STATUS_ON);
    }

    /** 图鉴分类总数（含下架，后台分页与标签统计用） */
    public Map<String, Integer> codexAllCounts() {
        return countByCategory(null);
    }

    /** 按分类聚合计数（SQL GROUP BY，条目多了也不会把整表读进内存） */
    private Map<String, Integer> countByCategory(Integer statusFilter) {
        QueryWrapper<CodexEntry> qw = new QueryWrapper<>();
        qw.select("category", "COUNT(*) AS cnt");
        if (statusFilter != null) {
            qw.eq("status", statusFilter);
        }
        qw.groupBy("category").last("ORDER BY FIELD(category,'神尊','功法','妖魔','秘境')");
        Map<String, Integer> m = new LinkedHashMap<>();
        for (Map<String, Object> row : codexMapper.selectMaps(qw)) {
            Object cat = pick(row, "category", "CATEGORY");
            Object cnt = pick(row, "cnt", "CNT");
            if (cat == null || cnt == null) {
                continue;
            }
            m.merge(String.valueOf(cat), ((Number) cnt).intValue(), Integer::sum);
        }
        return m;
    }

    /** 从 selectMaps 结果中按多个候选键取值（不同驱动返回的键大小写不一致） */
    private static Object pick(Map<String, Object> row, String... keys) {
        for (String k : keys) {
            if (row.containsKey(k)) {
                return row.get(k);
            }
        }
        for (Map.Entry<String, Object> en : row.entrySet()) {
            for (String k : keys) {
                if (en.getKey().equalsIgnoreCase(k)) {
                    return en.getValue();
                }
            }
        }
        return null;
    }

    /* ================= 核心界域 cy_home_section ================= */

    public List<HomeSection> listSections(boolean onlineOnly) {
        QueryWrapper<HomeSection> qw = new QueryWrapper<>();
        if (onlineOnly) {
            qw.eq("status", STATUS_ON);
        }
        qw.orderByAsc("sort_no").orderByAsc("id");
        return sectionMapper.selectList(qw);
    }

    public HomeSection saveSection(Map<String, Object> item) {
        Long id = asLong(item.get("id"));
        HomeSection s = (id == null) ? new HomeSection() : sectionMapper.selectById(id);
        if (s == null) {
            s = new HomeSection();
        }
        s.setSectionType(str(item.get("section_type"), "神尊"));
        s.setTitle(str(item.get("title"), s.getSectionType()));
        s.setIcon(str(item.get("icon"), "❖"));
        s.setColor(str(item.get("color"), defaultColor(s.getSectionType())));
        s.setDescription(str(item.get("description"), ""));
        s.setSortNo(asInt(item.get("sort_no"), 0));
        s.setStatus(asInt(item.get("status"), STATUS_ON));
        if (s.getId() == null) {
            sectionMapper.insert(s);
        } else {
            sectionMapper.updateById(s);
        }
        return s;
    }

    public boolean deleteSection(Long id) {
        return sectionMapper.deleteById(id) > 0;
    }

    /* ================= 元界动态 cy_home_news ================= */

    public List<HomeNews> listNews(boolean onlineOnly) {
        QueryWrapper<HomeNews> qw = new QueryWrapper<>();
        if (onlineOnly) {
            qw.eq("status", STATUS_ON);
        }
        qw.orderByAsc("sort_no").orderByAsc("id");
        return newsMapper.selectList(qw);
    }

    public HomeNews saveNews(Map<String, Object> item) {
        Long id = asLong(item.get("id"));
        HomeNews n = (id == null) ? new HomeNews() : newsMapper.selectById(id);
        if (n == null) {
            n = new HomeNews();
        }
        n.setTag(str(item.get("tag"), "公告"));
        n.setContent(str(item.get("content"), ""));
        n.setKeyword(str(item.get("keyword"), ""));
        n.setColor(str(item.get("color"), "#e9c46a"));
        n.setSortNo(asInt(item.get("sort_no"), 0));
        n.setStatus(asInt(item.get("status"), STATUS_ON));
        if (n.getId() == null) {
            newsMapper.insert(n);
        } else {
            newsMapper.updateById(n);
        }
        return n;
    }

    public boolean deleteNews(Long id) {
        return newsMapper.deleteById(id) > 0;
    }

    /* ================= 站点文案 cy_site_setting ================= */

    public List<SiteSetting> listSettings() {
        QueryWrapper<SiteSetting> qw = new QueryWrapper<>();
        qw.orderByAsc("id");
        return settingMapper.selectList(qw);
    }

    /** key -> value（首页渲染用） */
    public Map<String, String> settingMap() {
        Map<String, String> m = new LinkedHashMap<>();
        for (SiteSetting s : listSettings()) {
            m.put(s.getSettingKey(), s.getSettingValue() == null ? "" : s.getSettingValue());
        }
        return m;
    }

    /** 批量保存文案（已存在的键更新，未存在的键新增） */
    public int saveSettings(Map<String, String> settings) {
        if (settings == null || settings.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (Map.Entry<String, String> en : settings.entrySet()) {
            if (en.getKey() == null || en.getKey().isBlank()) {
                continue;
            }
            QueryWrapper<SiteSetting> qw = new QueryWrapper<>();
            qw.eq("setting_key", en.getKey());
            SiteSetting exist = settingMapper.selectOne(qw);
            if (exist == null) {
                SiteSetting s = new SiteSetting();
                s.setSettingKey(en.getKey());
                s.setSettingValue(en.getValue() == null ? "" : en.getValue());
                settingMapper.insert(s);
            } else {
                exist.setSettingValue(en.getValue() == null ? "" : en.getValue());
                settingMapper.updateById(exist);
            }
            n++;
        }
        return n;
    }

    /* ================= 操作日志 cy_admin_log ================= */

    public void log(Long adminId, String adminName, String action, String targetType, String targetId, String detail) {
        AdminLog l = new AdminLog();
        l.setAdminId(adminId == null ? 0L : adminId);
        l.setAdminName(adminName == null ? "" : adminName);
        l.setAction(action);
        l.setTargetType(targetType);
        l.setTargetId(targetId == null ? "" : targetId);
        l.setDetail(detail == null ? "" : detail);
        logMapper.insert(l);
    }

    public List<AdminLog> recentLogs(int limit) {
        QueryWrapper<AdminLog> qw = new QueryWrapper<>();
        qw.orderByDesc("id").last("LIMIT " + Math.max(1, Math.min(limit, 200)));
        return logMapper.selectList(qw);
    }

    /* ================= 工具 ================= */

    /** 分类默认主色（与首页四大界域一致） */
    public static String defaultColor(String category) {
        if (category == null) {
            return "#e9c46a";
        }
        return switch (category) {
            case "功法" -> "#2dd4bf";
            case "妖魔" -> "#ef4454";
            case "秘境" -> "#5eead4";
            default -> "#e9c46a";
        };
    }

    private static String str(Object o, String def) {
        if (o == null) {
            return def;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? def : s;
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

    private static Integer asInt(Object o, Integer def) {
        if (o == null || String.valueOf(o).isBlank()) {
            return def;
        }
        try {
            return Integer.valueOf(String.valueOf(o).trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
