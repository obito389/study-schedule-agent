# 最小完整版本测试记录

测试日期：2026-10-19（Asia/Shanghai）。本报告对应 codex/minimum-complete 工作分支。

## 环境

- Windows、JDK 25.0.2，Maven Wrapper 3.9.11，Java 编译目标 release 21。
- Spring Boot 3.5.16 / Hibernate 6.6.53 / Flyway，H2 2.3.232。
- MySQL Community Server 8.0.46，独立本地测试实例 3307，与用户现有 MySQL80 服务隔离。
- Node.js 24.18.0 / npm 11.16.0；依赖精确版本见 frontend/package-lock.json。
- Playwright Chromium，桌面 1440×1040、手机 390×844。

## 结果

| 检查 | 结果 |
|---|---|
| 后端完整测试，H2 MySQL 兼容模式 | 84 项通过，0 失败 |
| 后端完整测试，真实 MySQL 8.0.46 | 同一套 84 项通过，0 失败；其中 13 项为原数据库/API 集成测试，新增 13 项 AI 适配及集成测试 |
| 前端 Vitest 时间与事件映射 | 5 项通过 |
| 前端 vue-tsc 类型检查 + Vite 生产构建 | 通过 |
| Playwright 浏览器完整流程 | 6 项通过，包含 3 项新增语音交互用例及 444×706 小屏布局验证 |
| H2 文件库重启持久化 | 通过：任务、revision 和原执行回执均保留，重放不重复写入 |
| npm audit --omit=dev（官方 registry） | 运行依赖 0 条已知漏洞；这是当次扫描结果 |
| 内嵌前端 JAR 构建 | 通过，提供独立打包脚本 |

两次数据库回归运行同一套测试，不能相加宣称为 168 个不同用例。测试日志保存在 backend/target/ai-full-test.log 和 ai-mysql-test.log；前端日志与截图在 frontend/test-results 和 ai-e2e-check.log。target/node_modules 等生成目录不入库。

## 验证内容

- 契约 JSON 往返、非法字段、稳定排序、截止/重叠/锁定/禁止时段/每日上限、缺口回滚、Agent Mock 动作与用户模型原有测试。
- Flyway 从空库建表、Hibernate 结构验证、任务持久化、服务端排程预览未写入直到确认、幂等重放、重复键内容冲突、过期版本、并发写入串行化。
- 权限 scope 校验、无效任务、移动超过截止时回滚、已安排时长不能被缩短、撤销恢复、后续任务变化拒绝撤销。
- 偏好变更使预览失效；不满足已有日程的新上限回滚；完成反馈仅记录一次；删除耗时证据后统计重算。
- 上传只生成待校对提案，未完成校对不能入库，确认后只创建一次正式任务；回执丢失后的提交重试不重复写入。
- 强制结束完整 Java 进程树后重启：最初检查暴露 H2 默认 500 ms 延迟写盘会丢失最近提交；默认文件库已改为 WRITE_DELAY=0，再验证任务、revision 及幂等回执保留。脚本会真正结束父子进程，避免 Oracle java 启动器留下子进程而造成假重启。
- 浏览器：建任务→预览→确认→日历→撤销→完成反馈；Mock 上传→校对→入库→保存偏好；手机布局无横向溢出。

## 已知边界

真实 DeepSeek 已单独调用验证，记录见 live-ai-verification.json。自动化 AI 测试使用本地假服务，不计为真实模型调用。测试不证明真实 OCR/ASR、公开部署或多用户鉴权能力；这些未纳入本次最小完整范围。实际 JDK 21 运行未单独验收，已验证 release 21 编译及 JDK 25 运行。Element Plus 完整组件包产生约 940 KB 的 UI 分块（gzip 约 302 KB），构建有体积提示，不影响本地功能验收。后续优化可以按需引入组件。

H2 写盘设置参考：[SET WRITE_DELAY](https://h2database.com/html/commands.html#set_write_delay)。

## 独立复现 MySQL 测试

创建专用测试库，确保其中没有需要保留的数据。本测试会清空该库的项目业务表。

```powershell
cd backend
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3307/studyplanner_acceptance?serverTimezone=UTC'
$env:SPRING_DATASOURCE_USERNAME = '<测试数据库用户>'
$env:SPRING_DATASOURCE_PASSWORD = '<测试数据库密码>'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'com.mysql.cj.jdbc.Driver'
.\mvnw.cmd -B test
```

测试资源默认指向独立 H2 内存库。MySQL 验收必须显式指向独立测试库，不能复用演示或个人业务数据库。
