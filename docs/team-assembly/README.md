# 组员独立拼装版

此分支 `codex/team-assembly-independent` 从主线核心开始，合入黄健华后端及樊华钰前端原始提交，再编写跨模块适配层。没有合入 `codex/minimum-complete` 或其备份提交。完整版保存在 `codex/full-version-backup-20261020`，此前以完整版为基础的 PR #8 也不在本分支中。

| 部分 | 实际运行代码 |
| --- | --- |
| 页面、导航、日历、状态标签、动作预览、用户事实组件 | 樊华钰 `417dc8b` 的 `frontend/`，去掉模拟接口并适配真实后端 |
| 16 表、实体、Repository、批量执行、审计、批量撤销 | 黄健华 `d6cbe9f` 的 `backend-hjh/`，保留 PR 审查修复 |
| 契约、Agent 工具、贪心排程、硬约束、耗时估计 | 主线 `backend/` |
| 上传、待校对、确认和失败重试状态机 | 赵欢提交的 `ImportService` |
| 真实模型调用 | 仅从完整版借用 `DeepSeekModelAdapter` 与其 `ApiException` |
| 缺失连接 | 新写 `backend-hjh/.../team/`；补上任务管理、导入校对页面和课程设置对话框 |

执行链路：组员 Vue 页面 → 契约适配接口 → 主线排程/画像/导入模块 → 组员 ActionExecutor/UndoExecutor → 组员 MySQL 实体。模型只提出建议，不直接写库；确认时执行已保存的草案，忽略客户端篡改内容。任务和草案、审计、反馈、事实、统计、导入记录均写入组员表。

## 启动

需要 Java 21、Node 22 和独立 MySQL 8 数据库。预先建库，不要使用完整版数据库：

```sql
CREATE DATABASE team_assembly CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

PowerShell 在仓库根目录运行：

```powershell
$env:TEAM_DB_URL='jdbc:mysql://127.0.0.1:3308/team_assembly?serverTimezone=Asia/Shanghai&characterEncoding=UTF-8'
$env:TEAM_DB_USER='root'
$env:TEAM_DB_PASSWORD='' # 填写你自己的独立数据库凭据
# 可选：通过环境变量配置 DEEPSEEK_API_KEY；不配置仍可手工建任务和规则排程。
./backend/mvnw.cmd -f pom.xml -B package -DskipTests
java '-Djdk.net.unixdomain.tmpdir=C:/Users/Public' -jar backend-hjh/target/study-planner-team-server-0.0.1-SNAPSHOT.jar
```

另一个终端：

```powershell
cd frontend
npm ci
npm run dev
```

默认后端 8085，前端 5179；`TEAM_API_URL` 可覆盖 Vite 代理目标。首次迁移创建本地演示用户 1，前端沿用组员的 `X-User-Id: 1` 身份方式。此版本尚未加入正式登录。

组员原始 `db/migration` 脚本原样保留。本服务使用独立的 `db/team-migration` 副本：V1 保留 16 表定义，只移除了写死的 `CREATE DATABASE/USE studyplanner`，V2/V3 沿用原 SQL，V4 增加契约保存列与独立演示用户。此分支必须在新库首次迁移；不要对已有 Flyway 历史的其它版本数据库直接切换。

## 验证

```powershell
# 指定另一个专用测试数据库；不要填预览库。
$env:TEAM_TEST_DB_URL='jdbc:mysql://127.0.0.1:3308/team_assembly_test?serverTimezone=Asia/Shanghai'
./backend/mvnw.cmd -f pom.xml -B test
npm --prefix frontend run build
```

未设置 `TEAM_TEST_DB_URL` 时 MySQL 集成测试跳过，其余原有测试仍执行。CI 自动提供 MySQL 测试库。

端到端测试需将服务连接到第三个专用库 `team_assembly_e2e` 并运行于 8086；前端设置 `TEAM_API_URL=http://127.0.0.1:8086` 运行于 5180，然后在 `frontend` 中执行 `npx playwright install chromium`、`npm run test:e2e`。测试覆盖任务、排程确认/撤销、完成反馈、上传校对、偏好和课程添加/删除，不访问完整版端口。

## 当前边界

- 保留组员 Element Plus 页面风格，没有移植完整版的布局、日历定制和交互设计。
- 自然语言支持创建单个任务、规则排程、查询；任务取消可在任务页操作。图片和录音只接入上传与人工校对，尚无 OCR/ASR。
- 课程设置提供每周课程与单日不可用时段；组员原有单双周数据可读取。课程添加界面未提供单双周选项，步骤已有执行器接口但未提供独立前端编辑器。
- 原始 CRUD 写入口禁用，防止绕过统一版本和硬约束；保留读取接口，新建/修改通过工作台或执行器完成。
- 没有迁入完整版的历史日程、设置或用户数据。
