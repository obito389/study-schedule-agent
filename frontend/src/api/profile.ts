import { http, USE_MOCK } from './client'
import type { ProfileFact } from '@/types/contract'
import { mockProfileFacts } from './mockData'

let mockFacts = [...mockProfileFacts]

/** GET /api/v1/profile/facts — 列出用户事实（含各状态）。 */
export async function listProfileFacts(): Promise<ProfileFact[]> {
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 200))
    return mockFacts
  }
  const resp = await http.get<ProfileFact[]>('/profile/facts')
  return resp.data
}

/** PUT /api/v1/profile/facts/{key} — 修改事实值。 */
export async function updateProfileFact(
  fact: ProfileFact
): Promise<ProfileFact> {
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 200))
    const updated: ProfileFact = { ...fact, version: fact.version + 1 }
    mockFacts = mockFacts.map((f) => (f.key === fact.key ? updated : f))
    return updated
  }
  const resp = await http.put<ProfileFact>(
    `/profile/facts/${encodeURIComponent(fact.key)}`,
    fact
  )
  return resp.data
}

/** POST /api/v1/profile/facts/{key}/deny — 用户否认该事实。 */
export async function denyProfileFact(key: string): Promise<ProfileFact> {
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 200))
    const target = mockFacts.find((f) => f.key === key)
    if (!target) throw new Error(`fact not found: ${key}`)
    const updated: ProfileFact = {
      ...target,
      state: 'DENIED' as const,
      version: target.version + 1
    }
    mockFacts = mockFacts.map((f) => (f.key === key ? updated : f))
    return updated
  }
  const resp = await http.post<ProfileFact>(
    `/profile/facts/${encodeURIComponent(key)}/deny`
  )
  return resp.data
}

/** DELETE /api/v1/profile/facts/{key} — 删除事实（软删除）。 */
export async function deleteProfileFact(key: string): Promise<void> {
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 200))
    mockFacts = mockFacts.map((f) =>
      f.key === key ? { ...f, state: 'DELETED' as const, version: f.version + 1 } : f
    )
    return
  }
  await http.delete(`/profile/facts/${encodeURIComponent(key)}`)
}
