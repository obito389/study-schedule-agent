<script setup lang="ts">
import { computed, ref } from "vue";
import { ElMessageBox } from "element-plus";
import { snapshot, runCommand, safely } from "../composables/usePlanner";
import { formatTime } from "../lib/time";
import { visibleTasks } from "../lib/tasks";
import TaskEditor from "../components/TaskEditor.vue";
import PlanPreview from "../components/PlanPreview.vue";
import type { TaskSnapshot } from "../types/contracts";
const filter = ref("ACTIVE"),
  sort = ref("deadline"),
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
const tasks = computed(() =>
  visibleTasks(
    snapshot.value?.tasks ?? [],
    filter.value,
    query.value,
    sort.value,
  ),
);
const hasTasks = computed(() => Boolean(snapshot.value?.tasks.length));
function clearFilters() {
  query.value = "";
  filter.value = "ALL";
}
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
      <div class="eyebrow">一步一步，完成目标</div>
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
  <section class="panel tasks-panel">
    <div class="panel-toolbar">
      <div class="segmented">
        <button
          v-for="f in [
            { id: 'ACTIVE', label: '待完成' },
            { id: 'DONE', label: '已完成' },
            { id: 'ALL', label: '全部' },
          ]"
          :key="f.id"
          :class="{ selected: filter === f.id }"
          :aria-pressed="filter === f.id"
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
    <div class="task-results-bar">
      <span role="status"
        >{{ tasks.length }} 项任务<span v-if="query.trim()">
          · 搜索“{{ query.trim() }}”</span
        ></span
      >
      <select v-model="sort" aria-label="任务排序">
        <option value="deadline">截止时间优先</option>
        <option value="priority">优先级优先</option>
      </select>
    </div>
    <div v-if="!tasks.length" class="empty-state">
      <span class="empty-icon">☑</span>
      <template v-if="hasTasks || query.trim() || filter !== 'ACTIVE'">
        <h3>这里暂时没有匹配的任务</h3>
        <p>换个关键词，或查看全部任务。</p>
        <button class="secondary" @click="clearFilters">清除筛选</button>
      </template>
      <template v-else>
        <h3>从一个小目标开始</h3>
        <p>创建任务后，就可以为它安排专注的时间。</p>
        <button class="primary" @click="edit(null)">新建第一个任务</button>
      </template>
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
            <td data-label="截止时间">{{ formatTime(t.deadline) }}</td>
            <td data-label="预计耗时">{{ t.estimatedDurationMinutes }} 分钟</td>
            <td data-label="状态">
              <span
                :class="[
                  'badge',
                  t.status === 'DONE'
                    ? 'green'
                    : t.status === 'CANCELLED'
                      ? 'neutral'
                      : 'blue',
                ]"
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
