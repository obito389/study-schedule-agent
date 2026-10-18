# 前后端交接接口

固定本地用户 demo；时间字段为 ISO 8601 Instant，UI 用 Asia/Shanghai。成功响应直接返回 DTO，错误结构为 `{code,message}`；写动作失败回执使用 `errors: ContractError[]`。现有四个 contract 保持不变。

| 方法 | 路径 | 作用 |
|---|---|---|
| GET | /api/v1/health | 运行模式与健康状态 |
| GET | /api/v1/schedule | ScheduleSnapshot，含 planningRevision |
| POST | /api/v1/planning/previews | 生成服务端保存的候选，返回 previewId/baseRevision/candidate |
| POST | /api/v1/actions | 确认写动作，返回 ExecutionReceipt |
| GET | /api/v1/actions | 执行历史，含当前是否可撤销 |
| POST | /api/v1/actions/{executionId}/undo | 新 operationKey + 当前 baseRevision |
| POST / DELETE | /api/v1/courses[/{id}] | 课程及单次课程时段 |
| POST / DELETE | /api/v1/blocks[/{id}] | 禁止或个人固定时段 |
| GET | /api/v1/profile | 生效事实、历史及 profileVersion |
| POST | /api/v1/profile/preferences | 每日上限设置、事实否认或删除 |
| GET | /api/v1/profile/duration-estimate?originalMinutes=60 | 平滑耗时建议、样本数与证据 |
| POST | /api/v1/imports | multipart file/sourceType，X-Request-Key 幂等 |
| GET | /api/v1/imports/{requestKey} | 导入状态和校对提案 |
| POST | /api/v1/imports/{requestKey}/confirm | 校对后的完整 ImportProposal |

## 写动作封装

```json
{
  "operationKey": "客户端生成的唯一键",
  "baseRevision": 0,
  "type": "CREATE_TASK",
  "authorizationScope": "schedule:write:self",
  "payload": {
    "title": "完成实验报告",
    "deadline": "2026-10-21T14:00:00Z",
    "durationMinutes": 60,
    "priority": 3,
    "courseId": null
  }
}
```

| type | payload |
|---|---|
| CREATE_TASK | title/deadline/durationMinutes/priority/courseId? |
| UPDATE_TASK | taskId + 同 CREATE_TASK；保留现有时间块并重新校验 |
| CANCEL_TASK | taskId |
| COMPLETE_TASK | taskId/actualMinutes |
| MOVE_TASK | taskId/blockId/start/end；保持原时长 |
| APPLY_PLAN | previewId；不接受客户端自造候选块 |
| UNDO | executionId |

此封装是应用层命令，未扩展或改写冻结的 ActionType。原 Agent 继续输出 ActionSpec，应用层确认 API 接收明确的用户操作。

相同 operationKey + 语义相同请求返回原回执，不再次递增 revision；改变内容返回 409 DUPLICATE_OPERATION_MISMATCH。失败/冲突回执也会保存；修改内容或刷新后重新操作须用新键。网络结果未知时重试同键和原请求。

确认排程前请展示 gaps；可保存完整排入的任务，缺口任务不会偷偷截短。HTTP 409 表示需刷新或重新预览，422 表示硬约束失败，400 为参数错误，403 为权限 scope 错误。

课程 body：`{name,start,end,baseRevision}`。固定时段 body：`{title,start,end,unavailable,baseRevision}`。删除使用 query `baseRevision`。课程被任务引用时拒绝删除。偏好 body：`{key,value,action,baseRevision}`，action 为 SET/DELETE/DENY，SET 仅支持 `planning.dailyLimitMinutes`。
