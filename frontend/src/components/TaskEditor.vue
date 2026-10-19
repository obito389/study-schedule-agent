<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { snapshot, runCommand, safely } from "../composables/usePlanner";
import { shanghaiInput, tomorrowDeadline, toInstant } from "../lib/time";
import type { TaskSnapshot } from "../types/contracts";
const props = defineProps<{
  modelValue: boolean;
  task?: TaskSnapshot | null;
}>();
const emit = defineEmits<{ "update:modelValue": [boolean]; saved: [] }>();
const form = reactive({
    title: "",
    deadline: "",
    durationMinutes: 60,
    priority: 3,
    courseId: "",
  }),
  busy = ref(false);
watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      const t = props.task;
      Object.assign(form, {
        title: t?.title ?? "",
        deadline: t ? shanghaiInput(t.deadline) : tomorrowDeadline(),
        durationMinutes: t?.estimatedDurationMinutes ?? 60,
        priority: t?.priority ?? 3,
        courseId: t?.courseId ?? "",
      });
    }
  },
);
async function submit() {
  busy.value = true;
  await safely(async () => {
    await runCommand(props.task ? "UPDATE_TASK" : "CREATE_TASK", {
      ...form,
      deadline: toInstant(form.deadline),
      ...(props.task ? { taskId: props.task.id } : {}),
    });
    emit("update:modelValue", false);
    emit("saved");
  });
  busy.value = false;
}
</script>
<template>
  <el-dialog
    :model-value="modelValue"
    :title="task ? '修改任务 · 预览与确认' : '新建任务 · 预览与确认'"
    width="520px"
    :close-on-click-modal="!busy"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <form class="form-stack" @submit.prevent="submit">
      <label
        >任务名称<input
          v-model="form.title"
          required
          maxlength="100"
          placeholder="例如：完成数据库实验报告"
          data-testid="task-title"
      /></label>
      <label
        >截止时间 <small>北京时间</small
        ><input
          v-model="form.deadline"
          type="datetime-local"
          required
          data-testid="task-deadline"
      /></label>
      <div class="form-row">
        <label
          >预计耗时（分钟）<input
            v-model.number="form.durationMinutes"
            type="number"
            min="1"
            max="10080"
            required /></label
        ><label
          >优先级<select v-model.number="form.priority">
            <option :value="1">1 · 较低</option>
            <option :value="2">2 · 一般</option>
            <option :value="3">3 · 正常</option>
          </select></label
        >
      </div>
      <label
        >所属课程<select v-model="form.courseId">
          <option value="">不关联课程</option>
          <option v-for="c in snapshot?.courses" :key="c.id" :value="c.id">
            {{ c.name }}
          </option>
        </select></label
      >
      <p class="form-hint">
        确认后保存任务；时间块需在排程预览中单独确认。修改已排程任务仍须通过硬约束校验。
      </p>
      <div class="dialog-actions">
        <button
          type="button"
          class="secondary"
          :disabled="busy"
          @click="emit('update:modelValue', false)"
        >
          取消</button
        ><button class="primary" :disabled="busy" data-testid="task-submit">
          {{ busy ? "保存中…" : "确认保存任务" }}
        </button>
      </div>
    </form>
  </el-dialog>
</template>

