<template>
  <div class="admin-home">
    <!-- ===== 顶部栏 ===== -->
    <header class="top">
      <div class="top-inner">
        <div class="brand">
          <div class="seal">枢</div>
          <div class="brand-txt">
            <b>元界天枢 · 后台管理系统</b>
            <span>CANG YUAN ADMIN · 沧元界用户管理</span>
          </div>
        </div>

        <div class="top-right">
          <span class="who">当前管理员：<b>{{ adminName }}</b></span>
          <button class="t-btn" @click="goHome">返回首页</button>
          <button class="t-btn danger" @click="$emit('logout-admin')">退出后台</button>
        </div>
      </div>
    </header>

    <!-- ===== 统计 ===== -->
    <section class="stats">
      <div class="stat">
        <div class="stat-num">{{ users.length }}</div>
        <div class="stat-label">用户总数</div>
      </div>
      <div class="stat gold">
        <div class="stat-num">{{ adminCount }}</div>
        <div class="stat-label">管理员（权限 0）</div>
      </div>
      <div class="stat">
        <div class="stat-num">{{ userCount }}</div>
        <div class="stat-label">默认用户（权限 1）</div>
      </div>
      <div class="stat red">
        <div class="stat-num">{{ todayCount }}</div>
        <div class="stat-label">今日新增</div>
      </div>
    </section>

    <!-- ===== 用户管理 ===== -->
    <main class="board">
      <div class="board-head">
        <div class="bh-left">
          <span class="bh-ico">❖</span>
          <h2>用户管理</h2>
          <span class="bh-tip">权限：1 = 默认用户（注册默认） · 0 = 管理员（可登录后台） · 密码为明文存储</span>
        </div>
        <div class="bh-right">
          <input v-model="kw" type="text" class="kw" placeholder="搜索账号 / 地域…" />
          <button class="t-btn" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新列表' }}</button>
        </div>
      </div>

      <!-- 表格 -->
      <div class="table-wrap">
        <table class="tbl">
          <thead>
            <tr>
              <th>ID</th>
              <th>账号（deity_name）</th>
              <th>密码（明文）</th>
              <th>地域</th>
              <th>权限</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading && !users.length">
              <td colspan="7" class="empty-cell">正在从元界数据库读取…</td>
            </tr>
            <tr v-else-if="!filtered.length">
              <td colspan="7" class="empty-cell">暂无匹配用户</td>
            </tr>
            <tr v-for="u in filtered" :key="u.id" :class="{ self: u.id === adminId }">
              <td>{{ u.id }}</td>
              <td class="name-cell">
                <span class="dot" :class="Number(u.permission) === 0 ? 'gold' : 'jade'"></span>
                {{ u.deity_name }}
                <span class="self-tag" v-if="u.id === adminId">当前登录</span>
              </td>
              <td class="pwd-cell">{{ u.spell }}</td>
              <td>{{ u.region || '—' }}</td>
              <td>
                <span class="perm-tag" :class="Number(u.permission) === 0 ? 'admin' : 'user'">
                  {{ Number(u.permission) === 0 ? '0 · 管理员' : '1 · 默认用户' }}
                </span>
              </td>
              <td class="time-cell">{{ formatTime(u.created_at) }}</td>
              <td class="ops">
                <template v-if="u.id !== adminId">
                  <button class="op" :class="Number(u.permission) === 0 ? 'ghost' : 'gold'"
                          :disabled="busy" @click="togglePerm(u)">
                    {{ Number(u.permission) === 0 ? '取消管理员' : '设为管理员' }}
                  </button>
                  <button class="op danger" :disabled="busy" @click="askDelete(u)">删除</button>
                </template>
                <span v-else class="no-op">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>

    <footer class="foot">沧元图 · 后台管理系统 · 后端 /api/admin/*（仅 permission=0 管理员可用）</footer>

    <!-- 删除确认弹窗 -->
    <transition name="fade">
      <div class="modal" v-if="pendingDelete" @click.self="pendingDelete = null">
        <div class="modal-card">
          <h3>确认删除</h3>
          <p>即将删除用户「<b>{{ pendingDelete.deity_name }}</b>」，该操作不可恢复。</p>
          <div class="modal-ops">
            <button class="op" @click="pendingDelete = null">取消</button>
            <button class="op danger solid" @click="doDelete">确认删除</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- toast -->
    <div class="toast" :class="{ show: toastShow }">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchAdminUsers, updatePermission, deleteAdminUser } from '../api'

const props = defineProps({
  // 管理员实体（来自 /api/admin/login，含 deity_name / spell / id）
  admin: { type: Object, required: true }
})
const emit = defineEmits(['logout-admin', 'go-home'])

// 管理员凭证（后端每个接口都会重新校验）
const cred = computed(() => ({ name: props.admin.deity_name, spell: props.admin.spell }))
const adminName = computed(() => props.admin.myth_name || props.admin.deity_name)
const adminId = computed(() => props.admin.id)

/* ===== 用户列表 ===== */
const users = ref([])
const loading = ref(false)
const busy = ref(false)
const kw = ref('')
const pendingDelete = ref(null)

