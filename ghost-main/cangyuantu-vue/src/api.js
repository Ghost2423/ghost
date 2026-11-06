// 沧元界 API 封装（经 Vite 代理转发到 Spring Boot 后端 http://127.0.0.1:8125）
// 后端返回结构与旧版一致：{ok:true,...} / {ok:false,msg:'...'}

const NET_FAIL = { ok: false, msg: '无法连接元界数据库（后端未启动？）' }

async function request(url, options) {
  try {
    const res = await fetch(url, options)
    // 非 2xx 也尝试解析 JSON（后端错误同样返回 {ok:false,msg}）
    const text = await res.text()
    let data
    try {
      data = JSON.parse(text)
    } catch (e) {
      return { ok: false, msg: '后端返回异常（HTTP ' + res.status + '）' }
    }
    // 后台接口 403 = 管理员凭证失效（权限被撤销 / 密码被改）→ 通知控制台退回登录
    if (res.status === 403 && url.indexOf('/api/admin') === 0) {
      window.dispatchEvent(new CustomEvent('cy-admin-auth-fail', { detail: data.msg || '管理员权限已失效' }))
    }
    return data
  } catch (e) {
    return NET_FAIL
  }
}

/** 觉醒（登录）：POST /api/login {name, spell} -> {ok, avatar} */
export function loginAvatar(name, spell) {
  return request('/api/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, spell })
  })
}

/** 查询神尊根基：GET /api/avatar?name=xx -> {ok, avatar} */
export function fetchAvatar(name) {
  return request('/api/avatar?name=' + encodeURIComponent(name))
}

/** 锻造神尊根基（注册）：POST /api/forge */
export function forgeAvatar(payload) {
  return request('/api/forge', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

/** 元界检索（预留后端接口）：POST /api/search {keyword} -> {ok, results:[...]} */
export function searchCangyuan(keyword) {
  return request('/api/search', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ keyword })
  })
}

/** 首页图鉴全量（神尊/功法/妖魔/秘境）：GET /api/codex -> {ok, items:[...]} */
export function fetchCodex() {
  return request('/api/codex')
}

/** 首页展示配置（展示框/元界动态/站点文案/热门标签）：GET /api/home/config */
export function fetchHomeConfig() {
  return request('/api/home/config')
}

/* =============================================================
 * 后台管理系统 API（仅 permission=0 管理员可用）
 * cred 为当前管理员凭证 {name, spell}，每个接口均由后端校验
 * ============================================================= */

/** 管理员登录：POST /api/admin/login {name, spell} -> {ok, admin} */
export function adminLogin(name, spell) {
  return request('/api/admin/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, spell })
  })
}

/** 用户列表：POST /api/admin/users -> {ok, users:[...]} */
export function fetchAdminUsers(cred) {
  return request('/api/admin/users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cred)
  })
}

/** 变更用户权限：POST /api/admin/permission {name, spell, target_id, permission} */
export function updatePermission(cred, targetId, permission) {
  return request('/api/admin/permission', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...cred, target_id: targetId, permission })
  })
}

/** 删除用户：POST /api/admin/delete {name, spell, target_id} */
export function deleteAdminUser(cred, targetId) {
  return request('/api/admin/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...cred, target_id: targetId })
  })
}

/* =============================================================
 * 后台管理系统 · 首页数据管理 API
 * 覆盖首页展示的全部内容：图鉴 / 核心界域展示框 / 元界动态 / 站点文案
 * ============================================================= */

/** 概览统计：POST /api/admin/overview */
export function fetchAdminOverview(cred) {
  return request('/api/admin/overview', jsonPost(cred))
}

/** 操作日志：POST /api/admin/logs */
export function fetchAdminLogs(cred) {
  return request('/api/admin/logs', jsonPost(cred))
}

/**
 * 图鉴列表（分页）：POST /api/admin/codex/list
 * @param {object} opts {category, keyword, status, page, pageSize}
 *   status: null=不限，1=仅上架，0=仅下架
 * @returns {ok, items, total, page, page_size, pages, counts, counts_all}
 */
