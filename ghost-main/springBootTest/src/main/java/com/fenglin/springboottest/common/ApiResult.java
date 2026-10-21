package com.fenglin.springboottest.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一接口返回结构（与旧版 Python 服务保持一致）：
 *   成功：{"ok": true,  "avatar": {...}} / {"ok": true, "id": 1}
 *   失败：{"ok": false, "msg": "原因"}
 */
public final class ApiResult {

    private ApiResult() {}

    public static Map<String, Object> ok() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        return m;
    }

    public static Map<String, Object> ok(String key, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put(key, value);
        return m;
    }

    public static Map<String, Object> fail(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", false);
        m.put("msg", msg);
        return m;
    }
}
