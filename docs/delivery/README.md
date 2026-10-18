# 学习日程 Agent · 最小完整交付

本版本面向本地课程设计演示，包含前端、后端、数据库迁移及测试。固定用户为 demo，默认仅监听 127.0.0.1。截图/录音导入采用明确标识的 Mock，不包含真实 OCR/ASR 或外部模型接入。

## 最快启动：单个 JAR

需要 Java 21 或以上。本机实测 JDK 25.0.2；编译目标 Java 21。

在包含 `study-planner.jar` 的目录执行：

```powershell
$javaTempPath = Join-Path $env:PUBLIC 'studyplanner-java-tmp'
New-Item -ItemType Directory -Path $javaTempPath -Force | Out-Null
java "-Djdk.net.unixdomain.tmpdir=$javaTempPath" -jar study-planner.jar
```

访问 http://127.0.0.1:8080。前端已包含在 JAR 中。数据保存在当前工作目录的 `data/` 下，关闭后重新运行会保留。请固定运行目录；不要删除 data 目录。Windows 的独立 ASCII 临时目录参数用于规避本机 JDK 25 在中文用户目录中启动 NIO 时的路径错误。

## 从源码启动

环境：Java 21+、Node.js 22.12+、npm。Maven 使用仓库中的 Wrapper。

在项目根目录打开两个 PowerShell 终端：

```powershell
# 终端一，后端 8080
powershell -ExecutionPolicy Bypass -File scripts/start-backend.ps1

# 终端二，开发前端 5173
powershell -ExecutionPolicy Bypass -File scripts/start-frontend.ps1
```

访问 http://127.0.0.1:5173。默认无需 Docker、数据库账号或模型 Key；使用本地文件 H2。

## MySQL 8

已有 MySQL 时创建独立数据库及用户，设置环境变量后启动：

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/studyplanner?serverTimezone=UTC&characterEncoding=UTF-8'
$env:DB_USER = 'studyplanner'
$env:DB_PASSWORD = '<本机数据库密码>'
powershell -ExecutionPolicy Bypass -File scripts/start-backend.ps1 -MySql
```

也可在设置 `DB_PASSWORD` 和 `MYSQL_ROOT_PASSWORD` 后使用 `docker compose up -d`，等待数据库健康后再启动后端。若 3306 已被现有服务占用，请使用现有独立数据库，或调整 compose 端口和 DB_URL。Flyway 自动建表，Hibernate 只校验结构。

## 测试与构建

```powershell
# 后端完整测试、前端单测和构建
powershell -ExecutionPolicy Bypass -File scripts/test-all.ps1

# 加跑 Chromium 浏览器端到端测试，使用独立内存库及端口 8081/5174
powershell -ExecutionPolicy Bypass -File scripts/test-all.ps1 -Browser

# 验证服务重启后数据和幂等回执保留，端口 8082，独立测试文件库
powershell -ExecutionPolicy Bypass -File scripts/verify-persistence.ps1

# 构建内嵌前端的单 JAR，输出 delivery/study-planner.jar
powershell -ExecutionPolicy Bypass -File scripts/build-delivery.ps1
```

完整测试报告见 `docs/delivery/test-report.md`。端口已被占用时请关闭对应测试实例或修改脚本中的端口；不要停止不属于本项目的服务。

## 五分钟演示

1. 打开“任务清单”，新建“完成数据库实验报告”，截止设为明晚 22:00，预计 60 分钟，确认保存。
2. 点击“生成排程预览”，检查两段 30 分钟候选，此时日历尚未写入。
3. 点击“确认保存到日历”，进入“学习日历”查看安排。点击时间块可预览并确认移动；移动到冲突或超过截止时间时会拒绝。
4. 点击“执行记录”，撤销刚才的“保存排程”，确认时间块消失。已有后续任务修改时会拒绝不安全撤销。
5. 到“导入与校对”上传任意演示图片；页面明确显示 Mock。补齐任务名称、截止及耗时，确认后创建真实任务。
6. 到“偏好与反馈”设置每日上限；回到任务清单完成任务并填写实际耗时。累计三个有效样本后可查看平滑耗时建议；否认/删除证据后重算。建议不会自动覆盖旧任务。
7. 关闭并重新启动后端，确认任务与执行历史仍保留。

课程和固定时段在日历右上角添加，时间必须对齐半小时。当前课程按单次具体日期录入，固定安排不会自动展开为每周重复。

## 本次范围与后续交接

- 实现任务创建、修改、取消、完成反馈；课程/禁止/个人时段添加与删除；排程预览、确认和时间块移动；版本冲突、幂等与保守撤销；导入校对、偏好和证据历史。
- 提交端口已从内存替换为数据库事务，正式任务/时间块变更统一经过 ActionExecutor。导入课程名称只作证据，不自动绑定正式 courseId。
- 排程为确定性规则，现有 Agent 核心及 Mock 测试保留。没有自然语言聊天界面、真实多模态识别、登录和公网多用户隔离。
- 撤销比较整组任务镜像；有其他任务后续变化时也可能拒绝撤销，优先确保数据安全。任务完成及耗时反馈不提供撤销。
- 初版不包含通知推送、跨端同步、周期课程自动展开或复杂拖拽交互。
- 组员源码到齐后，优先对接既有 contract、ImportModelPort、ImportCommitPort 及 `/api/v1` 接口，保留当前验收用例。

## 技术参考

- Spring 事务回滚规则：https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html
- Vue 工程与构建：https://vuejs.org/guide/quick-start.html
- Flyway H2 支持：https://documentation.red-gate.com/flyway/reference/database-driver-reference/h2
