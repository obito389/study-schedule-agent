import { http } from './client'
import type { ScheduleSnapshot } from '@/types/contract'

/** GET /api/v1/schedule/snapshot — 获取动作前的日程快照。 */
export async function getScheduleSnapshot(): Promise<ScheduleSnapshot> {

  const resp = await http.get<ScheduleSnapshot>('/schedule/snapshot')
  return resp.data
}
