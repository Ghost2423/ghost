import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: true,
    // 将 /api 代理到本机 Spring Boot 后端（沧元界 · 神尊根基 API）
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8125',
        changeOrigin: true
      }
    }
  }
})
