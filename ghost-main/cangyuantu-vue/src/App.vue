<template>
  <div class="wrap">
    <!-- 登录 / 注册 双视图（无路由依赖，保持依赖最小化） -->
    <template v-if="view === 'auth'">
      <VisualPanel />
      <LoginPanel v-if="mode === 'login'" @go-register="mode = 'register'" @logged-in="onLoggedIn" />
      <RegisterPanel v-else @go-login="mode = 'login'" />
    </template>

    <!-- 首页（登录成功跳入） -->
    <HomePage v-else :avatar="avatar" @logout="onLogout" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import VisualPanel from './components/VisualPanel.vue'
import LoginPanel from './components/LoginPanel.vue'
import RegisterPanel from './components/RegisterPanel.vue'
import HomePage from './views/HomePage.vue'

// 登录 / 注册 双视图切换
const mode = ref('login')

// 视图：'auth' 登录注册 | 'home' 首页
const view = ref('auth')
// 当前神尊根基（来自后端 MySQL）
const avatar = ref(null)

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
</script>
