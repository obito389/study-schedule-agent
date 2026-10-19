import { http, USE_MOCK } from './client'
import type { ScheduleSnapshot } from '@/types/contract'
import { mockScheduleSnapshot } from './mockData'

/** GET /api/v1/schedule/snapshot — 获取动作前的日程快照。 */
export async function getScheduleSnapshot(): Promise<ScheduleSnapshot> {
  if (USE_MOCK) {
    // 模拟网络延迟，便于页面展示加载态
    await new Promise((r) => setTimeout(r, 200))
    return mockScheduleSnapshot
  }
  const resp = await http.get<ScheduleSnapshot>('/schedule/snapshot')
  return resp.data
}
