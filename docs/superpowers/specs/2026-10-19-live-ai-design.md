# 可演示的真实 AI 安排

用户已授权复用现有 DEEPSEEK_API_KEY，并要求在完整首版基础上优先体现真实 AI 运用，允许舍弃深入功能。本次选择单次模型理解 + 可确认草案；不做自动连续执行或多轮对话状态机。

- 使用已验证可访问的 DeepSeek deepseek-flash，JDK HttpClient 调用官方 JSON 输出接口。密钥仅从后端环境读取，不写入源码、前端或日志。
- 页面新增 AI 学习助手，接收一句自然语言，展示模型解释、需补充内容、结构化任务或时间块。
- 支持 CREATE_TASK（可带指定 start/end）、GENERATE_SCHEDULE（可限定 start/end 窗口和任务 targets）、QUERY_SCHEDULE。缺少任务必要信息返回 NEEDS_CLARIFICATION。不支持的复杂批量或破坏性动作明确要求缩小范围。
- 复用 ModelAdapter、AgentToolRegistry、HardConstraintValidator、ActionExecutor。AI 只产生候选，不直接写业务表。外部调用不占用数据库锁或事务；返回后比较 revision，变化则 409 并要求重新生成。
- 服务端保存独立 ai_draft，前端确认仅传 draftId；绑定的命令/operationKey/baseRevision 由服务器生成。重复确认只重放结果。不得从客户端替换 AI 草案内容。
- 新任务指定时间时允许在同一事务内创建任务和时间块；仍须未来、半小时对齐、截止/重叠/上限校验。无明确开始时间时只创建任务，再单独安排。
- 排程窗口作用于暂时快照，不改正式任务截止时间；其他任务的已有块仍参与冲突校验。候选服务端保存，确认继续按已有 APPLY_PLAN 执行。
- 模型超时、鉴权/额度/限流、空内容、非 JSON、越权动作等返回稳定错误码，不伪造 AI 成功。不把 API 错误正文或 key 放进错误消息。
- 默认测试使用本地 HTTP 假服务及注入模型，真实验证另外记录 provider/model/responseId，不执行未经确认的日程写入。

官方参考：https://api-docs.deepseek.com/guides/json_mode/ 。已安全调用 /models 确认 deepseek-flash 可用，未打印任何密钥。
