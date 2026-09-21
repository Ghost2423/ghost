#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
沧元界 · 本地服务（静态页 + API）
- 静态托管当前目录（register.html / index.html / cangyuantu-vue ...）
- POST /api/forge      注册锻造 -> 写入 MySQL cangyuan.avatar_foundation
- GET  /api/avatar?name=神尊相  登录校验 -> 返回该神尊根基（召唤语等）

运行：
  python server.py
默认监听 http://127.0.0.1:8125
"""
import os
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

import pymysql

ROOT = os.path.dirname(os.path.abspath(__file__))
HOST, PORT = "127.0.0.1", 8125

# ── MySQL 连接配置（本机）──────────────────────────────────────
# 演示用 root；生产请改为专用低权限账号（见 cangyuan_init.sql 注释）。
DB = dict(
    host="127.0.0.1", port=3306,
    user="root", password="123456",
    database="cangyuan", charset="utf8mb4",
    autocommit=True,
)

CT = {
    "html": "text/html; charset=utf-8",
    "js": "application/javascript; charset=utf-8",
    "css": "text/css; charset=utf-8",
    "json": "application/json; charset=utf-8",
    "png": "image/png", "jpg": "image/jpeg", "jpeg": "image/jpeg",
    "svg": "image/svg+xml", "ico": "image/x-icon",
    "vue": "application/javascript; charset=utf-8",
}


def db_conn():
    return pymysql.connect(**DB)


def send(self, code, body, ctype="application/json; charset=utf-8"):
    if isinstance(body, (dict, list)):
        body = json.dumps(body, ensure_ascii=False)
    data = body.encode("utf-8") if isinstance(body, str) else body
    self.send_response(code)
    self.send_header("Content-Type", ctype)
    self.send_header("Content-Length", str(len(data)))
    self.end_headers()
    self.wfile.write(data)


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *a):
        pass  # 静默访问日志

    def _read_json(self):
        n = int(self.headers.get("Content-Length", 0) or 0)
        raw = self.rfile.read(n) if n else b"{}"
        return json.loads(raw or b"{}")

    # ---- GET：静态文件 / 查询 API ----
    def do_GET(self):
        u = urlparse(self.path)
        if u.path.startswith("/api/"):
            return self.api_get(u)
        return self.serve_static(u.path)

    # ---- POST：锻造入库 ----
    def do_POST(self):
        if self.path == "/api/forge":
            return self.api_forge()
        send(self, 404, {"ok": False, "msg": "not found"})

    def serve_static(self, path):
        path = path.split("?", 1)[0]
        if path in ("", "/"):
            path = "/index.html"
        fp = os.path.normpath(os.path.join(ROOT, path.lstrip("/")))
        if not fp.startswith(ROOT):
            return send(self, 403, {"ok": False, "msg": "forbidden"})
        if not os.path.isfile(fp):
            return send(self, 404, {"ok": False, "msg": "not found"})
        ext = fp.rsplit(".", 1)[-1].lower()
        with open(fp, "rb") as f:
            send(self, 200, f.read(), CT.get(ext, "application/octet-stream"))

    def api_get(self, u):
        if u.path == "/api/avatar":
            name = parse_qs(u.query).get("name", [""])[0].strip()
            if not name:
                return send(self, 400, {"ok": False, "msg": "缺少 name"})
            try:
                con = db_conn()
                cur = con.cursor(pymysql.cursors.DictCursor)
                cur.execute(
                    "SELECT deity_name, spell, region, myth_name, myth_story "
                    "FROM avatar_foundation WHERE deity_name=%s", (name,))
                row = cur.fetchone()
                con.close()
                if row:
                    return send(self, 200, {"ok": True, "avatar": row})
                return send(self, 404, {"ok": False, "msg": "未寻得此神尊根基"})
            except Exception as e:
                return send(self, 500, {"ok": False, "msg": str(e)})
        return send(self, 404, {"ok": False, "msg": "unknown api"})

    def api_forge(self):
        try:
            data = self._read_json()
        except Exception:
            return send(self, 400, {"ok": False, "msg": "bad json"})

        region = (data.get("region") or "").strip()
        deity = (data.get("deity_name") or "").strip()
        spell = (data.get("spell") or "").strip()
        if not region or not deity or not spell:
            return send(self, 400, {"ok": False, "msg": "缺少必填字段 region/deity_name/spell"})

        myth = (data.get("myth_name") or "").strip()
        story = (data.get("myth_story") or "") or None
        agreed = 1 if data.get("agreed") else 0

        try:
            con = db_conn()
            cur = con.cursor()
            cur.execute(
                "INSERT INTO avatar_foundation "
                "(region, deity_name, myth_name, spell, myth_story, agreed) "
                "VALUES (%s,%s,%s,%s,%s,%s)",
                (region, deity, myth, spell, story, agreed))
            rid = cur.lastrowid
            con.close()
            return send(self, 200, {"ok": True, "id": rid})
        except pymysql.IntegrityError:
            return send(self, 409, {"ok": False, "msg": "该神尊相已存在于元界数据库"})
        except Exception as e:
            return send(self, 500, {"ok": False, "msg": str(e)})


if __name__ == "__main__":
    print(f"沧元界服务已启动 -> http://{HOST}:{PORT}")
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
