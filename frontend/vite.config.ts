import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 后端统一走 /api/v1，开发期代理到 Spring Boot (默认 8080)。
// 后端未就绪时前端启用 mock 回退（见 src/api/client.ts），不依赖代理。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '127.0.0.1',
    port: 5179,
    proxy: {
      '/api': {
        target: process.env.TEAM_API_URL ?? 'http://127.0.0.1:8085',
        changeOrigin: true
      }
    }
  }
})
