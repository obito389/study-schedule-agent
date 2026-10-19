<script setup lang="ts">
import { ref, watch } from "vue";
import { ElMessageBox } from "element-plus";
import { api } from "../api/client";
import { snapshot, runCommand, safely } from "../composables/usePlanner";
import type { Receipt } from "../types/contracts";
const props = defineProps<{ modelValue: boolean }>(),
  emit = defineEmits<{ "update:modelValue": [boolean] }>();
const receipts = ref<Receipt[]>([]),
  busy = ref(false);
const names: Record<string, string> = {
  CREATE_TASK: "创建任务",
  UPDATE_TASK: "修改任务",
  CANCEL_TASK: "取消任务",
  MOVE_TASK: "移动时间块",
  APPLY_PLAN: "保存排程",
  COMPLETE_TASK: "完成任务与耗时反馈",
  UNDO: "撤销操作",
};
async function load() {
  receipts.value = (await api.get<Receipt[]>("/actions")).data;
}
watch(
  () => props.modelValue,
  (open) => {
    if (open) safely(load);
  },
);
async function undo(r: Receipt) {
  try {
    await ElMessageBox.confirm(
      "将恢复该次执行前的任务状态。后续已有修改时会拒绝撤销。",
      "确认撤销",
      { confirmButtonText: "确认撤销", cancelButtonText: "返回" },
    );
  } catch {
    return;
  }
  busy.value = true;
  await safely(async () => {
    await runCommand("UNDO", { executionId: r.executionId });
    await load();
  });
  busy.value = false;
}
</script>
<template>
  <el-drawer
    :model-value="modelValue"
    title="执行记录与撤销"
    size="480px"
    @update:model-value="emit('update:modelValue', $event)"
    ><p class="muted">
      当前版本 {{ snapshot?.planningRevision }} ·
      已完成任务的耗时反馈不可在此撤销
    </p>
    <div v-if="!receipts.length" class="empty-state">还没有执行记录</div>
    <div
      v-for="r in receipts"
      :key="r.executionId ?? r.operationKey"
      class="history-item"
    >
      <div>
        <strong>{{ names[r.type] ?? r.type }}</strong
        ><span :class="['badge', r.status === 'APPLIED' ? 'green' : 'red']">{{
          r.status === "APPLIED" ? "已执行" : "失败"
        }}</span>
      </div>
      <p>{{ r.message }}</p>
      <small
        >版本 {{ r.planningRevision }} ·
        {{ r.operationKey.slice(0, 12) }}</small
      ><button
        v-if="r.undoable"
        class="text-button"
        :disabled="busy"
        @click="undo(r)"
      >
        撤销这次操作
      </button>
    </div></el-drawer
  >
</template>
