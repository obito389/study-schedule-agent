<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import FullCalendar from "@fullcalendar/vue3";
import dayGridPlugin from "@fullcalendar/daygrid";
import timeGridPlugin from "@fullcalendar/timegrid";
import interactionPlugin from "@fullcalendar/interaction";
import zhCnLocale from "@fullcalendar/core/locales/zh-cn";
import type { CalendarOptions, EventClickArg } from "@fullcalendar/core";
import { ElMessageBox } from "element-plus";
import { api } from "../api/client";
import {
  snapshot,
  refresh,
  runCommand,
  safely,
} from "../composables/usePlanner";
import {
  calendarEvents,
  formatTime,
  nextSlot,
  shanghaiInput,
  toInstant,
} from "../lib/time";
import type { TaskSnapshot, TimeBlock } from "../types/contracts";
import PlanPreview from "../components/PlanPreview.vue";
import TaskEditor from "../components/TaskEditor.vue";
const editor = ref(false),
  addOpen = ref(false),
  details = ref(false),
  busy = ref(false);
const selected = ref<{
  title: string;
  kind: string;
  block: TimeBlock;
  task?: TaskSnapshot;
} | null>(null);
const move = reactive({ start: "", end: "" });
const form = reactive({
  type: "COURSE",
  name: "",
  start: nextSlot(),
  end: nextSlot(),
});
const active = computed(() =>
  snapshot.value!.tasks.filter(
    (t) => !["DONE", "CANCELLED"].includes(t.status),
  ),
);
const arranged = computed(
  () => active.value.filter((t) => t.scheduledBlocks.length).length,
);
const minutes = computed(() =>
  snapshot
    .value!.tasks.flatMap((t) => t.scheduledBlocks)
    .reduce(
      (sum, b) => sum + (Date.parse(b.end) - Date.parse(b.start)) / 60000,
      0,
    ),
);
const upcoming = computed(() =>
  active.value
    .slice()
    .sort((a, b) => a.deadline.localeCompare(b.deadline))
    .slice(0, 4),
);
const options = computed<CalendarOptions>(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  locale: zhCnLocale,
  initialView: window.innerWidth < 760 ? "timeGridDay" : "timeGridWeek",
  initialDate: shanghaiInput(snapshot.value!.now),
  timeZone: "local",
  headerToolbar: {
    left: "prev,next today",
    center: "title",
    right:
      window.innerWidth < 760
        ? "timeGridDay,dayGridMonth"
        : "timeGridWeek,dayGridMonth",
  },
  slotDuration: "00:30:00",
  allDaySlot: false,
  height: 660,
  scrollTime:
    String(
      Math.max(0, Number(shanghaiInput(snapshot.value!.now).slice(11, 13)) - 1),
    ).padStart(2, "0") + ":00:00",
  nowIndicator: true,
  events: calendarEvents(snapshot.value!),
  eventClick: clickEvent,
  editable: false,
  displayEventTime: true,
  dayMaxEvents: true,
}));
function clickEvent(arg: EventClickArg) {
  const p = arg.event.extendedProps;
  selected.value = {
    title: arg.event.title,
    kind: p.kind,
    block: p.block,
    task: p.task,
  };
  move.start = shanghaiInput(p.block.start);
  move.end = shanghaiInput(p.block.end);
  details.value = true;
}
function openAdd() {
  form.name = "";
  form.start = nextSlot();
  form.end = shanghaiInput(
    new Date(Date.parse(toInstant(form.start)) + 3600000).toISOString(),
  );
  addOpen.value = true;
}
async function saveBlock() {
  busy.value = true;
  await safely(async () => {
    const payload = {
      start: toInstant(form.start),
      end: toInstant(form.end),
      baseRevision: snapshot.value!.planningRevision,
    };
    if (form.type === "COURSE")
      await api.post("/courses", { ...payload, name: form.name });
    else
      await api.post("/blocks", {
        ...payload,
        title: form.name,
        unavailable: form.type === "UNAVAILABLE",
      });
    await refresh();
    addOpen.value = false;
  });
  busy.value = false;
}
async function saveMove() {
  if (!selected.value?.task) return;
  busy.value = true;
  await safely(async () => {
    await runCommand("MOVE_TASK", {
      taskId: selected.value!.task!.id,
      blockId: selected.value!.block.id,
      start: toInstant(move.start),
      end: toInstant(move.end),
    });
    details.value = false;
  });
  busy.value = false;
}
async function remove() {
  if (!selected.value) return;
  try {
    await ElMessageBox.confirm(
      `确认删除“${selected.value.title}”的这项安排？`,
      "确认删除",
      { confirmButtonText: "删除", cancelButtonText: "返回" },
    );
  } catch {
    return;
  }
  busy.value = true;
  await safely(async () => {
    const s = selected.value!;
    await api.delete(
      s.block.kind === "COURSE"
        ? `/courses/${s.block.ownerId}`
        : `/blocks/${s.block.id}`,
      { params: { baseRevision: snapshot.value!.planningRevision } },
    );
    await refresh();
    details.value = false;
  });
  busy.value = false;
}
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">A LITTLE STRUCTURE, A LOT MORE FOCUS</div>
      <h1>让学习，有条不紊<span class="heading-dot">.</span></h1>
      <p>留出专注的时间，也给生活保留空白。</p>
    </div>
    <div class="button-group">
      <button class="secondary" @click="editor = true">＋ 新建任务</button
      ><PlanPreview />
    </div>
  </div>
  <div class="stats-grid">
    <div class="stat-card">
      <span>待完成任务</span>
      <div>{{ active.length }}<small>项</small></div>
      <i>☑</i>
    </div>
    <div class="stat-card">
      <span>已安排任务</span>
      <div>{{ arranged }}<small>项</small></div>
      <i>▦</i>
    </div>
    <div class="stat-card">
      <span>已规划学习</span>
      <div>{{ Number((minutes / 60).toFixed(1)) }}<small>小时</small></div>
      <i>◷</i>
    </div>
    <div class="stat-card">
      <span>累计完成</span>
      <div>
        {{ snapshot?.tasks.filter((t) => t.status === "DONE").length
        }}<small>项</small>
      </div>
      <i>✓</i>
    </div>
  </div>
  <div class="calendar-layout">
    <section class="panel calendar-panel">
      <div class="panel-heading">
        <div>
          <h2>我的日程</h2>
          <span class="muted">北京时间 · 30 分钟时间槽</span>
        </div>
        <button class="text-button" @click="openAdd">＋ 课程 / 固定时段</button>
      </div>
      <div class="calendar-legend">
        <span><i class="legend-dot blue-dot"></i>学习任务</span
        ><span><i class="legend-dot gold-dot"></i>课程</span
        ><span><i class="legend-dot grey-dot"></i>固定 / 禁止时段</span>
      </div>
      <FullCalendar :options="options" />
    </section>
    <aside class="calendar-aside">
      <section class="panel upcoming-panel">
        <div class="panel-heading">
          <h2>接下来要做</h2>
          <router-link to="/tasks" class="subtle-link">全部 ↗</router-link>
        </div>
        <div v-if="!upcoming.length" class="empty-state compact">
          还没有待办任务<br />从一个小目标开始吧。
        </div>
        <div v-for="t in upcoming" :key="t.id" class="upcoming-item">
          <span class="task-square"></span>
          <div>
            <strong>{{ t.title }}</strong
            ><small>{{ formatTime(t.deadline) }} 截止</small
            ><span class="mini-tag"
              >{{ t.estimatedDurationMinutes }} 分钟 ·
              {{ t.scheduledBlocks.length ? "已安排" : "待排程" }}</span
            >
          </div>
        </div>
        <button class="wide-button" @click="editor = true">
          ＋ 添加学习任务
        </button>
      </section>
      <section class="focus-note">
        <div class="note-icon">✧</div>
        <h3>先预览，再确认</h3>
        <p>
          排程会避开课程和固定时段。确认前可以查看每个候选时间块，保存后也可在执行记录中撤销。
        </p>
        <div class="note-line"></div>
        <small>一步一步，靠近目标。</small>
      </section>
    </aside>
  </div>
  <TaskEditor v-model="editor" />
  <el-dialog v-model="addOpen" title="添加课程或固定时段" width="500px"
    ><form class="form-stack" @submit.prevent="saveBlock">
      <label
        >类型<select v-model="form.type">
          <option value="COURSE">课程</option>
          <option value="UNAVAILABLE">禁止排程时段</option>
          <option value="PERSONAL">个人固定安排</option>
        </select></label
      ><label
        >名称<input
          v-model="form.name"
          required
          :maxlength="form.type === 'COURSE' ? 200 : 80"
          placeholder="例如：软件工程 / 晚间休息" /></label
      ><label
        >开始时间<input
          v-model="form.start"
          type="datetime-local"
          step="1800"
          required /></label
      ><label
        >结束时间<input
          v-model="form.end"
          type="datetime-local"
          step="1800"
          required
      /></label>
      <p class="form-hint">
        本次添加单个日期的时段。必须对齐半小时，且与已有安排不冲突。
      </p>
      <div class="dialog-actions">
        <button class="primary" :disabled="busy">确认保存时段</button>
      </div>
    </form></el-dialog
  >
  <el-dialog v-model="details" :title="selected?.title" width="480px"
    ><template v-if="selected"
      ><span class="badge blue">{{ selected.kind }}</span>
      <p>
        {{ formatTime(selected.block.start) }} —
        {{ formatTime(selected.block.end) }}
      </p>
      <form v-if="selected.task" class="form-stack" @submit.prevent="saveMove">
        <div class="notice">
          移动前会重新校验冲突、截止时间及每日上限，时长保持不变。
        </div>
        <label
          >新的开始时间<input
            v-model="move.start"
            type="datetime-local"
            step="1800"
            required /></label
        ><label
          >新的结束时间<input
            v-model="move.end"
            type="datetime-local"
            step="1800"
            required
        /></label>
        <div class="dialog-actions">
          <button class="primary" :disabled="busy">确认移动时间块</button>
        </div>
      </form>
      <button v-else class="danger-button" :disabled="busy" @click="remove">
        删除这项安排
      </button></template
    ></el-dialog
  >
</template>
