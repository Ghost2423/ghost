<template>
  <div class="adm">
    <!-- ===== 侧边栏 ===== -->
    <aside class="adm-side">
      <div class="adm-brand">
        <div class="adm-seal">枢</div>
        <div class="adm-brand-txt">
          <b>元界天枢</b>
          <span>ADMIN CONSOLE</span>
        </div>
      </div>

      <nav class="adm-menu">
        <button v-for="m in menus" :key="m.key"
                :class="{ on: tab === m.key }" @click="tab = m.key">
          <span class="mi">{{ m.icon }}</span>
          <span class="mt">{{ m.label }}</span>
        </button>
      </nav>

      <div class="adm-side-foot">
        <div class="adm-who">当前管理员：<b>{{ adminName }}</b></div>
        <button class="adm-btn" @click="$emit('go-home')">返回首页</button>
        <button class="adm-btn danger" @click="$emit('logout-admin')">退出后台</button>
      </div>
    </aside>

    <!-- ===== 主区 ===== -->
    <div class="adm-main">
      <header class="adm-top">
        <h1>
          {{ current.label }}
          <small>{{ current.desc }}</small>
        </h1>
        <div class="adm-top-right">
          <span class="adm-tag gold">{{ current.tag }}</span>
          <button class="adm-btn sm" @click="goHome">首页预览 ↗</button>
        </div>
      </header>

      <div class="adm-body">
        <component :is="current.comp" :cred="cred" :admin-id="admin.id" @toast="toast" @go="tab = $event" />
      </div>
    </div>

    <div class="adm-toast" :class="{ show: toastShow, err: toastErr }">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import OverviewTab from './admin/OverviewTab.vue'
import CodexTab from './admin/CodexTab.vue'
import SectionTab from './admin/SectionTab.vue'
import NewsTab from './admin/NewsTab.vue'
import SettingTab from './admin/SettingTab.vue'
import UserTab from './admin/UserTab.vue'
import RegisterTab from './admin/RegisterTab.vue'
import LogTab from './admin/LogTab.vue'

const props = defineProps({
  // 管理员实体（来自 /api/admin/login，permission=0）
  admin: { type: Object, required: true }
})
const emit = defineEmits(['logout-admin', 'go-home'])

// 管理员凭证：后端每个接口都会重新校验
const cred = computed(() => ({ name: props.admin.deity_name, spell: props.admin.spell }))
const adminName = computed(() => props.admin.myth_name || props.admin.deity_name || '管理员')

const menus = [
  { key: 'overview', icon: '◈', label: '数据概览', desc: '首页数据总览与最近操作', tag: 'OVERVIEW', comp: OverviewTab },
  { key: 'codex', icon: '❖', label: '图鉴管理', desc: '神尊 / 功法 / 妖魔 / 秘境条目', tag: 'CODEX', comp: CodexTab },
  { key: 'section', icon: '✦', label: '核心界域', desc: '首页四大展示框配置', tag: 'SECTIONS', comp: SectionTab },
  { key: 'news', icon: '✉', label: '元界动态', desc: '首页滚动动态条', tag: 'NEWS', comp: NewsTab },
  { key: 'setting', icon: '✎', label: '站点文案', desc: '品牌名 / 首屏标题 / 热门标签', tag: 'COPY', comp: SettingTab },
  { key: 'users', icon: '☰', label: '用户管理', desc: '注册用户与后台权限', tag: 'USERS', comp: UserTab },
  { key: 'registers', icon: '✦', label: '注册记录', desc: '每次注册的账号留痕', tag: 'REGISTERS', comp: RegisterTab },
  { key: 'logs', icon: '✧', label: '操作日志', desc: '后台操作审计记录', tag: 'LOGS', comp: LogTab }
]

const tab = ref('overview')
const current = computed(() => menus.find(m => m.key === tab.value) || menus[0])

function goHome() { emit('go-home') }

/* ===== toast ===== */
const toastMsg = ref('')
const toastShow = ref(false)
const toastErr = ref(false)
let toastTimer = null

function toast(msg, isErr = false) {
  toastMsg.value = msg
  toastErr.value = !!isErr
  toastShow.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastShow.value = false }, 2400)
}

/* 管理员凭证失效（后端 403）→ 提示后退回后台登录页，避免停留在无权限界面 */
let failTimer = null
function onAuthFail(e) {
  toast(e.detail || '管理员权限已失效，请重新登录', true)
  if (failTimer) clearTimeout(failTimer)
  failTimer = setTimeout(() => emit('logout-admin'), 1200)
}

onMounted(() => window.addEventListener('cy-admin-auth-fail', onAuthFail))
onBeforeUnmount(() => {
  window.removeEventListener('cy-admin-auth-fail', onAuthFail)
  if (toastTimer) clearTimeout(toastTimer)
  if (failTimer) clearTimeout(failTimer)
})
</script>

<style src="../styles/admin.css"></style>
