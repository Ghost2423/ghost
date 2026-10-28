<template>
  <section class="panel admin-panel">
    <form class="card" autocomplete="off" @submit.prevent="onSubmit">
      <div class="ttl">元界天枢</div>
      <div class="sub">后台管理系统 · 仅限管理员权限（permission=0）登录</div>
      <div class="badge">ADMIN CONSOLE</div>
      <div class="divider"></div>

      <div class="field">
        <label for="admin-acc">管理员账号</label>
        <div class="control">
          <input id="admin-acc" type="text" v-model="acc" placeholder="请输入管理员账号" />
        </div>
      </div>

      <div class="field">
        <label for="admin-pwd">密码</label>
        <div class="control">
          <input id="admin-pwd" :type="showPwd ? 'text' : 'password'" v-model="pwd" placeholder="请输入密码" />
          <span class="ico" title="显示/隐藏" @click="showPwd = !showPwd">👁</span>
        </div>
        <div class="hint" :class="{ err: denied }">{{ denyHint }}</div>
      </div>

      <button class="btn" type="submit" :disabled="loading">{{ loading ? '验 证 中…' : '登 录 后 台' }}</button>

      <button class="btn-guest" type="button" @click="$emit('back')">返回首页</button>
    </form>

    <!-- 提示 toast -->
    <div class="toast" :class="{ show: toastShow }">{{ toastMsg }}</div>
  </section>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { adminLogin } from '../api'

const props = defineProps({
  // 从首页进入时可预填当前登录账号
  initialName: { type: String, default: '' }
})
const emit = defineEmits(['admin-logged-in', 'back'])

const acc = ref(props.initialName || '')
const pwd = ref('')
const showPwd = ref(false)
const loading = ref(false)
const denied = ref(false)

const DEFAULT_HINT = '普通用户（权限=1）无法登录后台管理系统。'
const denyHint = ref(DEFAULT_HINT)

const toastMsg = ref('')
const toastShow = ref(false)
let toastTimer = null

function toast(msg) {
  toastMsg.value = msg
  toastShow.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastShow.value = false }, 2400)
}

// 管理员登录 —— 走后端 /api/admin/login
// 后端校验：账号存在 → 密码（明文）比对 → permission 必须为 0，否则 403 拒绝
async function onSubmit() {
  const a = acc.value.trim()
  const p = pwd.value
  if (!a) return toast('请输入管理员账号')
  if (!p) return toast('请输入密码')

  denied.value = false
  denyHint.value = DEFAULT_HINT

  loading.value = true
  const d = await adminLogin(a, p)
  loading.value = false

  if (d.ok && d.admin) {
    toast('验证通过，正在进入后台管理系统…')
    setTimeout(() => emit('admin-logged-in', d.admin), 700)
  } else {
    // 403 = 账号密码正确但权限为默认用户
    if (d.msg && d.msg.includes('权限不足')) {
      denied.value = true
      denyHint.value = '该账号为默认用户（权限=1），无权进入后台管理系统。'
    }
    toast(d.msg || '登录失败')
  }
}

onBeforeUnmount(() => { if (toastTimer) clearTimeout(toastTimer) })
</script>

<style scoped>
.badge { text-align: center; margin-top: 14px; font-size: 12px; color: var(--gold);
  letter-spacing: 5px; text-shadow: 0 0 12px rgba(233, 196, 106, .45); }
.hint { font-size: 12px; color: var(--muted); margin-top: 6px; letter-spacing: .5px; line-height: 1.7; }
.hint.err { color: var(--crimson); }
.ico { position: absolute; right: 13px; top: 50%; transform: translateY(-50%);
  color: var(--muted); cursor: pointer; user-select: none; font-size: 14px; }
.ico:hover { color: var(--jade); }
.btn:disabled { opacity: .6; cursor: wait; }
</style>
