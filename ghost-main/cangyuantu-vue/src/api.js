// 沧元界 API 封装（经 Vite 代理转发到 Spring Boot 后端 http://127.0.0.1:8125）
// 后端返回结构与旧版一致：{ok:true,...} / {ok:false,msg:'...'}

const NET_FAIL = { ok: false, msg: '无法连接元界数据库（后端未启动？）' }

async function request(url, options) {
  try {
    const res = await fetch(url, options)
    // 非 2xx 也尝试解析 JSON（后端错误同样返回 {ok:false,msg}）
    const text = await res.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      return { ok: false, msg: '后端返回异常（HTTP ' + res.status + '）' }
    }
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
