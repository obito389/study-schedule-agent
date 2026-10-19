<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { api } from "../api/client";
import { refresh, safely } from "../composables/usePlanner";
import { tomorrowDeadline, toInstant, shanghaiInput } from "../lib/time";
import type { ImportJob, ImportProposal } from "../types/contracts";
const statusLabels: Record<string, string> = {
  REVIEW: "待校对",
  FAILED: "失败",
  COMMITTED: "已保存",
  PROCESSING: "处理中",
  UPLOADED: "已上传",
  EXPIRED: "已过期",
};
const job = ref<ImportJob | null>(null),
  file = ref<File | null>(null),
  sourceType = ref("SCREENSHOT"),
  busy = ref(false);
const requestKey = ref(sessionStorage.getItem("lastImportKey") ?? "");
const form = reactive({
  title: "",
  deadline: tomorrowDeadline(),
  durationMinutes: 60,
  courseCandidate: "",
});
function accept(value: ImportJob) {
  job.value = value;
  const p = value.proposal;
  if (p) {
    form.title = p.title;
    form.deadline = p.deadline ? shanghaiInput(p.deadline) : tomorrowDeadline();
    form.durationMinutes = p.durationMinutes ?? 60;
    form.courseCandidate = p.courseCandidate ?? "";
  }
}
function selectFile(event: Event) {
  file.value = (event.target as HTMLInputElement).files?.[0] ?? null;
  requestKey.value = "";
  job.value = null;
}
async function upload() {
  if (!file.value) return;
  busy.value = true;
  await safely(async () => {
    if (file.value!.size > 10 * 1024 * 1024)
      throw new Error("文件不能超过 10 MB");
    if (!requestKey.value) requestKey.value = crypto.randomUUID();
    sessionStorage.setItem("lastImportKey", requestKey.value);
    const data = new FormData();
    data.append("file", file.value!);
    data.append("sourceType", sourceType.value);
    accept(
      (
        await api.post<ImportJob>("/imports", data, {
          headers: { "X-Request-Key": requestKey.value },
        })
      ).data,
    );
  });
  busy.value = false;
}
async function recover() {
  if (requestKey.value)
    await safely(async () =>
      accept(
        (
          await api.get<ImportJob>(
            `/imports/${encodeURIComponent(requestKey.value)}`,
          )
        ).data,
      ),
    );
}
async function confirm() {
  if (!job.value?.proposal) return;
  busy.value = true;
  await safely(async () => {
    const p: ImportProposal =
      job.value!.status === "FAILED"
        ? job.value!.proposal!
        : {
            ...job.value!.proposal!,
            kind: "ASSIGNMENT",
            title: form.title,
            courseCandidate: form.courseCandidate || null,
            deadline: toInstant(form.deadline),
            durationMinutes: form.durationMinutes,
            ambiguities: [],
            errors: [],
            status: "REVIEW",
          };
    accept(
      (
        await api.post<ImportJob>(
          `/imports/${encodeURIComponent(requestKey.value)}/confirm`,
          p,
        )
      ).data,
    );
    await refresh();
  });
  busy.value = false;
}
onMounted(() => {
  if (requestKey.value) recover();
});
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">收集信息，认真核对</div>
      <h1>导入与校对</h1>
      <p>上传截图或录音，核对后再创建正式任务。</p>
    </div>
    <span class="mode-tag">文本 AI 提取 · 图片与录音人工校对</span>
  </div>
  <div class="notice warning">
    当前图片和录音采用人工校对
    流程：不会自动读取图片文字或转写录音；文本文件可由 DeepSeek 提取建议。所有提案须校对确认后才会保存到 MySQL。
  </div>
  <div class="import-layout">
    <section class="panel padded">
      <h2>01 / 选择来源</h2>
      <div class="form-stack">
        <label
          >来源类型<select v-model="sourceType" :disabled="!!job">
            <option value="SCREENSHOT">截图 / 图片</option>
            <option value="AUDIO">录音 / 音频</option>
            <option value="TEXT">文本文件</option>
          </select></label
        ><label class="upload-area"
          ><span class="upload-icon">↥</span
          ><strong>{{ file?.name ?? "点击选择要导入的文件" }}</strong
          ><small>单个文件不超过 10 MB</small
          ><input
            type="file"
            @change="selectFile"
            :accept="
              sourceType === 'SCREENSHOT'
                ? 'image/*'
                : sourceType === 'AUDIO'
                  ? 'audio/*'
                  : '.txt'
            "
            aria-label="上传文件" /></label
        ><button
          class="primary"
          :disabled="busy || !file || job?.status === 'COMMITTED'"
          @click="upload"
        >
          {{
            busy
              ? "处理中…"
              : job?.status === "FAILED" && !job?.proposal
                ? "重试识别"
                : "上传并生成提案"
          }}</button
        ><small v-if="requestKey" class="muted break-word"
          >请求键 {{ requestKey }}</small
        >
      </div>
    </section>
    <section class="panel padded">
      <div class="panel-heading">
        <h2>02 / 校对与确认</h2>
        <span
          v-if="job"
          :class="['badge', job.status === 'COMMITTED' ? 'green' : 'blue']"
          >{{ statusLabels[job.status] }}</span
        >
      </div>
      <div v-if="!job" class="empty-state">
        <span class="empty-icon">⌁</span>
        <h3>提案将在这里显示</h3>
        <p>上传后，逐项核对任务名称、截止时间和耗时。</p>
      </div>
      <div v-else-if="job.status === 'COMMITTED'" class="empty-state compact">
        <span class="success-circle">✓</span>
        <h3>任务已保存</h3>
        <p>{{ job.proposal?.title }}</p>
        <router-link to="/tasks" class="subtle-link"
          >前往任务清单并生成排程 ↗</router-link
        >
      </div>
      <form
        v-else-if="job.proposal"
        class="form-stack"
        @submit.prevent="confirm"
      >
        <div v-if="job.errors.length" class="notice warning">
          {{ job.errors.map((e) => e.message).join("；")
          }}<span v-if="job.status === 'FAILED'">请原样重试已确认提案。</span>
        </div>
        <div v-if="job.proposal.ambiguities.length" class="notice">
          {{ job.proposal.ambiguities.join("；") }}
        </div>
        <label
          >任务名称<input
            v-model="form.title"
            required
            maxlength="200"
            :disabled="job.status === 'FAILED'" /></label
        ><label
          >课程名称候选 <small>暂不自动关联正式课程</small
          ><input
            v-model="form.courseCandidate"
            :disabled="job.status === 'FAILED'" /></label
        ><label
          >截止时间（北京时间）<input
            v-model="form.deadline"
            type="datetime-local"
            required
            :disabled="job.status === 'FAILED'" /></label
        ><label
          >预计耗时（分钟）<input
            v-model.number="form.durationMinutes"
            type="number"
            min="1"
            max="10080"
            required
            :disabled="job.status === 'FAILED'"
        /></label>
        <div class="evidence-box">
          <small>来源证据</small>
          <div v-for="e in job.proposal.sourceEvidence" :key="e">{{ e }}</div>
        </div>
        <p class="form-hint">点击确认表示已核对并解决上述待补充信息。</p>
        <button class="primary" :disabled="busy">
          {{
            job.status === "FAILED" ? "重试确认保存" : "已核对，确认创建任务"
          }}
        </button>
      </form>
      <div v-else class="notice warning">
        {{
          job.errors.map((e) => e.message).join("；") || "请重新上传或刷新状态"
        }}<button class="text-button" @click="recover">刷新状态</button>
      </div>
    </section>
  </div>
</template>

