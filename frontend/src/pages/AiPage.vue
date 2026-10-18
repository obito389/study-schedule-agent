<script setup lang="ts">
import { onMounted, ref } from "vue";
import { api, errorMessage } from "../api/client";
import { snapshot, refresh } from "../composables/usePlanner";
import { formatTime } from "../lib/time";
import type { ActionSpec, Preview, Receipt } from "../types/contracts";
interface AiPreview {
  draftId: string;
  status: string;
  provider: string;
  model: string;
  baseRevision: number;
  elapsedMillis: number;
  explanations: string[];
  clarifications: string[];
  action: ActionSpec | null;
  candidate: Preview["candidate"] | null;
}
const instruction = ref(""),
  busy = ref(false),
  saving = ref(false),
  error = ref(""),
  result = ref<AiPreview | null>(null),
  receipt = ref<Receipt | null>(null);
const configuration = ref<{
  configured: boolean;
  provider: string;
  model: string;
  mode: string;
} | null>(null);
const examples = [
  "明天晚上20点到21点复习数据库，创建一个预计60分钟、优先级4的任务。",
  "把已有未完成任务安排在明天18点到22点之间。",
  "帮我看看接下来有哪些任务和安排。",
];
async function generate() {
  busy.value = true;
  error.value = "";
  result.value = null;
  receipt.value = null;
  try {
    result.value = (
      await api.post<AiPreview>(
        "/ai/previews",
        { instruction: instruction.value },
        { timeout: 65000 },
      )
    ).data;
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
  }
}
async function confirm() {
  if (!result.value) return;
  saving.value = true;
  error.value = "";
  try {
    receipt.value = (
      await api.post<Receipt>(`/ai/drafts/${result.value.draftId}/confirm`)
    ).data;
    await refresh();
  } catch (e) {
    error.value = errorMessage(e);
    await refresh().catch(() => {});
  } finally {
    saving.value = false;
  }
}
function taskTitle(id: string) {
  return (
    snapshot.value?.tasks.find((t) => t.id === id)?.title ??
    String(result.value?.action?.changes.title ?? "新任务")
  );
}
function displayDeadline(value: unknown) {
  return typeof value === "string" ? formatTime(value) : "—";
}
onMounted(async () => {
  try {
    configuration.value = (await api.get("/ai/status")).data;
  } catch (e) {
    error.value = errorMessage(e);
  }
});
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">YOUR WORDS, A CLEARER PLAN</div>
      <h1>用一句话，安排学习<span class="heading-dot">.</span></h1>
      <p>DeepSeek 理解你的要求，后端校验冲突，你决定是否保存。</p>
    </div>
    <span class="mode-tag">{{
      configuration?.configured
        ? "真实 AI · " + configuration.model
        : "等待配置 DeepSeek"
    }}</span>
  </div>
  <div class="ai-layout">
    <section class="panel padded">
      <div class="panel-heading">
        <h2>告诉 AI 你想做什么</h2>
        <span class="badge blue">单次理解 · 人工确认</span>
      </div>
      <form class="form-stack" @submit.prevent="generate">
        <label
          >学习要求<textarea
            v-model="instruction"
            rows="6"
            maxlength="1500"
            required
            placeholder="例如：明天晚上8点复习数据库，预计1小时，帮我创建并安排这个任务。"
            :disabled="busy"
            data-testid="ai-instruction"
          ></textarea>
        </label>
        <small class="muted"
          >{{ instruction.length }} / 1500 · 日期按北京时间理解</small
        >
        <div class="ai-examples">
          <small class="muted">试试这样说</small
          ><button
            v-for="example in examples"
            :key="example"
            type="button"
            :disabled="busy"
            @click="instruction = example"
          >
            {{ example }} ↗
          </button>
        </div>
        <button
          class="primary"
          :disabled="busy || !instruction.trim() || !configuration?.configured"
          data-testid="ai-generate"
        >
          {{ busy ? "AI 正在理解与生成建议…" : "✧ 让 AI 生成建议" }}
        </button>
        <p class="form-hint">
          当前支持创建单个任务、按一个时间窗口安排已有任务尚未排入的部分、查询日程。创建任务请说明耗时和截止时间，或明确开始时刻与时长。生成时会向
          DeepSeek 发送本次要求和当前任务、课程、时段。
        </p>
      </form>
      <div
        v-if="configuration && !configuration.configured"
        class="notice warning"
      >
        后端未检测到 DEEPSEEK_API_KEY。可继续使用任务页的规则排程；这里不会用
        Mock 冒充真实 AI。
      </div>
    </section>
    <section class="panel padded ai-result" aria-live="polite">
      <div class="panel-heading">
        <h2>AI 理解与执行预览</h2>
        <span v-if="result" class="badge green"
          >{{ result.provider }} ·
          {{ (result.elapsedMillis / 1000).toFixed(1) }} 秒</span
        >
      </div>
      <div
        v-if="error"
        class="notice warning"
        role="alert"
        data-testid="ai-error"
      >
        {{ error }}
      </div>
      <div v-if="busy" class="empty-state compact">
        <span class="empty-icon">✧</span>
        <h3>正在调用真实模型</h3>
        <p>AI 正在提取任务、日期和时间限制。<br />此时还没有写入任务或日历。</p>
      </div>
      <div v-else-if="!result && !error" class="empty-state">
        <span class="empty-icon">✧</span>
        <h3>把模糊想法，变成清晰安排</h3>
        <p>先生成、再检查、最后确认。<br />模型建议仍需通过后端硬约束校验。</p>
      </div>
      <template v-if="result">
        <div class="ai-reasons">
          <small class="eyebrow">AI 的理解与依据</small>
          <p v-for="(reason, i) in result.explanations" :key="i">
            {{ reason }}
          </p>
        </div>
        <div v-if="result.clarifications.length" class="notice warning">
          <strong>{{
            result.status === "NO_CAPACITY" ? "当前无法安排" : "需要补充信息"
          }}</strong>
          <div v-for="(q, i) in result.clarifications" :key="i">{{ q }}</div>
          <small>请在左侧补充完整要求后重新生成。</small>
        </div>
        <template
          v-if="
            result.action?.type === 'CREATE_TASK' &&
            result.status === 'NEEDS_CONFIRMATION'
          "
        >
          <div class="ai-task-card">
            <small class="muted">待创建任务</small>
            <h3>{{ result.action.changes.title }}</h3>
            <div>
              截止：{{ displayDeadline(result.action.changes.deadline) }}
            </div>
            <div>
              预计：{{ result.action.changes.durationMinutes }} 分钟 · 优先级
              {{ result.action.changes.priority ?? 3 }}
            </div>
          </div>
          <p v-if="!result.candidate?.blocks.length" class="form-hint">
            未指定具体开始时间，本次只创建任务。保存后可再让 AI 安排已有任务。
          </p>
        </template>
        <div v-if="result.candidate?.gaps.length" class="notice warning">
          {{ result.candidate.gaps.length }}
          个任务无法完整排入指定窗口，未截短安排。
        </div>
        <div v-if="result.candidate?.blocks.length" class="preview-list">
          <div
            v-for="(block, i) in result.candidate.blocks"
            :key="i"
            class="preview-row"
          >
            <span class="number-dot">{{ i + 1 }}</span
            ><strong>{{ taskTitle(block.taskId) }}</strong
            ><span
              >{{ formatTime(block.start) }} — {{ formatTime(block.end) }}</span
            >
          </div>
        </div>
        <div v-if="receipt" class="notice">
          <strong data-testid="ai-saved">已确认保存</strong>
          <div>
            执行版本 {{ receipt.planningRevision }} ·
            可到日历查看，或在执行记录撤销。
          </div>
          <router-link to="/" class="subtle-link">前往学习日历 ↗</router-link>
        </div>
        <template v-else-if="result.status === 'NEEDS_CONFIRMATION'"
          ><div class="notice">
            以上是 AI
            候选建议，尚未保存。已通过当前版本的硬约束预检，确认时还会重新校验。
          </div>
          <button
            class="primary"
            :disabled="saving"
            @click="confirm"
            data-testid="ai-confirm"
          >
            {{ saving ? "正在确认保存…" : "确认采用 AI 建议" }}
          </button></template
        >
        <div v-if="result.status === 'ANSWER'" class="form-hint">
          这是基于当前日程的 AI 解读，没有修改数据。
        </div>
      </template>
    </section>
  </div>
</template>