const filtered = computed(() => {
  const k = kw.value.trim().toLowerCase()
  if (!k) return users.value
  return users.value.filter(u =>
    (u.deity_name || '').toLowerCase().includes(k) ||
    (u.region || '').toLowerCase().includes(k)
  )
})

const adminCount = computed(() => users.value.filter(u => Number(u.permission) === 0).length)
const userCount = computed(() => users.value.filter(u => Number(u.permission) !== 0).length)
const todayCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return users.value.filter(u => (u.created_at || '').slice(0, 10) === today).length
})

async function load() {
  loading.value = true
  const d = await fetchAdminUsers(cred.value)
  loading.value = false
  if (d.ok && Array.isArray(d.users)) {
    users.value = d.users
  } else {
    toast(d.msg || '用户列表获取失败')
    // 凭证失效（如权限被其他管理员改回默认）→ 退回后台登录
    if (d.msg && d.msg.includes('无权')) emit('logout-admin')
  }
}

// 设为 / 取消管理员（0=管理员，1=默认用户）
async function togglePerm(u) {
  const target = Number(u.permission) === 0 ? 1 : 0
  busy.value = true
  const d = await updatePermission(cred.value, u.id, target)
  busy.value = false
  if (d.ok) {
    u.permission = target
    toast(target === 0
      ? `已将「${u.deity_name}」提升为管理员（权限 0）`
      : `已将「${u.deity_name}」恢复为默认用户（权限 1）`)
  } else {
    toast(d.msg || '权限变更失败')
  }
}

function askDelete(u) { pendingDelete.value = u }

async function doDelete() {
  const u = pendingDelete.value
  if (!u) return
  busy.value = true
  const d = await deleteAdminUser(cred.value, u.id)
  busy.value = false
  pendingDelete.value = null
  if (d.ok) {
    users.value = users.value.filter(x => x.id !== u.id)
    toast(`已删除用户「${u.deity_name}」`)
  } else {
    toast(d.msg || '删除失败')
  }
}

function goHome() { emit('go-home') }

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

/* ===== toast ===== */
const toastMsg = ref('')
const toastShow = ref(false)
let toastTimer = null
function toast(msg) {
  toastMsg.value = msg
  toastShow.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastShow.value = false }, 2200)
}

onMounted(load)
</script>

<style scoped>
.admin-home{position:fixed;inset:0;overflow-y:auto;
  background:
    radial-gradient(120% 80% at 80% 0%,rgba(233,196,106,.10),transparent 55%),
    radial-gradient(100% 90% at 10% 100%,rgba(45,212,191,.10),transparent 55%),
    linear-gradient(180deg,var(--ink2),var(--ink) 70%);
  color:var(--text)}

/* ===== 顶部栏 ===== */
.top{position:sticky;top:0;z-index:30;
  background:linear-gradient(180deg,rgba(7,11,22,.94),rgba(7,11,22,.6));
  backdrop-filter:blur(14px);border-bottom:1px solid var(--line);
  box-shadow:0 8px 30px rgba(0,0,0,.4)}
.top-inner{max-width:1240px;margin:0 auto;display:flex;align-items:center;justify-content:space-between;
  padding:14px 26px;gap:16px;flex-wrap:wrap}
