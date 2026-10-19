# Minimum Complete Implementation Plan

**Goal:** 可持久保存、前后端连通且有测试的学习日程最小完整版本。

**Architecture:** 既有核心契约 + JPA/Flyway + 统一事务执行器 + REST + Vue 前端。

**Tech Stack:** Java 21 target、Spring Boot 3.5.16、H2/MySQL、Vue 3、TypeScript、Vite、Element Plus、FullCalendar。

## Global Constraints

- 固定本地 demo 用户；服务默认仅 localhost。Mock 模式显式标识。
- 四个冻结契约不变；Clock 注入；所有任务/时间块变更经 ActionExecutor。
- 不推送、不合并同学 PR。真实 MySQL 验证结果与 H2 分开记录。

## Task 1 数据与执行器

- [x] JPA 实体、Repository、Flyway V1、H2 文件及 MySQL profiles。
- [x] revision 行锁、任务动作、排程预览/确认、权限/幂等/硬约束、撤销。
- [x] 持久化课程/时间块/偏好/导入；完成耗时反馈与预览失效。
- [x] 数据库集成测试先覆盖一次完整写入与重复/冲突/回滚/撤销路径。

## Task 2 API

- [x] REST DTO、统一错误码、业务 API、导入确认持久化适配、Mock Agent 入口。
- [x] MockMvc 端点验证、失败状态码与输入校验。

## Task 3 前端

- [x] Vue/Vite 路由、API、类型、时间工具、日历及任务表单。
- [x] 导入校对、预览确认、执行回执与撤销、偏好/耗时页面。
- [x] 类型检查、Vitest、构建、Playwright 核心闭环。

## Task 4 交付

- [x] 完整自动化回归，数据库重启保留检查；可用则验证 MySQL。
- [x] 启动/测试脚本、compose、README、测试报告、演示步骤、进度更新。
- [x] 审查差异、提交、保留工作树及可查看的运行结果。
