<template>
  <section class="visual">
    <!-- 流光粒子 -->
    <div class="embers">
      <i v-for="(e, i) in embers" :key="i" :style="e"></i>
    </div>

    <div class="brand">
      <div class="seal">沧</div>
      <div><b>沧元图</b><span>CANG YUAN TU · 元界</span></div>
    </div>

    <div class="hero">
      <h1>沧元图</h1>
      <div class="line"></div>
      <p>元界动荡，妖魔横行。<br>执剑入元图，护一城烟火。<br>觉醒你的神尊，开启斩妖征途。</p>
      <div class="tags">
        <span class="tag">水墨仙侠</span>
        <span class="tag">剑气除妖</span>
        <span class="tag">孟川 · 柳七月</span>
        <span class="tag">腾讯视频国漫</span>
      </div>
    </div>

    <div class="foot-note">© 沧元图主题登录演示 · 仅作展示用途，非官方站点</div>
  </section>
</template>

<script setup>
const colors = ['#2dd4bf', '#e9c46a', '#ef4454', '#5eead4', '#ffffff']

// 复用原网页的粒子逻辑，用响应式数组渲染
const embers = Array.from({ length: 26 }, () => {
  const c = colors[Math.floor(Math.random() * colors.length)]
  const size = 2 + Math.random() * 3
  const duration = (7 + Math.random() * 8).toFixed(2)
  const delay = (-Math.random() * 10).toFixed(2)
  return {
    left: (Math.random() * 100).toFixed(2) + '%',
    background: c,
    boxShadow: `0 0 8px ${c}`,
    width: size + 'px',
    height: size + 'px',
    animation: `rise linear infinite ${duration}s ${delay}s`
  }
})
</script>
