# 团队模块集成交付（2026-10-20）

## 保留与工作位置

- 原完整版源码备份：`codex/full-version-backup-20261020`，提交 `895c5ed5983d17b7df9021fec4bb3195e021a06c`，已推送 GitHub，包含当时未提交的日历改动。
- 原工作树 `review-team-prs/日程安排agent`、`codex/minimum-complete`、原来的未提交文件和 8080/5174 服务均未替换。
- 集成分支：`codex/team-modules-integrated`，基于 main，通过合并保留完整版的原始提交历史，新增统一执行与导入适配。
- 独立预览：http://127.0.0.1:5178/；后端及打包页面：http://127.0.0.1:8082/。
- 实库：本机独立 MySQL 8.0.46 进程，127.0.0.1:3308，`team_workflow_preview`；测试仅使用 `team_workflow_test`。现有 MySQL80 服务和原数据库未修改。

## 各成员代码如何接入

| 来源 | 本次采用 | 保留的边界 |
|---|---|---|
| main 中的核心模块 | 稳定契约、HardConstraintValidator、GreedyScheduler、AgentToolRegistry、ProfileService | 不改变既有契约字段和含义 |
| 赵欢 #4 | 原 ImportService、导入状态机、校对与确认端口 | 在端口层接数据库和真实文本模型；截图/音频目前仍人工填写 |
| 黄健华 #7 | 两阶段执行、批次原子性、版本串行化、内容绑定的幂等、审计和安全撤销要求 | 在现有 String ID / Instant 契约下移植；不加载另一套 Spring 应用、Long ID 实体或 16 表迁移，也不宣称额外 CRUD/步骤表已接入 |
| 樊华钰 #6，原提交 417dc8b | ConflictBanner、ActionStatusTag 及状态映射，接入现有排程预览和真实执行历史 | 不启用 USE_MOCK，不复制与真实接口不匹配的调用或伪造成功逻辑 |
| 用户完整版 | 数据库端口、真实 DeepSeek 适配、完整 Vue 页面、日历交互、自动测试和构建脚本 | 原版可单独恢复；当前源码只在独立集成工作树修改 |

#7 和 #6 的整个 PR 没有被宣称合入 main。本集成按稳定契约选择采用代码/语义，不通过同时运行两套数据库来拼接模块。

## 贯通的路径

1. UTF-8 文本上传 → DeepSeek 提取任务 → REVIEW → 校对确认 → ActionExecutor → MySQL → 任务清单。
2. 自然语言 → 真实模型 → 持久化 AI 草案 → 用户确认 → 执行器校验 → 日历。
3. 规则排程 → 候选预览 → 确认 → 同一硬约束实现 → 时间块。
4. 课程、固定时段、偏好 → CalendarService 兼容入口 → ActionExecutor → 版本/约束/审计。
5. 完成任务 → 执行器事务 → 实际耗时证据 → 耗时估计与偏好页面。
6. 批次预览 → 批次确认 → 一次版本推进；错误不写部分任务；同键变更内容返回 409；安全撤销再次检查当前任务镜像及全部硬约束。

课程、固定时段、偏好变更会生成审计但不提供自动撤销；任务完成的耗时反馈同样不提供历史按钮撤销。已有任务操作/排程批次可按当前规则撤销。失败回执不覆盖成功回执。

## 批次接口

`POST /api/v1/executor/preview` 和 `/apply` 共用请求：

```json
{
  "operationKey": "batch-client-generated-key",
  "baseRevision": 0,
  "actions": [
    {"type": "CREATE_TASK", "payload": {"title": "数据库作业", "deadline": "2026-10-23T14:00:00Z", "durationMinutes": 60, "priority": 3}},
    {"type": "CREATE_TASK", "payload": {"title": "软件工程复习", "deadline": "2026-10-23T14:00:00Z", "durationMinutes": 30, "priority": 3}}
  ]
}
```

日期仅为结构示例，实测时请使用未来日期。每批 1～50 个动作，支持 CREATE_TASK / UPDATE_TASK / CANCEL_TASK / MOVE_TASK / APPLY_PLAN。整批 operationKey 绑定有序内容和版本，重试必须原样；子动作不单独占用幂等键。COMPLETE_TASK、UNDO、课程和偏好不嵌套到此批次。

preview 返回 valid/violations/beforeTasks/afterTasks；无效预览不输出实体快照。apply 返回统一 ExecutionReceipt，成功一个批次推进一个版本。未增加能绕过版本和权限范围的直接 Repository 写接口。

## 数据库与验证

- Flyway V1/V2 保持原样，新 V3 只向 action_execution 追加完整日程 before/after 审计快照列。旧执行记录仍可读取。
- 后端完整 91 项测试在 H2、MySQL 8.0.46 上分别通过，0 失败、0 错误、0 跳过。覆盖并发版本冲突、幂等、批次失败不部分写入、课程/固定时段/偏好审计和撤销恢复旧时段时的新课程冲突。
- 前端类型检查和生产构建通过，Vitest 17 项通过。最终完整 17 项 Playwright 浏览器测试通过，包含冲突后的刷新重试、文本导入、批次提交和日历各档位。
- 真实 DeepSeek 两条人工 API 验收通过：文本导入提取任务，确认前任务数 0→0；确认后任务与排程真实保存；完成后产生一条实际耗时证据。AI 创建带时段任务也生成 NEEDS_CONFIRMATION 并经确认保存。
- 强制结束独立 8082 后端后重新启动：2 个任务、版本 4、4 条执行记录、1 条耗时证据全部一致，重试原排程命令返回相同 executionId，没有重复写入。
- 预览中的数据均为“整合验收”测试数据，不是从原完整版数据库搬来的个人日程。

## 本机复现

```powershell
# 仓库根目录。需要已安装 Java、Node 和本机 MySQL 8 二进制。
./scripts/start-mysql-sandbox.ps1
./backend/mvnw.cmd -f backend/pom.xml test
./scripts/test-mysql-integration.ps1
cd frontend
npm ci
npm run build
npm test
cd ..
./backend/mvnw.cmd -f backend/pom.xml package -Pweb -DskipTests
./scripts/start-integrated-preview.ps1
# 另一个终端可启用热更新：
$env:PLANNER_API_URL='http://127.0.0.1:8082'
cd frontend
npm run dev -- --port 5178 --strictPort
```

DeepSeek 凭据继续仅来自环境变量 DEEPSEEK_API_KEY，模型/地址沿用原配置；未写入 Git、日志或证据。缺少密钥时规则排程、手工校对和正式保存仍可使用，AI 请求明确提示未配置。

MySQL sandbox helper 专供本机隔离验收，使用只绑定回环地址的独立数据目录与测试账户配置，不替代生产部署配置。它会验证端口归属，不覆盖现有服务。自动测试会清空指定的专用测试库业务表，绝不指向原库。正式部署请使用 application-mysql.yml 的独立账号/环境变量。

## 仍有明确限制

- 截图 OCR、音频文件 ASR 尚未接识别服务；上传后人工校对的保存路径已接通。浏览器按住说话能力继续由浏览器语音 API 提供。
- 当前是单用户本机应用，没有多用户登录或外部日历同步。
- 黄健华的额外 16 表 CRUD、任务步骤及学期等功能没有伪装成当前产品已使用的功能。
- 月历重要事项仍沿用当前前端的本机浏览器存储；任务、课程、日历时间块、导入、AI 草案、偏好与执行历史保存到 MySQL。
- Vite 构建仍有 Element Plus 分包体积提示，不影响本次构建/运行。
