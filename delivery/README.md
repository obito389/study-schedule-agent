# 学习日程 Agent 最终交付版

本分支 `codex/final-delivery` 以组员独立拼装版为工程基础，迁入组长确认的前端整体设计。保留组员 16 表、Repository、动作执行与撤销，以及主线契约、排程、画像和赵欢导入状态机；适配层负责接通页面，不复制原完整版的持久化层。原完整版、备份分支和组员独立版继续保留。

## 启动

需要 JDK 21 或更高版本、Node.js 22、MySQL 8。首次解压后在项目根目录打开 PowerShell：

1. 创建独立数据库：`CREATE DATABASE final_delivery CHARACTER SET utf8mb4;`。不要指向原版数据库；首次启动由 Flyway 初始化表。
2. 按自己的数据库设置环境变量：

```powershell
$env:TEAM_DB_URL='jdbc:mysql://127.0.0.1:3306/final_delivery?serverTimezone=Asia/Shanghai&characterEncoding=UTF-8'
$env:TEAM_DB_USER='你的数据库用户'
$env:TEAM_DB_PASSWORD='你的数据库密码'
# 可选：从自己已有的配置安全设置 DEEPSEEK_API_KEY，不要写入代码或提交仓库。
./backend/mvnw.cmd -f pom.xml -B package -DskipTests
java -jar backend-hjh/target/study-planner-team-server-0.0.1-SNAPSHOT.jar
```

3. 另开终端，在同一项目目录执行：

```powershell
cd frontend
npm ci
npm run dev
```

后端默认 8087，前端默认 5181，打开 http://127.0.0.1:5181/ 。本机本次验收数据库使用 3308；换电脑时应显式设置上面的连接地址。服务只监听本机。未提供模型密钥时，规则排程仍可用，模型入口会显示未配置。

## 测试复现

- 新建独立空库 `final_delivery_test`，设置 `TEAM_TEST_DB_URL` 后，在根目录执行 `./backend/mvnw.cmd -f pom.xml -B verify`。测试库会清理测试数据，禁止指定演示或生产库。两个模块的 `target/site/jacoco/index.html` 是可浏览的白盒覆盖率。
- `cd frontend` 后执行 `npm test` 和 `npm run build`。
- 浏览器和外部黑盒需要独立服务：数据库 `final_delivery_e2e`、后端 8088、前端 5182、固定模型夹具 8090。夹具启动命令是 `node frontend/tests/fixtures/ai-provider.mjs`。
- 后端测试实例启动参数：`--server.port=8088 --spring.datasource.url=jdbc:mysql://127.0.0.1:3308/final_delivery_e2e?serverTimezone=Asia/Shanghai --team.ai.base-url=http://127.0.0.1:8090 --team.ai.api-key=e2e-fixture`（按自己的 MySQL 端口修改）。
- 前端测试实例：在 frontend 中设置 `$env:PLANNER_API_URL='http://127.0.0.1:8088'`，执行 `npm run dev -- --port 5182`。安装浏览器 `npx playwright install chromium`，再执行 `npx playwright test`。
- 外部 HTTP 黑盒及性能：在根目录执行 `python scripts/verify_delivery.py`，默认访问专用 8088 服务，使用 Python 标准库。

本轮结果见 [测试记录](TEST_RESULTS.md)。浏览器用固定模型夹具作确定性断言，真实 DeepSeek 连通单独记录，二者不混用。

## 交付材料与边界

本地完整包包含 `delivery/reports` 下最终 Word 报告、`delivery/materials` 下成员原始署名报告及组长旧稿、`delivery/evidence` 实测证据、真人讲解视频和口语版讲解稿（自动配音旧版另行保留）。完整署名报告、学号和视频放在本地交付包，不推送到公开代码仓库。报告生成脚本需要本地材料及本轮构建证据；运行应用不依赖这些材料。

图片/录音目前为上传加人工校对；文本可由模型提取；语音输入依赖浏览器。月历重要事项只存当前浏览器。身份沿用本地 `X-User-Id` 演示方式，尚无正式认证。服务重启恢复检查被自动审批阻止，没有计入通过项；已完成数据库实查、事务回滚和浏览器刷新恢复检查。当前推荐视频采用组长本人录音和真实界面截图，约 5 分 40 秒。原始录音保持不变，只在本地处理；原始音频不推入代码仓库。旧自动配音版继续保留。

生成真人讲解版交付包：`python scripts/package_delivery.py --human`。源码、报告和证据与原交付版保持一致，视频改用本人录音。剪辑脚本 `scripts/build_human_video.py` 的时间点对应本次录音，换录音需要重新对齐；需 Pillow 和 imageio-ffmpeg，依赖位于本机隔离的 .runtime/video-deps，不随包提交。
