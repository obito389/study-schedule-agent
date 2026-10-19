<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { useVoiceInput } from "../composables/useVoiceInput";
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
const submittedInstruction = ref("");
const resultPanel = ref<HTMLElement | null>(null);
const inputLocked = computed(() => busy.value || saving.value);
const stale = computed(() =>
  Boolean(
    result.value &&
    !receipt.value &&
    (instruction.value.trim() !== submittedInstruction.value ||
      result.value.baseRevision !== snapshot.value?.planningRevision),
  ),
);
const voice = useVoiceInput(instruction, inputLocked);
const { listening, processing, message: voiceMessage } = voice;
const summary = computed(() => {
  const preview = result.value;
  if (!preview) return [];
  if (preview.status === "NEEDS_CLARIFICATION") {
    return ["还需补充或确认关键信息，完善要求后即可重新生成。"];
  }
  if (preview.status === "NO_CAPACITY") {
    return ["当前窗口无法容纳完整安排，请调整时段或任务后重试。"];
  }
  if (
    preview.action?.type === "CREATE_TASK" &&
    preview.status === "NEEDS_CONFIRMATION"
  ) {
    const changes = preview.action.changes;
    return [
      `创建「${changes.title}」，预计 ${changes.durationMinutes} 分钟。${preview.candidate?.blocks.length ? "已安排具体时段，请核对后确认。" : "本次仅创建任务，暂不安排时段。"}`,
    ];
  }
  if (preview.candidate && preview.status === "NEEDS_CONFIRMATION") {
    return [
      `已生成 ${preview.candidate.blocks.length} 个安排时段${preview.candidate.gaps.length ? `，${preview.candidate.gaps.length} 个任务未能完整排入` : ""}，请核对后确认。`,
    ];
  }
  // Query answers and clarification context retain their meaning; never truncate dates or conditions.
  return preview.explanations;
});
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
  if (
    inputLocked.value ||
    listening.value ||
    processing.value ||
    !instruction.value.trim() ||
    !configuration.value?.configured
  )
    return;
  submittedInstruction.value = instruction.value.trim();
  busy.value = true;
  error.value = "";
  result.value = null;
  receipt.value = null;
  try {
    result.value = (
      await api.post<AiPreview>(
        "/ai/previews",
        { instruction: submittedInstruction.value },
        { timeout: 65000 },
      )
    ).data;
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    busy.value = false;
    await nextTick();
    if (window.matchMedia("(max-width: 1000px)").matches) {
      resultPanel.value?.scrollIntoView({
        behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches
          ? "instant"
          : "smooth",
        block: "start",
      });
    }
  }
}
async function confirm() {
  if (!result.value || stale.value || inputLocked.value || receipt.value)
    return;
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
      <div class="eyebrow">你的学习，开始有序</div>
      <h1>想学什么，一起安排。</h1>
    </div>
  </div>
  <ol class="workflow-steps" aria-label="安排步骤">
    <li :class="{ current: !result && !busy }"><span>01</span> 描述想法</li>
    <li :class="{ current: busy || (result && !receipt) }">
      <span>02</span> 核对建议
    </li>
    <li :class="{ current: receipt }"><span>03</span> 确认保存</li>
  </ol>
  <div class="ai-layout">
    <section class="panel padded ai-composer">
      <div class="panel-heading">
        <h2>这次想完成什么？</h2>
      </div>
      <form class="form-stack" @submit.prevent="generate">
        <textarea
          aria-label="学习要求"
          v-model="instruction"
          rows="5"
          maxlength="1500"
          required
          placeholder="例如：明天晚上8点复习数据库，预计1小时，帮我创建并安排这个任务。"
          :disabled="inputLocked"
          :class="{ 'voice-listening': listening }"
          aria-describedby="voice-help"
          data-testid="ai-instruction"
        ></textarea>
        <div class="voice-controls">
          <button
            type="button"
            class="voice-button"
            :class="{ recording: listening }"
            :disabled="inputLocked"
            @pointerdown="voice.press"
            @pointermove="voice.move"
            @keydown="voice.keydown"
            @keyup="voice.keyup"
            @blur="voice.release"
            :aria-pressed="listening"
          >
            {{ listening ? "● 正在听…" : "🎙 按住说话" }}
          </button>
          <small id="voice-help" role="status">{{ voiceMessage }}</small>
        </div>
        <details class="example-disclosure">
          <summary>
            还没想好？试试这些例子 <span aria-hidden="true">＋</span>
          </summary>
          <div class="ai-examples">
            <button
              v-for="example in examples"
              :key="example"
              type="button"
              :disabled="inputLocked || listening || processing"
              @click="instruction = example"
            >
              {{ example }} ↗
            </button>
          </div>
        </details>
        <button
          class="primary"
          :disabled="
            inputLocked ||
            listening ||
            processing ||
            !instruction.trim() ||
            !configuration?.configured
          "
          data-testid="ai-generate"
        >
          {{
            busy
              ? "正在梳理你的安排…"
              : result
                ? "重新生成建议 →"
                : "生成学习建议 →"
          }}
        </button>
        <p class="form-hint">
          可创建单个任务、安排已有任务或查询日程。请说清时间和耗时；生成时会将本次要求及当前日程发送给
          DeepSeek。
        </p>
      </form>
      <div
        v-if="configuration && !configuration.configured"
        class="notice warning"
      >
        AI 暂未配置，你仍可在任务清单中手动创建任务或使用规则排程。
      </div>
    </section>
    <section
      ref="resultPanel"
      class="panel padded ai-result"
      aria-live="polite"
      :aria-busy="busy"
    >
      <div class="panel-heading">
        <h2>{{ receipt ? "安排已保存" : "建议预览" }}</h2>
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
        <h3>正在为你梳理安排</h3>
        <p>理解任务、检查时间，再给你一份建议。<br />完成后由你核对和确认。</p>
      </div>
      <div v-else-if="!result && !error" class="empty-state">
        <span class="empty-icon">✧</span>
        <h3>下一步，会在这里变清晰</h3>
        <p>
          写下学习想法，生成一份可核对的建议。<br />只有你确认后，才会保存到日程。
        </p>
      </div>
      <template v-if="result">
        <div v-if="stale" class="notice warning" data-testid="ai-stale">
          要求或日程已有变化，这份建议已过期。请重新生成后再确认。
        </div>
        <div class="ai-reasons">
          <small class="eyebrow">AI 理解摘要</small>
          <p v-for="(reason, i) in summary" :key="i">
            {{ reason }}
          </p>
        </div>
        <div v-if="result.clarifications.length" class="notice warning">
          <strong>{{
            result.status === "NO_CAPACITY" ? "当前无法安排" : "需要补充信息"
          }}</strong>
          <div v-for="(q, i) in result.clarifications" :key="i">{{ q }}</div>
          <small>请补充完整要求后重新生成。</small>
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
              <span>截止：</span
              ><time class="ai-deadline">{{
                displayDeadline(result.action.changes.deadline)
              }}</time>
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
            ><span class="ai-block-time"
              >{{ formatTime(block.start) }} — {{ formatTime(block.end) }}</span
            >
          </div>
        </div>
        <div v-if="receipt" class="notice">
          <strong data-testid="ai-saved">已确认保存</strong>
          <div>可到日历查看，或在右上角执行记录中撤销。</div>
          <router-link to="/" class="subtle-link">前往学习日历 ↗</router-link>
        </div>
        <template v-else-if="result.status === 'NEEDS_CONFIRMATION'"
          ><div class="notice">
            建议尚未保存。请核对任务与时间，确认后才会写入日程。
          </div>
          <button
            class="primary"
            :disabled="inputLocked || stale"
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
