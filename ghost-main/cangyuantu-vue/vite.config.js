import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    // 绑定全部网卡：localhost / 127.0.0.1 / 局域网实际 IP 都能访问，
    // 不写死 IP，避免电脑换 IP 后 Vite 启动失败。
    host: '0.0.0.0',
    port: 5173,
    open: true,
    // 将 /api 代理到本机 Spring Boot 后端（走回环地址，不依赖局域网 IP）
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8125',
        changeOrigin: true
      }
    }
  }
})
