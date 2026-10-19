# PR #7 评审与修复记录

日期：2026-10-20。仓库：obito389/study-schedule-agent。
原始评审提交：d6cbe9f148fcc0b0d648c74b24fd040273f4f990。
作者：huaxiaozai（黄健华）；目标：main；独立实现位于 backend-hjh/。

## 当前结论

暂不合并到 main。本次修复执行器中已复现的缺陷，不表示两套后端已经完成集成，也不替代真实数据库验收。当前 Vue / DeepSeek 完整版及其前端工作树未被修改。

## 本次已修复

1. 预览校验不通过或版本过期时，不再模拟和输出实体快照，避免泄漏他人内容。有效预览在实体副本上模拟，避免改变 JPA 管理的对象。
2. 幂等键绑定完整有序请求的 SHA-256 指纹，包括版本、授权和草案。相同请求重试仍返回原结果；同键不同请求、已撤销的键及无法核对请求身份的旧记录均拒绝复用。V3 迁移新增可空 request_hash，不修改 V1/V2；旧键需要换新键，不能以新内容冒充重试。
3. 锁定块不能修改、删除，也不能通过更新 locked=false 绕过锁定保护。
4. 撤销先取得用户锁再读取审计；CREATE/UPDATE 的任务、步骤、时间块都比较当前对象和执行后快照，拒绝覆盖后续修改。比较忽略持久化时间戳，保留其他业务字段。
5. 失败审计取得相同的用户锁，不覆盖 APPLIED/UNDONE，也不将其他请求已使用的失败键重新绑定。

## 验证

- 原始版本的四个缺陷分别由独立测试复现；将断言改成正确行为后，修复前四项全部失败。
- 最终执行 backend-hjh 的 clean test：25 tests，0 failures，0 errors，0 skipped。
- 测试文件：backend-hjh/src/test/java/com/studyplanner/review/ExecutorReviewRegressionTest.java。
- Windows 命令（在 backend-hjh 内）：

```powershell
../backend/mvnw.cmd -f pom.xml -B clean test '-DargLine=-Djdk.net.unixdomain.tmpdir=C:/Users/Public'
```

上述为 JUnit/Mockito 回归测试，不包含 MySQL 验证。Docker CLI 存在，但本机 Docker 引擎未运行；未启动或修改现有数据库，也未执行 V3 实库迁移。Java 编译目标为 21；本机 Maven 运行时使用 Java 25，产生 Mockito 动态 agent 警告，不影响本次结果。

## 合并前仍需解决

- **P1：写入口未收敛。** TaskController、TaskStepController、TimeBlockController 直接操作 Repository，没有统一校验 baseRevision、硬约束或推进 planningRevision，仍可绕过执行器保护。
- **P1：撤销的逆操作未重新校验完整硬约束。** 快照一致只能防止覆盖同一对象的后续编辑；不能保证恢复旧时段后不与后来新建的其他时段冲突。
- **P1：删除父子记录后的撤销关联。** DELETE_* 撤销使用新主键，尚未实现整批父子引用重映射，不能认定关联数据可完整恢复。
- 默认校验器尚未与现有 backend/ 中排程共用的硬约束实现统一；每日上限、截止时间、课程冲突和 30 分钟槽位仍需联调验收。
- backend-hjh/ 与当前 backend/ 是两套独立 API、实体和迁移，不能直接替换当前运行版本或向同一数据库混跑迁移。
- 尚未验证真实 MySQL 的 Flyway V1→V3、锁竞争、事务整批回滚及重启恢复。
- PR 原描述中的 52 组手工测试和 77 份证据未包含在原提交中，无法从这次代码差异核验。

## 其他 PR（此前评审记录）

#6 为 yinyu2377 的前端提交，已关闭、未合并；#2 为旧关闭 PR。#1、#3、#4、#5 已合并。本次仅处理 #7，不改变其他 PR 状态。
