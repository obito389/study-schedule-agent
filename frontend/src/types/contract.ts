/**
 * 前端类型与后端 Java record / 枚举一一对应。
 * 契约版本固定为 1；带时刻字段统一为 ISO-8601 UTC 字符串；
 * 用户时区使用 IANA Zone ID（如 Asia/Shanghai）。
 *
 * 对应后端包：com.studyplanner.contract
 */

export const CURRENT_SCHEMA_VERSION = 1

// ---------- 枚举（与 Java enum 同名同值） ----------

export enum ActionStatus {
  NEEDS_CLARIFICATION = 'NEEDS_CLARIFICATION',
  NEEDS_CONFIRMATION = 'NEEDS_CONFIRMATION',
  APPLIED = 'APPLIED',
  FAILED = 'FAILED'
}

export enum ActionType {
  QUERY_SCHEDULE = 'QUERY_SCHEDULE',
  GENERATE_SCHEDULE = 'GENERATE_SCHEDULE',
  CREATE_TASK = 'CREATE_TASK',
  MOVE_TASK = 'MOVE_TASK',
  CANCEL_TASK = 'CANCEL_TASK'
}

export enum TaskStatus {
  TODO = 'TODO',
  IN_PROGRESS = 'IN_PROGRESS',
  DONE = 'DONE',
  CANCELLED = 'CANCELLED'
}

export enum ProfileFactState {
  ACTIVE = 'ACTIVE',
  DENIED = 'DENIED',
  DELETED = 'DELETED',
  EXPIRED = 'EXPIRED'
}

export enum TimeBlockKind {
  COURSE = 'COURSE',
  TASK = 'TASK',
  PERSONAL = 'PERSONAL',
  UNAVAILABLE = 'UNAVAILABLE'
}

export enum ImportStatus {
  UPLOADED = 'UPLOADED',
  PROCESSING = 'PROCESSING',
  REVIEW = 'REVIEW',
  COMMITTED = 'COMMITTED',
  FAILED = 'FAILED',
  EXPIRED = 'EXPIRED'
}

// ---------- 公共错误结构 ----------

/** 对应 ContractError record。 */
export interface ContractError {
  code: string
  message: string
  objectIds: string[]
}

// ---------- 时间块 ----------

/**
 * 对应 TimeBlock record。
 * start/end 为 ISO-8601 UTC 字符串；locked=true 表示不可移动。
 */
export interface TimeBlock {
  id: string
  ownerId: string
  start: string
  end: string
  locked: boolean
  kind: TimeBlockKind
}

// ---------- 课程与任务快照 ----------

/** 对应 CourseSnapshot record。meetingBlocks 为课程固定时间。 */
export interface CourseSnapshot {
  id: string
  name: string
  meetingBlocks: TimeBlock[]
}

/** 对应 TaskSnapshot record。deadline 为 ISO-8601 UTC 字符串。 */
export interface TaskSnapshot {
  id: string
  courseId: string
  title: string
  deadline: string
  estimatedDurationMinutes: number
  priority: number // 1..5
  status: TaskStatus
  scheduledBlocks: TimeBlock[]
}

// ---------- 四大稳定契约 ----------

/**
 * 对应 ScheduleSnapshot record。
 * planningRevision 是动作乐观锁基线，每次成功执行后递增。
 */
export interface ScheduleSnapshot {
  schemaVersion: number
  now: string
  timezone: string
  courses: CourseSnapshot[]
  tasks: TaskSnapshot[]
  lockedBlocks: TimeBlock[]
  activeFacts: ProfileFact[]
  planningRevision: number
}

/**
 * 对应 ActionSpec record。
 * operationKey 用于幂等；baseRevision 用于版本冲突校验；
 * changes 为结构化 JSON 对象。Agent 只生成该对象，不直接写库。
 */
export interface ActionSpec {
  schemaVersion: number
  operationKey: string
  baseRevision: number
  type: ActionType
  targets: string[]
  changes: Record<string, unknown>
  authorizationScope: string
  reasonFacts: string[]
  status: ActionStatus
  errors: ContractError[]
}

/**
 * 对应 ProfileFact record。
 * value 为结构化 JSON；strength 范围 0.0..1.0；version 从 1 递增。
 */
export interface ProfileFact {
  schemaVersion: number
  key: string
  value: unknown
  scope: string
  strength: number
  source: string
  state: ProfileFactState
  evidenceIds: string[]
  version: number
}

// ---------- 动作执行回执与冲突 ----------

/**
 * 对应 samples/contracts/action-conflict-response.json。
 * 过期 baseRevision 时后端返回 HTTP 409。
 */
export interface ActionConflictResponse {
  httpStatus: 409
  operationKey: string
  actionStatus: ActionStatus // 通常为 FAILED
  baseRevision: number
  currentRevision: number
  retryable: boolean
  error: ContractError
}

/** 对应 samples/contracts/action-undo-flow.json 的 request 部分。 */
export interface UndoRequest {
  operationKey: string
  executionId: string
  baseRevision: number
}

/** 对应 samples/contracts/action-undo-flow.json 的 response 部分。 */
export interface UndoResponse {
  httpStatus: number
  executionId: string
  reversedExecutionId: string
  operationKey: string
  actionStatus: ActionStatus
  planningRevision: number
}

// ---------- 页面展示辅助映射 ----------

/**
 * 后端 ActionStatus 到前端展示文案的映射。
 * 编码约定要求：页面必须区分处理中、待补充、待确认、已执行和失败，
 * 不能把处理中显示为成功。
 */
export const ACTION_STATUS_LABEL: Record<ActionStatus, string> = {
  [ActionStatus.NEEDS_CLARIFICATION]: '待补充',
  [ActionStatus.NEEDS_CONFIRMATION]: '待确认',
  [ActionStatus.APPLIED]: '已执行',
  [ActionStatus.FAILED]: '失败'
}

export const ACTION_STATUS_TAG_TYPE: Record<ActionStatus, 'info' | 'warning' | 'success' | 'danger'> = {
  [ActionStatus.NEEDS_CLARIFICATION]: 'info',
  [ActionStatus.NEEDS_CONFIRMATION]: 'warning',
  [ActionStatus.APPLIED]: 'success',
  [ActionStatus.FAILED]: 'danger'
}

export const ACTION_TYPE_LABEL: Record<ActionType, string> = {
  [ActionType.QUERY_SCHEDULE]: '查询日程',
  [ActionType.GENERATE_SCHEDULE]: '生成排程',
  [ActionType.CREATE_TASK]: '创建任务',
  [ActionType.MOVE_TASK]: '移动任务',
  [ActionType.CANCEL_TASK]: '取消任务'
}

export const PROFILE_FACT_STATE_LABEL: Record<ProfileFactState, string> = {
  [ProfileFactState.ACTIVE]: '生效',
  [ProfileFactState.DENIED]: '已否认',
  [ProfileFactState.DELETED]: '已删除',
  [ProfileFactState.EXPIRED]: '已过期'
}

export const PROFILE_FACT_STATE_TAG_TYPE: Record<ProfileFactState, 'success' | 'warning' | 'info' | 'danger'> = {
  [ProfileFactState.ACTIVE]: 'success',
  [ProfileFactState.DENIED]: 'warning',
  [ProfileFactState.DELETED]: 'info',
  [ProfileFactState.EXPIRED]: 'danger'
}
