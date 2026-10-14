<template>
  <section class="panel">
    <form class="card" id="loginForm" autocomplete="off" @submit.prevent="onSubmit">
      <div class="ttl">沧元界</div>
      <div class="sub">刀斩妖邪，笔绘众生 — 请觉醒你的神尊</div>
      <div class="divider"></div>

      <!-- 登录表单主体 -->
      <div id="formBody" v-show="!awakened">
        <div class="field">
          <label for="acc">账号</label>
          <div class="control">
            <input id="acc" type="text" v-model="acc" placeholder="请输入注册时的账号" />
          </div>
        </div>

        <div class="field">
          <label for="pwd">密码</label>
          <div class="control">
            <input id="pwd" :type="showPwd ? 'text' : 'password'" v-model="pwd" placeholder="请输入密码" />
            <span class="ico" id="toggle" title="显示/隐藏" @click="showPwd = !showPwd">👁</span>
          </div>
        </div>

        <div class="row">
          <label><input type="checkbox" id="remember" v-model="remember" /> 记住此身</label>
          <a href="#" id="forget" @click.prevent="onForget">遗忘召唤语？</a>
        </div>

        <button class="btn" type="submit" :disabled="loading">{{ loading ? '觉 醒 中…' : '觉 醒' }}</button>

        <button class="btn-guest" type="button" @click="guestLogin">游 客 入 界</button>

        <div class="switch">尚无根基？<a href="#" @click.prevent="$emit('go-register')">前往锻造神尊根基 ›</a></div>
      </div>

      <!-- 觉醒态（数据来自后端 MySQL） -->
      <div class="awaken" :class="{ show: awakened }">
        <div class="halo">
          <div class="portrait"><img :src="tianzunImg" alt="神尊" /></div>
          <span class="flash"></span>
        </div>
        <div class="name">{{ avatarName }}</div>
        <span class="ink-stroke"></span>
        <div class="desc">{{ avatarDesc }}</div>
        <div class="myth" v-if="avatarMyth">{{ avatarMyth }}</div>
        <button class="reset" type="button" @click="onReset">重新封印</button>
      </div>
    </form>

    <!-- 提示 toast -->
    <div class="toast" :class="{ show: toastShow }">{{ toastMsg }}</div>
  </section>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { loginAvatar } from '../api'
import tianzunImg from '../assets/tianzun.jpg'

const emit = defineEmits(['go-register', 'logged-in'])

const acc = ref('')
const pwd = ref('')
const remember = ref(false)
const showPwd = ref(false)
const awakened = ref(false)
const loading = ref(false)

const avatarName = ref('')
const avatarDesc = ref('')
const avatarMyth = ref('')

const toastMsg = ref('')
const toastShow = ref(false)
let toastTimer = null

function toast(msg) {
  toastMsg.value = msg
  toastShow.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastShow.value = false }, 2200)
}

// 觉醒（登录）—— 走后端 /api/login，由 MySQL 神尊根基校验
async function onSubmit() {
  const a = acc.value.trim()
  const p = pwd.value
  if (!a) return toast('请输入神尊相')
  if (!p) return toast('请输入召唤语')

  loading.value = true
  const d = await loginAvatar(a, p)
  loading.value = false

  if (d.ok && d.avatar) {
    const av = d.avatar
    avatarName.value = av.deity_name
    avatarDesc.value = (av.myth_name ? av.myth_name + ' · ' : '') + (av.region || '') + ' 之神'
    avatarMyth.value = av.myth_story || ''
    awakened.value = true
    toast('觉醒成功，正在进入沧元界…')
    // 觉醒态短暂呈现后，跳转首页（携带神尊根基数据）
    setTimeout(() => emit('logged-in', av), 1300)
  } else {
    toast(d.msg || '觉醒失败')
  }
}

// 遗忘召唤语
function onForget() {
  toast('召唤语找回链接已发送至绑定邮箱')
}

// 游客免注册登录 —— 不落库，进入首页后身份为「游历散修」
function guestLogin() {
  if (loading.value) return
  const titles = ['游历散修', '云游道友', '无名过客', '闲云野鹤', '踏雪寻梅', '听雨客']
  const base = titles[Math.floor(Math.random() * titles.length)]
  const suffix = Math.floor(Math.random() * 9000 + 1000)
  const av = {
    deity_name: base + '·' + suffix,
    myth_name: '散修',
    region: '无所属',
    myth_story: '未入元界册籍的游方之士，凭一缕道心踏遍山河。此身不系于门派，唯以眼观万象、以心记苍生。',
    isGuest: true
  }
  toast('以游客之身踏入沧元界…')
  setTimeout(() => emit('logged-in', av), 800)
}

// 重新封印（回到登录）
function onReset() {
  awakened.value = false
  acc.value = ''
  pwd.value = ''
}

onBeforeUnmount(() => { if (toastTimer) clearTimeout(toastTimer) })
</script>

<style scoped>
.switch { text-align: center; margin-top: 18px; font-size: 13px; color: var(--muted); letter-spacing: 1px; }
.switch a { color: var(--jade); text-decoration: none; margin-left: 4px; }
.switch a:hover { text-decoration: underline; }
.awaken .myth { color: var(--muted); font-size: 12.5px; margin: 16px auto 0; max-width: 312px;
  line-height: 1.95; letter-spacing: .5px; border-top: 1px solid var(--line); padding-top: 14px; }
.btn:disabled { opacity: .6; cursor: wait; }
</style>
