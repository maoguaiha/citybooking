import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// API 基址走同源 /api，开发态通过 dev proxy 转发到后端 8080，避免 CORS。
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:18100',
        changeOrigin: true,
      },
    },
  },
})
