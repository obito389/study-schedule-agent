import { http } from './client'
import type { ProfileFact } from '@/types/contract'
import { getScheduleSnapshot } from './schedule'


/** GET /api/v1/profile/facts — 列出用户事实（含各状态）。 */
export async function listProfileFacts(): Promise<ProfileFact[]> {

  const resp = await http.get<ProfileFact[]>('/profile/facts')
  return resp.data
}

/** PUT /api/v1/profile/facts/{key} — 修改事实值。 */
export async function updateProfileFact(
  fact: ProfileFact
): Promise<ProfileFact> {

  const current = await getScheduleSnapshot()
  const resp = await http.put<ProfileFact>(
    `/profile/facts/${encodeURIComponent(fact.key)}`,
    { ...fact, baseRevision: current.planningRevision }
  )
  return resp.data
}

/** POST /api/v1/profile/facts/{key}/deny — 用户否认该事实。 */
export async function denyProfileFact(key: string, version?: number): Promise<ProfileFact> {

  const current = await getScheduleSnapshot()
  const resp = await http.post<ProfileFact>(
    `/profile/facts/${encodeURIComponent(key)}/deny`, { baseRevision: current.planningRevision, version }
  )
  return resp.data
}

/** DELETE /api/v1/profile/facts/{key} — 删除事实（软删除）。 */
export async function deleteProfileFact(key: string, version?: number): Promise<void> {

  const current = await getScheduleSnapshot()
  await http.delete(`/profile/facts/${encodeURIComponent(key)}`, { params: { baseRevision: current.planningRevision, version } })
}

export async function setDailyLimit(minutes: number): Promise<ProfileFact> {
  const snapshot = await getScheduleSnapshot()
  const facts = await listProfileFacts()
  const version = facts.find(f => f.key === 'planning.dailyLimitMinutes')?.version ?? 0
  return (await http.put<ProfileFact>('/profile/facts/planning.dailyLimitMinutes', { value: minutes, version, baseRevision: snapshot.planningRevision })).data
}