export function fetchAdminCodex(cred, opts = {}) {
  const { category = '', keyword = '', status = null, page = 1, pageSize = 20 } = opts
  // 后端 Jackson 全局 SNAKE_CASE，分页字段需以 page_size 传
  return request('/api/admin/codex/list',
    jsonPost({ ...cred, category, keyword, status, page, page_size: pageSize }))
}

/** 图鉴新增/修改：POST /api/admin/codex/save {name, spell, item} */
export function saveAdminCodex(cred, item) {
  return request('/api/admin/codex/save', jsonPost({ ...cred, item }))
}

/** 图鉴上架/下架：POST /api/admin/codex/status {name, spell, id, status} */
export function toggleAdminCodex(cred, id, status) {
  return request('/api/admin/codex/status', jsonPost({ ...cred, id, status }))
}

/** 图鉴批量上架/下架：POST /api/admin/codex/status/batch {name, spell, ids, status} */
export function batchAdminCodexStatus(cred, ids, status) {
  return request('/api/admin/codex/status/batch', jsonPost({ ...cred, ids, status }))
}

/** 图鉴删除：POST /api/admin/codex/delete {name, spell, id} */
export function deleteAdminCodex(cred, id) {
  return request('/api/admin/codex/delete', jsonPost({ ...cred, id }))
}

/** 核心界域展示框列表：POST /api/admin/section/list */
export function fetchAdminSections(cred) {
  return request('/api/admin/section/list', jsonPost(cred))
}

/** 展示框保存：POST /api/admin/section/save {name, spell, item} */
export function saveAdminSection(cred, item) {
  return request('/api/admin/section/save', jsonPost({ ...cred, item }))
}

/** 展示框删除：POST /api/admin/section/delete */
export function deleteAdminSection(cred, id) {
  return request('/api/admin/section/delete', jsonPost({ ...cred, id }))
}

/** 元界动态列表：POST /api/admin/news/list */
export function fetchAdminNews(cred) {
  return request('/api/admin/news/list', jsonPost(cred))
}

/** 元界动态保存：POST /api/admin/news/save {name, spell, item} */
export function saveAdminNews(cred, item) {
  return request('/api/admin/news/save', jsonPost({ ...cred, item }))
}

/** 元界动态删除：POST /api/admin/news/delete */
export function deleteAdminNews(cred, id) {
  return request('/api/admin/news/delete', jsonPost({ ...cred, id }))
}

/** 站点文案列表：POST /api/admin/setting/list */
export function fetchAdminSettings(cred) {
  return request('/api/admin/setting/list', jsonPost(cred))
}

/** 站点文案保存：POST /api/admin/setting/save {name, spell, settings:{k:v}} */
export function saveAdminSettings(cred, settings) {
  return request('/api/admin/setting/save', jsonPost({ ...cred, settings }))
}

/** 统一 JSON POST 构造 */
function jsonPost(body) {
  return {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  }
}

/* =============================================================
 * 修炼系统 API（神尊力 / 铜币 / 境界突破 / 秘境 / 切磋 / 功法阁）
 * cred 为当前用户凭证 {name, spell}，写操作均由后端校验
 * ============================================================= */

/** 修炼状态总览：GET /api/cultivation/status?name=xx -> {ok, cultivation} */
export function fetchCultivation(name) {
  return request('/api/cultivation/status?name=' + encodeURIComponent(name))
}

/** 进入秘境修炼：POST /api/cultivation/secret {name, spell} */
export function secretTraining(cred) {
  return request('/api/cultivation/secret', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...cred })
  })
}

/** 切磋：POST /api/cultivation/spar {name, spell, target}（双方各得神尊力与铜币） */
export function sparWith(cred, target) {
  return request('/api/cultivation/spar', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...cred, target })
  })
}

/** 购买功法：POST /api/cultivation/buy {name, spell, gongfa} */
export function buyGongfa(cred, gongfa) {
  return request('/api/cultivation/buy', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...cred, gongfa })
  })
}

/** 突破境界：POST /api/cultivation/break {name, spell} */
export function breakthroughRealm(cred) {
  return request('/api/cultivation/break', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...cred })
  })
}
