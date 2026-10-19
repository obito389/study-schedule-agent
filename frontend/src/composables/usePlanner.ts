import { ref } from "vue";
import { ElMessage } from "element-plus";
import axios from "axios";
import { api, errorMessage } from "../api/client";
import type { Command, Receipt, ScheduleSnapshot } from "../types/contracts";
export const snapshot = ref<ScheduleSnapshot | null>(null);
export const loading = ref(false);
export const connectionError = ref("");
const pending = new Map<string, Command>();
export async function refresh() {
  loading.value = true;
  try {
    snapshot.value = (await api.get<ScheduleSnapshot>("/schedule")).data;
    connectionError.value = "";
  } catch (e) {
    connectionError.value = errorMessage(e);
    throw e;
  } finally {
    loading.value = false;
  }
}
export async function runCommand(
  type: string,
  payload: Record<string, unknown>,
  baseRevision?: number,
) {
  const identity = JSON.stringify({ type, payload });
  let command = pending.get(identity);
  if (!command) {
    command = {
      operationKey: crypto.randomUUID(),
      baseRevision: baseRevision ?? snapshot.value!.planningRevision,
      type,
      authorizationScope: "schedule:write:self",
      payload,
    };
    pending.set(identity, command);
  }
  try {
    const receipt = (await api.post<Receipt>("/actions", command)).data;
    pending.delete(identity);
    await refresh();
    ElMessage.success(receipt.message);
    return receipt;
  } catch (e) {
    if (axios.isAxiosError(e) && e.response) {
      pending.delete(identity);
      await refresh().catch(() => {});
    }
    throw e;
  }
}
export async function safely(work: () => Promise<unknown>) {
  try {
    await work();
  } catch (e) {
    ElMessage.error(errorMessage(e));
  }
}
