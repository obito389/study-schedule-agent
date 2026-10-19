import axios from "axios";
export const api = axios.create({ baseURL: "/api/v1", timeout: 30000 });
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (!error.response)
      return "暂时无法连接服务。操作结果可能尚未返回，请保留当前页面后重试或刷新确认。";
    return error.response.data?.message ?? "请求失败，请重试";
  }
  return error instanceof Error ? error.message : "操作失败";
}
