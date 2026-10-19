import { http } from './client'
import type {
  ActionSpec,
  UndoRequest,
  UndoResponse
} from '@/types/contract'
import { getScheduleSnapshot } from './schedule'

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

  const resp = await http.post<ActionSpec>('/actions', { prompt, baseRevision })
  return resp.data
}

/**
 * POST /api/v1/actions/{operationKey}/confirm — 用户确认后执行动作。
 * 若 baseRevision 过期，后端返回 409 REVISION_CONFLICT。
 */
export async function confirmAction(action: ActionSpec): Promise<ActionSpec> {

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

  const current = await getScheduleSnapshot()
  const resp = await http.post<UndoResponse>('/actions/undo', { ...req, baseRevision: current.planningRevision })
  return resp.data
}

export async function requestRulePlan(baseRevision: number): Promise<ActionSpec> {
  return (await http.post<ActionSpec>('/actions/plan', { baseRevision })).data
}
