<template>
  <!-- 全站通用背景层：固定在页面最底层，不拦截点击 -->
  <div class="page-backdrop"
       :style="{ backgroundImage: `linear-gradient(180deg, rgba(7,11,22,.42) 0%, rgba(7,11,22,.74) 52%, rgba(7,11,22,.95) 100%), url('${src}')` }">
  </div>
</template>

<script setup>
defineProps({
  src: { type: String, required: true }
})
</script>

<style scoped>
.page-backdrop {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background-size: cover, cover;
  background-position: center 35%, center 35%;
  background-repeat: no-repeat;
}
</style>
