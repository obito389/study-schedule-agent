import axios, { AxiosError } from 'axios'
import type { ContractError } from '@/types/contract'

/**
 * 统一 axios 实例。所有业务接口走 /api/v1。
 * 所有页面请求组员后端；连接失败直接显示错误。
 */

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json', 'X-User-Id': '1' }
})

/** 统一错误结构：后端返回 { code, message, objectIds } 或 409 冲突体。 */
export class ApiError extends Error {
  code: string
  objectIds: string[]
  httpStatus: number

  constructor(code: string, message: string, objectIds: string[], httpStatus: number) {
    super(message)
    this.code = code
    this.objectIds = objectIds
    this.httpStatus = httpStatus
  }
}

http.interceptors.response.use(
  (resp) => {
    if (resp.data && typeof resp.data.code === 'number' && 'data' in resp.data) {
      if (resp.data.code !== 0) throw new ApiError(String(resp.data.code), resp.data.message, [], resp.status)
      resp.data = resp.data.data
    }
    return resp
  },
  (error: AxiosError<any>) => {
    const status = error.response?.status ?? 0
    const body = error.response?.data
    const contractError: ContractError | undefined =
      body?.error ?? (body?.code ? (body as ContractError) : undefined)
    throw new ApiError(
      contractError?.code ?? `HTTP_${status}`,
      contractError?.message ?? error.message ?? '请求失败',
      contractError?.objectIds ?? [],
      status
    )
  }
)
