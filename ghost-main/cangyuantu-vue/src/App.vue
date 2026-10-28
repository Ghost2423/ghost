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
      :avatar="avatar"
      @logout="onLogout"
      @open-admin="openAdmin"
    />

    <!-- 后台管理系统 · 管理员登录（仅权限 permission=0 可登录成功） -->
    <AdminLogin
      v-else-if="view === 'admin-login'"
      :initial-name="adminBackName"
      @admin-logged-in="onAdminLoggedIn"
      @back="backToHome"
    />

    <!-- 后台管理系统 · 管理面板 -->
    <AdminPanel
      v-else-if="view === 'admin'"
      :admin="admin"
      @logout-admin="onAdminLogout"
      @go-home="backToHome"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import VisualPanel from './components/VisualPanel.vue'
import LoginPanel from './components/LoginPanel.vue'
import RegisterPanel from './components/RegisterPanel.vue'
import AdminLogin from './components/AdminLogin.vue'
import HomePage from './views/HomePage.vue'
import AdminPanel from './views/AdminPanel.vue'

// 登录 / 注册 双视图切换
const mode = ref('login')

// 视图：'auth' 登录注册 | 'home' 首页 | 'admin-login' 后台登录 | 'admin' 后台管理
const view = ref('auth')
// 当前神尊根基（来自后端 MySQL）
const avatar = ref(null)
// 当前后台管理员（来自 /api/admin/login，permission=0）
const admin = ref(null)
// 后台登录页预填账号（从首页进入时带入当前账号，便于管理员快速登录）
const adminBackName = ref('')

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

// 首页「后台管理」按钮（仅管理员渲染）→ 进入后台登录
function openAdmin() {
  adminBackName.value = (avatar.value && avatar.value.deity_name) || ''
  view.value = 'admin-login'
}

// 管理员登录成功 → 进入后台管理面板
function onAdminLoggedIn(ad) {
  admin.value = ad
  view.value = 'admin'
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
