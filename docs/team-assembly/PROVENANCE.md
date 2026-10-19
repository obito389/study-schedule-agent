# 独立组员拼装版

用户纠正：必须以组员提交为主体，缺失部分才参考完整版。

- 基线：origin/main；合入黄健华 PR 7 的真实 backend-hjh/ 实体、16表迁移、Repository、ActionExecutor 和 UndoExecutor。
- 前端：樊华钰 PR 6 原始提交 417dc8b，保留其 App、路由、页面、日历、动作预览及用户事实组件；直接适配真实接口。
- 核心：main 的稳定契约、排程、Agent、ProfileService；赵欢 PR 4 的 ImportService 与导入状态机。
- 运行方式：Maven reactor，backend 为核心依赖 jar，backend-hjh 为唯一启动服务。不会加载完整版持久化层或数据库迁移。
- 唯一从完整版借用的生产源码：DeepSeekModelAdapter.java（缺失的真实模型适配器）、ApiException.java（该适配器的错误类型）；引用 895c5ed。所有其他拼接层和补缺页面在此分支新写，不复制完整版页面。
- 独立端口：8085/5179；独立数据库：team_assembly。原完整版及先前整合分支保留。

数据库使用独立 `db/team-migration` 链，原始组员迁移脚本保持不变。独立 V1 副本删除建库和切库语句，避免越过连接所指定的数据库；原表结构保留。

验收：58 项主线核心测试、25 项原执行器回归、12 项新增真实 MySQL 集成测试均通过；前端类型检查及构建通过；2 项 Playwright 场景覆盖创建/排程/预览确认/撤销/导入/偏好/反馈与课程添加删除。另已验证真实 DeepSeek 创建建议。新分支 Git 祖先检查不含完整版备份提交。
