<template>
  <section class="panel">
    <form class="card" id="loginForm" autocomplete="off" @submit.prevent="onSubmit">
      <div class="ttl">沧元界</div>
      <div class="sub">刀斩妖邪，笔绘众生 — 请觉醒你的神尊</div>
      <div class="divider"></div>

      <!-- 登录表单主体 -->
      <div id="formBody" v-show="!awakened">
        <div class="field">
          <label for="acc">神尊相 / 邮箱</label>
          <div class="control">
            <input id="acc" type="text" v-model="acc" placeholder="请输入神尊相或邮箱" />
          </div>
        </div>

        <div class="field">
          <label for="pwd">召唤语</label>
          <div class="control">
            <input id="pwd" :type="showPwd ? 'text' : 'password'" v-model="pwd" placeholder="请输入召唤语" />
            <span class="ico" id="toggle" title="显示/隐藏" @click="showPwd = !showPwd">👁</span>
          </div>
        </div>

        <div class="row">
          <label><input type="checkbox" id="remember" v-model="remember" /> 记住此身</label>
          <a href="#" id="forget" @click.prevent="onForget">遗忘召唤语？</a>
        </div>

        <button class="btn" type="submit">觉 醒</button>
      </div>

      <!-- 觉醒态 -->
      <div class="awaken" :class="{ show: awakened }">
        <div class="halo">
          <div class="portrait"><img :src="tianzunImg" alt="天尊·孟川" /></div>
          <span class="flash"></span>
        </div>
        <div class="name">天尊·孟川</div>
        <span class="ink-stroke"></span>
        <div class="desc">雷霆加护 · 万物臣服</div>
        <button class="reset" type="button" @click="onReset">重新封印</button>
      </div>
    </form>

    <!-- 提示 toast -->
    <div class="toast" :class="{ show: toastShow }">{{ toastMsg }}</div>
  </section>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'
import tianzunImg from '../assets/tianzun.jpg'

const SP_ACC = '九天应元雷声普化天尊'
const SP_PWD = '诸般雷霆，遵吾号令'

const acc = ref('')
const pwd = ref('')
const remember = ref(false)
const showPwd = ref(false)
const awakened = ref(false)

const toastMsg = ref('')
const toastShow = ref(false)
let toastTimer = null

function toast(msg) {
  toastMsg.value = msg
  toastShow.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastShow.value = false }, 2200)
}

// 觉醒（登录）
function onSubmit() {
  const a = acc.value.trim()
  const p = pwd.value
  if (a === SP_ACC && p === SP_PWD) {
    // 神尊相命中：显现天尊·孟川
    awakened.value = true
    toast('天尊降世 · 孟川觉醒')
    return
  }
  if (!a) return toast('请输入神尊相')
  if (p.length < 6) return toast('召唤语至少 6 位')
  toast('觉醒成功，正在进入沧元界…')
}

// 遗忘召唤语
function onForget() {
  toast('召唤语找回链接已发送至绑定邮箱')
}

// 重新封印（回到登录）
function onReset() {
  awakened.value = false
  acc.value = ''
  pwd.value = ''
}

onBeforeUnmount(() => { if (toastTimer) clearTimeout(toastTimer) })
</script>
