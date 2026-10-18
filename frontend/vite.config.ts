import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: process.env.PLANNER_API_URL ?? "http://127.0.0.1:8080",
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          calendar: [
            "@fullcalendar/core",
            "@fullcalendar/vue3",
            "@fullcalendar/timegrid",
            "@fullcalendar/daygrid",
          ],
          ui: ["element-plus"],
          vue: ["vue", "vue-router"],
        },
      },
    },
  },
  test: { include: ["src/**/*.test.ts"] },
});
