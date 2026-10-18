<script setup lang="ts">
import { computed, ref } from "vue";
import { ElMessageBox } from "element-plus";
import { snapshot, runCommand, safely } from "../composables/usePlanner";
import { formatTime } from "../lib/time";
import TaskEditor from "../components/TaskEditor.vue";
import PlanPreview from "../components/PlanPreview.vue";
import type { TaskSnapshot } from "../types/contracts";
const filter = ref("ACTIVE"),
  query = ref(""),
  editor = ref(false),
  selected = ref<TaskSnapshot | null>(null),
  busy = ref(false);
const statuses: Record<string, string> = {
  TODO: "待安排",
  IN_PROGRESS: "进行中",
  DONE: "已完成",
  CANCELLED: "已取消",
};
const tasks = computed(
  () =>
    snapshot.value?.tasks.filter(
      (t) =>
        (filter.value === "ALL" ||
          (filter.value === "ACTIVE"
            ? !["DONE", "CANCELLED"].includes(t.status)
            : t.status === filter.value)) &&
        t.title.includes(query.value),
    ) ?? [],
);
function edit(task: TaskSnapshot | null) {
  selected.value = task;
  editor.value = true;
}
async function cancel(t: TaskSnapshot) {
  try {
    await ElMessageBox.confirm(`取消“${t.title}”并移除其时间块？`, "确认取消", {
      confirmButtonText: "确认取消任务",
      cancelButtonText: "返回",
    });
  } catch {
    return;
  }
  busy.value = true;
  await safely(() => runCommand("CANCEL_TASK", { taskId: t.id }));
  busy.value = false;
}
async function complete(t: TaskSnapshot) {
  let minutes: number;
  try {
    const result = await ElMessageBox.prompt(
      "请输入实际耗时（分钟），用于更新耗时估计。",
      "完成任务与反馈",
      {
        inputValue: String(t.estimatedDurationMinutes),
        inputPattern: /^[1-9]\d{0,4}$/,
        inputErrorMessage: "请输入正整数",
        confirmButtonText: "确认完成",
        cancelButtonText: "返回",
      },
    );
    minutes = Number(result.value);
  } catch {
    return;
  }
  busy.value = true;
  await safely(() =>
    runCommand("COMPLETE_TASK", { taskId: t.id, actualMinutes: minutes }),
  );
  busy.value = false;
}
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">MAKE ROOM FOR WHAT MATTERS</div>
      <h1>任务清单<span class="heading-dot">.</span></h1>
      <p>把学习目标变成清晰、可安排的一小步。</p>
    </div>
    <div class="button-group">
      <PlanPreview /><button
        class="secondary"
        @click="edit(null)"
        data-testid="new-task"
      >
        ＋ 新建任务
      </button>
    </div>
  </div>
  <section class="panel">
    <div class="panel-toolbar">
      <div class="segmented">
        <button
          v-for="f in [
            { id: 'ACTIVE', label: '进行中的任务' },
            { id: 'DONE', label: '已完成' },
            { id: 'ALL', label: '全部' },
          ]"
          :key="f.id"
          :class="{ selected: filter === f.id }"
          @click="filter = f.id"
        >
          {{ f.label }}
        </button>
      </div>
      <input
        v-model="query"
        class="search-input"
        placeholder="搜索任务名称"
        aria-label="搜索任务"
      />
    </div>
    <div v-if="!tasks.length" class="empty-state">
      <span class="empty-icon">☑</span>
      <h3>给今天一个小目标</h3>
      <p>新建一个任务，再生成排程预览。</p>
      <button class="primary" @click="edit(null)">新建第一个任务</button>
    </div>
    <div v-else class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>任务 / 课程</th>
            <th>截止时间</th>
            <th>预计耗时</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in tasks" :key="t.id">
            <td>
              <strong>{{ t.title }}</strong
              ><small class="table-sub"
                >{{
                  snapshot?.courses.find((c) => c.id === t.courseId)?.name ??
                  "独立任务"
                }}
                · 优先级 {{ t.priority }}</small
              >
            </td>
            <td>{{ formatTime(t.deadline) }}</td>
            <td>{{ t.estimatedDurationMinutes }} 分钟</td>
            <td>
              <span
                :class="['badge', t.status === 'DONE' ? 'green' : 'blue']"
                >{{
                  t.status === "TODO" && t.scheduledBlocks.length
                    ? "已安排"
                    : statuses[t.status]
                }}</span
              >
            </td>
            <td>
              <div
                v-if="!['DONE', 'CANCELLED'].includes(t.status)"
                class="row-actions"
              >
                <button @click="edit(t)" :disabled="busy">编辑</button
                ><button @click="complete(t)" :disabled="busy">完成</button
                ><button @click="cancel(t)" :disabled="busy">取消</button>
              </div>
              <span v-else class="muted">—</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
  <TaskEditor v-model="editor" :task="selected" />
</template>
