# 最小完整交付设计

用户确认最小完整版本并授权立即开发，沿用 agent.md 的模块化单体及稳定契约。

## 交付范围

单人本地课程设计演示，固定用户 demo；绑定本地地址，不提供公网多用户服务。Vue 3/TypeScript/Vite/Element Plus/FullCalendar 前端；Java/Spring Boot 后端。课程、任务、锁定/禁止时段、排程预览与确认、动作撤销、用户偏好、耗时反馈、导入校对形成闭环。模型能力以明确标识的离线 Mock 运行；不冒充真实识别或云端智能能力。

## 数据与事务

Spring Data JPA + Flyway。默认文件 H2 用于本地可复现运行；MySQL 8 profile 和 compose 提供正式数据库路径，两者均须明确测试状态。用户日程 revision 行用悲观锁串行化写入。业务字段明确存列；动作 before/after 快照和 ProfileFact 等证据保存 JSON。

ActionExecutor 是任务和时间块修改唯一事务入口，负责权限 scope、幂等键绑定、版本冲突、硬约束、写入及可撤销镜像。成功、冲突和业务失败记录确定回执。排程预览由服务器保存，确认只接受 previewId + operationKey + baseRevision；执行时重新校验，不信任客户端块。

课程、锁定时段、偏好修改同样锁 revision 并使预览过期。禁止与已有任务冲突。撤销采用执行后镜像比较及逆变更硬约束校验，恢复被影响任务，递增版本。完成任务清除未来安排并记录耗时样本，重复回执不重复计样本。

导入任务存数据库，确认端口以 proposalId 幂等创建任务，课程名称不当作 courseId；Mock 不处理上传内容，校对页面必须补齐字段并提示。原契约字段保持兼容，新增 API DTO 独立定义。

## 前端和验收

日历、任务、导入、偏好/反馈四个入口；预览和写入分开，失败及版本冲突显式反馈。使用固定 seed 样例供手动演示，时间以 Asia/Shanghai 展示。

JUnit/MockMvc 覆盖实际 H2 迁移、事务写入、冲突、幂等、撤销、导入及耗时；Vitest 覆盖时间/状态映射，Playwright 覆盖浏览器核心闭环；运行类型检查和构建。独立记录 MySQL 验证及真实模型缺口。

## 开发边界

工作树 codex/minimum-complete 基于已修复 PR #4 的本地提交；不合并组员 PR、不推送。按 persistence/business、web API、frontend、测试交付分批提交。CodeGraph 未安装，使用 rg 分析调用点。coding-agent 的 OpenClaw 通知环境不可用，不启动外部后台 worker，本会话直接完成。
