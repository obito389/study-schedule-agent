# 学习日程前端

Vue 3 + TypeScript + Vite + Element Plus + FullCalendar。后端默认 localhost:8080，Vite `/api` 代理到该地址；可用 `PLANNER_API_URL` 改变代理目标。

```powershell
npm ci
npm run dev
npm run typecheck
npm test
npm run build
```

开发地址 http://127.0.0.1:5173。路由使用 hash，生产构建可内嵌 Spring Boot JAR。

运行浏览器验收前先在 backend 运行 `mvnw.cmd package`，再执行 `npm run test:e2e`。Playwright 自动启动独立 8081/5174 服务及内存数据库，不影响日常演示数据。

全部时间以北京时间展示，后端传输 Instant。前端不自作正式写入；预览和确认分开，网络结果未知时保留原动作幂等键。Mock 来源在导入页面明确说明。

组件与后端约定见 `../docs/delivery/api.md`。当前 5 项 Vitest 覆盖跨日期的时区换算、非法时间及日历事件类型映射；2 条 Playwright 用例覆盖完整主流程。
