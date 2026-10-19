<script setup lang="ts">
import { ref } from "vue";
import ConflictBanner from "./ConflictBanner.vue";
import { api } from "../api/client";
import {
  snapshot,
  runCommand,
  safely,
  refresh,
} from "../composables/usePlanner";
import { formatTime } from "../lib/time";
import type { Preview } from "../types/contracts";
const open = ref(false),
  busy = ref(false),
  preview = ref<Preview | null>(null);
async function generate() {
  busy.value = true;
  await safely(async () => {
    preview.value = (await api.post<Preview>("/planning/previews")).data;
    open.value = true;
  });
  busy.value = false;
}
async function save() {
  if (!preview.value) return;
  busy.value = true;
  await safely(async () => {
    await runCommand(
      "APPLY_PLAN",
      { previewId: preview.value!.previewId },
      preview.value!.baseRevision,
    );
    open.value = false;
  });
  busy.value = false;
}
async function regenerate() {
  busy.value = true;
  await safely(async () => {
    await refresh();
    preview.value = (await api.post<Preview>("/planning/previews")).data;
  });
  busy.value = false;
}
const title = (id: string) =>
  snapshot.value?.tasks.find((t) => t.id === id)?.title ?? id;
</script>
<template>
  <button
    class="primary"
    :disabled="busy"
    @click="generate"
    data-testid="generate-plan"
  >
    {{ busy ? "生成中…" : "规则排程预览" }}
  </button>
  <el-dialog v-model="open" title="排程预览 · 等待确认" width="720px">
    <template v-if="preview"
      ><div class="notice">
        以下为规则排程候选，尚未写入日历。版本 {{ preview.baseRevision }} · 30
        分钟时间槽。
      </div>
      <ConflictBanner
        :visible="preview.baseRevision !== snapshot?.planningRevision"
        code="REVISION_CONFLICT"
        message="预览已经过期，请重新生成。"
        @refresh="regenerate"
      />
      <div v-if="preview.candidate.gaps.length" class="notice warning">
        {{ preview.candidate.gaps.length }}
        个任务无法完整排入，未截短安排；请调整截止时间、耗时或可用时段。
        <div v-for="gap in preview.candidate.gaps" :key="gap.taskId">
          {{ title(gap.taskId) }}
        </div>
      </div>
      <div v-if="!preview.candidate.blocks.length" class="empty-state compact">
        没有可新增的时间块。请先创建待办任务，或查看已有日程。
      </div>
      <div v-else class="preview-list">
        <div
          v-for="(block, i) in preview.candidate.blocks"
          :key="i"
          class="preview-row"
        >
          <span class="number-dot">{{ i + 1 }}</span
          ><strong>{{ title(block.taskId) }}</strong
          ><span
            >{{ formatTime(block.start) }} — {{ formatTime(block.end) }}</span
          >
        </div>
      </div>
      <div class="dialog-actions">
        <button class="secondary" @click="open = false">返回调整</button
        ><button
          class="primary"
          :disabled="
            busy ||
            !preview.candidate.blocks.length ||
            preview.baseRevision !== snapshot?.planningRevision
          "
          @click="save"
          data-testid="confirm-plan"
        >
          {{ busy ? "正在保存…" : "确认保存到日历" }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>
