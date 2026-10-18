# 学习日程 Agent

这是一个面向课程设计与期末答辩的学习日程 Agent。初版目标是在五周内完成截图和语音输入、人工校对、规则排程、Agent 动作生成、应用内日历执行、耗时反馈和用户偏好管理。

## 技术方案

- 后端：Java 21、Spring Boot 3.5.x、Spring AI 1.1.x、Spring Data JPA。
- 数据库：MySQL 8、Flyway。
- 前端：Vue 3、TypeScript、Vite、Element Plus、FullCalendar。
- 架构：模块化单体；Agent 只生成结构化动作，正式写入统一经过事务执行器。

详细技术栈、编码规范和模块边界见 [agent.md](agent.md)。熊翔负责模块的任务拆分、进度和交接安排见 [planning.md](planning.md)。

## 团队协作建议

建议每位成员都采用 `planning.md` 的做法管理自己的工作：把负责内容拆成可以单独验收的小任务，并持续记录状态、依赖、交接对象、验收命令和下一步计划。这样能尽早暴露接口不一致和联调阻塞，也便于答辩前确认每项功能是否真正完成。

为避免多人同时修改根目录的 `planning.md`，推荐保留它作为熊翔的主计划和示例，其他成员在 `plans/` 目录创建自己的计划：

```text
plans/
  赵欢.md
  黄健华.md
  樊华钰.md
```

每份成员计划至少包含：

1. 当前任务及预计工时。
2. 可检查的子任务和完成状态。
3. 输入依赖、输出接口和交接对象。
4. 验收方法、测试结果和已知问题。
5. 下一步准备完成的工作。

完成一项功能时，应同时提交实现、测试、配置说明和可复现样例，并更新对应计划文件。

## 当前状态

2026-10-18 已形成最小完整交付首版：Vue 前端、JPA/Flyway 数据持久化、事务执行器、课程/任务/固定时段、导入校对、排程预览与确认、日历、偏好、耗时反馈和安全撤销均已接通。默认本地单用户，导入识别明确使用 Mock。H2 与真实 MySQL 8 上均通过 71 项后端测试，另有 5 项前端单测、2 条浏览器端到端用例及服务重启持久化验收。

## 启动与验收

在项目根目录打开两个终端：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-backend.ps1
powershell -ExecutionPolicy Bypass -File scripts/start-frontend.ps1
```

后端 http://127.0.0.1:8080，开发前端 http://127.0.0.1:5173。默认文件数据库无需账号，数据保留在 `backend/data`。单 JAR 交付运行时不需要 Node；用 `scripts/build-delivery.ps1` 打包，访问同一个后端地址即可使用内嵌前端。

完整启动、MySQL 配置、五分钟演示和范围说明见 [交付说明](docs/delivery/README.md)，验收证据见 [测试报告](docs/delivery/test-report.md)。当前没有真实 OCR/ASR、云模型或多用户公网鉴权，后续可接组员实现。

## 文档

- [项目技术和编码约定](agent.md)
- [熊翔开发计划](planning.md)
- [多模态导入接口样例](samples/importing/README.md)
- [前后端 API 交接](docs/delivery/api.md)