.brand{display:flex;align-items:center;gap:12px}
.seal{width:42px;height:42px;border-radius:11px;
  background:linear-gradient(135deg,var(--gold),#a16207);
  display:grid;place-items:center;font-weight:900;color:#1c1300;font-size:20px;
  box-shadow:0 0 22px rgba(233,196,106,.55)}
.brand-txt b{font-size:17px;letter-spacing:3px;font-weight:900;display:block}
.brand-txt span{font-size:11px;color:var(--muted);letter-spacing:2px}
.top-right{display:flex;align-items:center;gap:12px;flex-wrap:wrap}
.who{font-size:13px;color:var(--muted);letter-spacing:1px}
.who b{color:var(--gold);margin-left:2px}
.t-btn{padding:8px 16px;border:1px solid var(--line);border-radius:999px;
  background:none;color:var(--muted);cursor:pointer;font-family:inherit;font-size:13px;
  letter-spacing:2px;transition:.15s;white-space:nowrap}
.t-btn:hover{color:var(--jade);border-color:var(--jade)}
.t-btn.danger:hover{color:var(--crimson);border-color:var(--crimson)}
.t-btn:disabled{opacity:.5;cursor:wait}

/* ===== 统计 ===== */
.stats{max-width:1240px;margin:30px auto 0;padding:0 26px;display:grid;
  grid-template-columns:repeat(4,1fr);gap:18px}
.stat{padding:22px 24px;border-radius:18px;
  background:linear-gradient(160deg,rgba(14,22,42,.7),rgba(7,11,22,.55));
  border:1px solid rgba(255,255,255,.10);
  box-shadow:0 18px 44px rgba(0,0,0,.42),inset 0 1px 0 rgba(255,255,255,.06)}
.stat-num{font-size:34px;font-weight:900;color:var(--jade);letter-spacing:2px;
  font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.stat.gold .stat-num{color:var(--gold)}
.stat.red .stat-num{color:var(--crimson)}
.stat-label{margin-top:6px;font-size:12px;color:var(--muted);letter-spacing:1px}

/* ===== 用户管理 ===== */
.board{max-width:1240px;margin:28px auto 40px;padding:0 26px}
.board-head{display:flex;align-items:center;justify-content:space-between;gap:14px;
  margin-bottom:18px;flex-wrap:wrap}
.bh-left{display:flex;align-items:center;gap:12px;flex-wrap:wrap}
.bh-ico{font-size:20px;color:var(--gold);text-shadow:0 0 14px var(--gold)}
.board-head h2{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:24px;
  letter-spacing:4px;font-weight:900}
.bh-tip{font-size:12px;color:var(--muted);letter-spacing:1px}
.bh-right{display:flex;align-items:center;gap:10px}
.kw{padding:9px 16px;min-width:210px;font-size:13px;
  background:rgba(7,11,22,.6);border:1px solid rgba(255,255,255,.14);border-radius:999px;
  color:var(--text);outline:none;font-family:inherit;transition:.2s}
.kw:focus{border-color:var(--jade);box-shadow:0 0 0 3px rgba(45,212,191,.18)}

.table-wrap{border-radius:18px;overflow:hidden;
  border:1px solid rgba(255,255,255,.10);
  background:linear-gradient(160deg,rgba(14,22,42,.7),rgba(7,11,22,.55));
  box-shadow:0 18px 44px rgba(0,0,0,.42)}
.tbl{width:100%;border-collapse:collapse;font-size:13.5px}
.tbl th{text-align:left;padding:14px 16px;letter-spacing:2px;font-size:12.5px;
  color:var(--gold);background:rgba(233,196,106,.08);
  border-bottom:1px solid rgba(233,196,106,.22);white-space:nowrap}
.tbl td{padding:13px 16px;border-bottom:1px solid rgba(255,255,255,.06);
  letter-spacing:.5px;vertical-align:middle}
.tbl tr:last-child td{border-bottom:none}
.tbl tr:hover td{background:rgba(45,212,191,.05)}
.tbl tr.self td{background:rgba(233,196,106,.07)}
.empty-cell{text-align:center;color:var(--muted);padding:44px 0;letter-spacing:2px}

.name-cell{font-weight:700;white-space:nowrap}
.dot{display:inline-block;width:8px;height:8px;border-radius:50%;margin-right:8px;
  box-shadow:0 0 8px currentColor}
.dot.jade{background:var(--jade);color:var(--jade)}
.dot.gold{background:var(--gold);color:var(--gold)}
.self-tag{margin-left:8px;font-size:10.5px;padding:2px 8px;border-radius:999px;
  color:var(--gold);border:1px solid rgba(233,196,106,.45);letter-spacing:1px}
.pwd-cell{color:var(--muted)}
.time-cell{color:var(--muted);white-space:nowrap}

.perm-tag{font-size:11.5px;letter-spacing:1px;padding:4px 11px;border-radius:999px;white-space:nowrap}
.perm-tag.admin{color:var(--gold);background:rgba(233,196,106,.12);
  border:1px solid rgba(233,196,106,.45)}
.perm-tag.user{color:var(--jade);background:rgba(45,212,191,.10);
  border:1px solid rgba(45,212,191,.40)}

.ops{white-space:nowrap}
.op{padding:7px 14px;margin-right:8px;border-radius:999px;cursor:pointer;
  font-family:inherit;font-size:12px;letter-spacing:1px;transition:.15s;
  border:1px solid rgba(45,212,191,.45);color:var(--jade);background:rgba(45,212,191,.08)}
.op:hover{background:rgba(45,212,191,.18)}
.op.gold{border-color:rgba(233,196,106,.5);color:var(--gold);background:rgba(233,196,106,.10)}
.op.gold:hover{background:rgba(233,196,106,.2)}
.op.ghost{border-color:var(--line);color:var(--muted);background:none}
.op.ghost:hover{color:var(--jade)}
.op.danger{border-color:rgba(239,68,84,.45);color:var(--crimson);background:rgba(239,68,84,.08)}
.op.danger:hover{background:rgba(239,68,84,.2)}
.op.danger.solid{background:linear-gradient(135deg,var(--crimson),#b91c1c);color:#fff;border:none}
.op:disabled{opacity:.5;cursor:wait}
.no-op{color:var(--muted)}

.foot{text-align:center;padding:22px;font-size:12px;color:var(--muted);
  letter-spacing:1px;border-top:1px solid rgba(255,255,255,.06)}

/* ===== 删除确认弹窗 ===== */
.modal{position:fixed;inset:0;z-index:80;display:grid;place-items:center;padding:24px;
  background:rgba(4,7,14,.66);backdrop-filter:blur(6px)}
.modal-card{width:100%;max-width:400px;padding:32px 30px 26px;border-radius:20px;
  background:linear-gradient(160deg,rgba(14,22,42,.97),rgba(7,11,22,.97));
  border:1px solid rgba(239,68,84,.4);
  box-shadow:0 30px 80px rgba(0,0,0,.6);animation:cardIn .35s ease both}
@keyframes cardIn{from{opacity:0;transform:translateY(14px) scale(.97)}to{opacity:1;transform:none}}
.modal-card h3{font-size:20px;letter-spacing:3px;margin-bottom:14px;color:var(--crimson)}
.modal-card p{font-size:14px;color:#cdd9ec;line-height:1.9;letter-spacing:.5px}
.modal-card p b{color:var(--gold)}
.modal-ops{display:flex;justify-content:flex-end;gap:10px;margin-top:24px}
.modal-ops .op{margin-right:0}

.fade-enter-active,.fade-leave-active{transition:opacity .25s}
.fade-enter-from,.fade-leave-to{opacity:0}

/* ===== toast ===== */
.toast{position:fixed;left:50%;top:84px;transform:translateX(-50%) translateY(-20px);
  background:linear-gradient(135deg,var(--jade-deep),var(--jade));
  color:#04130f;padding:12px 22px;border-radius:12px;font-weight:700;
  letter-spacing:1px;opacity:0;pointer-events:none;transition:.3s;z-index:90;
  box-shadow:0 14px 40px rgba(45,212,191,.45)}
.toast.show{opacity:1;transform:translateX(-50%) translateY(0)}

@media(max-width:980px){.stats{grid-template-columns:repeat(2,1fr)}}
@media(max-width:640px){
  .stats{grid-template-columns:repeat(2,1fr)}
  .tbl th:nth-child(4),.tbl td:nth-child(4),
  .tbl th:nth-child(6),.tbl td:nth-child(6){display:none}
  .kw{min-width:0;flex:1}
}
</style>
