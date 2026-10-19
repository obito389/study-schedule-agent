import { http, USE_MOCK, ApiError } from './client'
import type {
  ActionSpec,
  UndoRequest,
  UndoResponse
} from '@/types/contract'
import { ActionStatus } from '@/types/contract'
import { mockActionSpec } from './mockData'

/**
 * POST /api/v1/actions — 请求 Agent 生成动作预览。
 * 返回的 ActionSpec.status 可能是：
 *   NEEDS_CLARIFICATION → 需要用户补充信息
 *   NEEDS_CONFIRMATION  → 需要用户确认后才执行
 *   APPLIED / FAILED    → 只读查询类结果或失败
 */
export async function requestAction(
  prompt: string,
  baseRevision: number
): Promise<ActionSpec> {
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 300))
    // 模拟一条待确认的移动任务动作
    return { ...mockActionSpec, baseRevision }
  }
  const resp = await http.post<ActionSpec>('/actions', { prompt, baseRevision })
  return resp.data
}

/**
 * POST /api/v1/actions/{operationKey}/confirm — 用户确认后执行动作。
 * 若 baseRevision 过期，后端返回 409 REVISION_CONFLICT。
 */
export async function confirmAction(action: ActionSpec): Promise<ActionSpec> {
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 300))
    // 模拟一次版本冲突：第二次确认时 baseRevision 已过期
    if (action.baseRevision < 7) {
      throw new ApiError(
        'REVISION_CONFLICT',
        '日程已发生变化，请刷新预览后重试',
        ['schedule-user-001'],
        409
      )
    }
    return { ...action, status: ActionStatus.APPLIED }
  }
  const resp = await http.post<ActionSpec>(
    `/actions/${action.operationKey}/confirm`,
    action
  )
  return resp.data
}

/**
 * POST /api/v1/actions/undo — 基于执行记录撤销。
 * 对应 samples/contracts/action-undo-flow.json。
 */
export async function undoAction(req: UndoRequest): Promise<UndoResponse> {
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 300))
    return {
      httpStatus: 200,
      executionId: 'execution-002',
      reversedExecutionId: req.executionId,
      operationKey: req.operationKey,
      actionStatus: ActionStatus.APPLIED,
      planningRevision: req.baseRevision + 1
    }
  }
  const resp = await http.post<UndoResponse>('/actions/undo', req)
  return resp.data
}
