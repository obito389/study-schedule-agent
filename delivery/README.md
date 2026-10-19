# 最终交付版

分支 codex/final-delivery，以组员独立版为工程基础，迁入组长已确认的清爽学习工作台前端设计。保留组员 16 表、Repository、执行器与撤销，以及主线契约、排程、画像和赵欢导入状态机。DeliveryCommands/DeliveryController 仅适配界面契约，未复制完整版的持久化层。

运行：MySQL 创建 final_delivery 后，通过 TEAM_DB_URL/TEAM_DB_USER/TEAM_DB_PASSWORD 指定连接；DEEPSEEK_API_KEY 可选。仓库根执行 ./backend/mvnw.cmd -f pom.xml -B package -DskipTests，运行 backend-hjh/target/study-planner-team-server-0.0.1-SNAPSHOT.jar，默认 8087。frontend 中 npm ci、npm run dev，默认 5181。

验证：指定独立 TEAM_TEST_DB_URL 后执行 Maven verify，输出两个模块 JaCoCo 覆盖率；frontend 执行 npm test、npm run build。浏览器测试使用 8088/5182 专用服务、独立 final_delivery_e2e 库和 8090 的 frontend/tests/fixtures/ai-provider.mjs，不使用真实模型作为断言来源。HTTP 黑盒及小样本性能：scripts/verify_delivery.py。

数据边界：图片/录音为上传加人工校对，文本通过 DeepSeek 提取，语音输入依赖浏览器能力。重要事项只保存在当前浏览器。登录仍沿用本地 X-User-Id 演示身份，尚未实现正式账号认证。完整报告和成员署名材料放在本地交付包中，不作为代码仓库的公开材料。

最后交付包包含源码、老师模板结构的团队完整报告、组长贡献章节、两位成员原始署名报告、实测证据、截图和讲解材料。报告不把历史阶段测试当作当前最终验收。
