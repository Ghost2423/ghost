<template>
  <div class="wrap">
    <!-- 登录 / 注册 双视图（无路由依赖，保持依赖最小化） -->
    <template v-if="view === 'auth'">
      <VisualPanel />
      <LoginPanel v-if="mode === 'login'" @go-register="mode = 'register'" @logged-in="onLoggedIn" />
      <RegisterPanel v-else @go-login="mode = 'login'" />
    </template>

    <!-- 首页（登录成功跳入） -->
    <HomePage
      v-else-if="view === 'home'"
      :key="homeKey"
      :avatar="avatar"
      @logout="onLogout"
      @open-admin="openAdmin"
      @open-cultivation="openCultivation"
      @open-forum="openForum"
    />

    <!-- 修炼殿（修炼系统：境界/神尊力/秘境/切磋/功法阁） -->
    <CultivationPanel
      v-else-if="view === 'cultivation'"
      :avatar="avatar"
      @go-home="backToHome"
    />

    <!-- 论道论坛（发帖 / 回帖 / 共鸣 / 板块检索 / 后台论道管理） -->
    <ForumPanel
      v-else-if="view === 'forum'"
      :avatar="avatar"
      @go-home="backToHome"
    />

    <!-- 后台管理系统 · 管理员登录（仅权限 permission=0 可登录成功；仅登录，不开放注册） -->
    <template v-else-if="view === 'admin-login'">
      <VisualPanel />
      <AdminLogin
        :initial-name="adminBackName"
        @admin-logged-in="onAdminLoggedIn"
        @back="backToHome"
      />
    </template>

    <!-- 后台管理系统 · 管理面板 -->
    <AdminPanel
      v-else-if="view === 'admin'"
      :admin="admin"
      @logout-admin="onAdminLogout"
      @go-home="backToHome"
      @update-admin="onAdminUpdate"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import VisualPanel from './components/VisualPanel.vue'
import LoginPanel from './components/LoginPanel.vue'
import RegisterPanel from './components/RegisterPanel.vue'
import AdminLogin from './components/AdminLogin.vue'
import HomePage from './views/HomePage.vue'
import AdminPanel from './views/AdminPanel.vue'
import CultivationPanel from './views/CultivationPanel.vue'
import ForumPanel from './views/ForumPanel.vue'

/* ===== 会话持久化：刷新页面不丢登录态、不丢后台位置 =====
 * 只存当前标签页（sessionStorage，关掉标签即清除）；管理员凭证按项目约定明文存储。 */
const SESSION_KEY = 'cy_session_v1'
const VALID_VIEWS = ['auth', 'home', 'cultivation', 'forum', 'admin-login', 'admin']
function readSession() {
  try { return JSON.parse(sessionStorage.getItem(SESSION_KEY) || '{}') || {} } catch (e) { return {} }
}
const saved = readSession()

// 登录 / 注册 双视图切换
const mode = ref(saved.mode === 'register' ? 'register' : 'login')

// 视图：'auth' 登录注册 | 'home' 首页 | 'cultivation' 修炼殿 | 'forum' 论道论坛 | 'admin-login' 后台登录 | 'admin' 后台管理
const view = ref(VALID_VIEWS.includes(saved.view) ? saved.view : 'auth')
// 当前神尊根基（来自后端 MySQL）
const avatar = ref(saved.avatar || null)
// 当前后台管理员（来自 /api/admin/login，permission=0）
const admin = ref(saved.admin || null)
// 后台登录页预填账号（从首页进入时带入当前账号，便于管理员快速登录）
const adminBackName = ref('')
// 首页强制刷新键：后台每次写库成功后 +1，切回首页必定重新读取数据库
const homeKey = ref(0)

// 恢复的视图必须有对应身份，否则退回登录页
if (view.value === 'admin' && !admin.value) view.value = 'auth'
if (['home', 'cultivation', 'forum'].includes(view.value) && !avatar.value) view.value = 'auth'

watch([view, avatar, admin, mode], () => {
  try {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify({
      view: view.value, avatar: avatar.value, admin: admin.value, mode: mode.value
    }))
  } catch (e) { /* 存储不可用时不影响使用 */ }
}, { deep: true })

function onDataChanged() { homeKey.value++ }
onMounted(() => window.addEventListener('cy-admin-data-changed', onDataChanged))
onBeforeUnmount(() => window.removeEventListener('cy-admin-data-changed', onDataChanged))

// 登录成功 → 进入首页
function onLoggedIn(av) {
  avatar.value = av
  view.value = 'home'
}

// 退出 → 回到登录
function onLogout() {
  avatar.value = null
  mode.value = 'login'
  view.value = 'auth'
}

// 首页导航栏「后台管理」按钮 → 进入后台登录页（常显入口，普通用户与游客亦可到达，登录时校验权限）
function openAdmin() {
  const a = avatar.value || {}
  // 仅当当前登录者本身是管理员（permission=0）时预填账号，避免普通用户误填
  adminBackName.value = (!a.isGuest && Number(a.permission) === 0) ? (a.deity_name || '') : ''
  view.value = 'admin-login'
}

// 首页「修炼」菜单 → 进入修炼殿（游客已被首页拦截，不会到达此处）
function openCultivation() {
  view.value = 'cultivation'
}

// 首页「论道论坛」菜单 → 进入论道坛（游客可浏览，写操作由前端与后端双重拦截）
function openForum() {
  view.value = 'forum'
}

// 管理员登录成功 → 进入后台管理面板
function onAdminLoggedIn(ad) {
  admin.value = ad
  view.value = 'admin'
}

// 管理员在「用户管理」里改了自己的账号或密码 → 立即换用新凭证，
// 否则后续接口仍拿旧 name/spell 请求会被 403 踢回登录页
function onAdminUpdate(u) {
  if (u && u.deity_name) admin.value = { ...(admin.value || {}), ...u }
}

// 退出后台 / 返回首页
function onAdminLogout() {
  admin.value = null
  view.value = 'admin-login'
}

// 后台返回首页（若已退出主站登录则回登录页）
function backToHome() {
  admin.value = null
  view.value = avatar.value ? 'home' : 'auth'
}
</script>
