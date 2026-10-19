import axios, { AxiosError } from 'axios'
import type { ContractError } from '@/types/contract'

/**
 * 统一 axios 实例。所有业务接口走 /api/v1。
 * 后端未就绪时，各 api 模块内启用 mock 回退（不发起真实请求），
 * 保证断网可演示；后端上线后把 USE_MOCK 置为 false 即可。
 */
export const USE_MOCK = true

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
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
  (resp) => resp,
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
