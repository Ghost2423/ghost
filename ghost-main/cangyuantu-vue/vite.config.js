import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    // 绑定局域网地址，手机/其他电脑可用 http://192.168.68.50:5173 访问
    host: '192.168.68.50',
    port: 5173,
    open: true,
    // 将 /api 代理到本机 Spring Boot 后端（沧元界 · 神尊根基 API）
    proxy: {
      '/api': {
        target: 'http://192.168.68.50:8125',
        changeOrigin: true
      }
    }
  }
})
