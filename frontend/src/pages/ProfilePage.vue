<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessageBox } from "element-plus";
import { api } from "../api/client";
import { snapshot, refresh, safely } from "../composables/usePlanner";
import type { ProfileFact } from "../types/contracts";
const history = ref<ProfileFact[]>([]),
  limit = ref(240),
  busy = ref(false),
  original = ref(60);
const estimate = ref<{
  minutes: number;
  multiplier: number;
  sampleCount: number;
  evidenceIds: string[];
} | null>(null);
const latest = computed(() => {
  const values = new Map<string, ProfileFact>();
  history.value.forEach((f) => values.set(f.key + "|" + f.scope, f));
  return [...values.values()].reverse();
});
async function load() {
  history.value = (
    await api.get<{ history: ProfileFact[] }>("/profile")
  ).data.history;
  const active = snapshot.value?.activeFacts.find(
    (f) => f.key === "planning.dailyLimitMinutes",
  );
  if (active) limit.value = Number(active.value);
  await calculate();
}
async function calculate() {
  estimate.value = (
    await api.get("/profile/duration-estimate", {
      params: { minutes: original.value },
    })
  ).data;
}
async function setPreference() {
  busy.value = true;
  await safely(async () => {
    await api.post("/profile/preferences", {
      key: "planning.dailyLimitMinutes",
      value: limit.value,
      action: "SET",
      baseRevision: snapshot.value!.planningRevision,
    });
    await refresh();
    await load();
  });
  busy.value = false;
}
async function changeFact(f: ProfileFact, action: "DELETE" | "DENY") {
  try {
    await ElMessageBox.confirm(
      action === "DELETE"
        ? "删除后将不再使用该证据，并使旧排程预览失效。"
        : "否认后将停止使用该事实。",
      "确认更新事实",
      { confirmButtonText: "确认", cancelButtonText: "返回" },
    );
  } catch {
    return;
  }
  busy.value = true;
  await safely(async () => {
    await api.post("/profile/preferences", {
      key: f.key,
      value: null,
      action,
      baseRevision: snapshot.value!.planningRevision,
    });
    await refresh();
    await load();
  });
  busy.value = false;
}
function description(f: ProfileFact) {
  if (f.key === "planning.dailyLimitMinutes") return `每日最多 ${f.value} 分钟`;
  const v = f.value as Record<string, number>;
  return `预计 ${v.estimatedMinutes} 分钟 / 实际 ${v.actualMinutes} 分钟`;
}
onMounted(() => safely(load));
</script>
<template>
  <div class="profile-grid">
    <section class="panel padded">
      <div class="section-icon">◷</div>
      <h2>每日学习上限</h2>
      <form class="form-stack" @submit.prevent="setPreference">
        <label
          >每天最多安排多少分钟？<input
            v-model.number="limit"
            type="number"
            min="30"
            max="1440"
            required
            data-testid="daily-limit"
        /></label>
        <p class="form-hint">已有安排超过新上限时会拒绝修改，请先调整日程。</p>
        <button class="primary" :disabled="busy" data-testid="save-preference">
          确认保存偏好
        </button>
      </form>
    </section>
    <section class="panel padded">
      <div class="section-icon warm">✧</div>
      <h2>耗时估计参考</h2>
      <p class="muted">在任务清单中完成任务并记录实际耗时，逐步积累样本。</p>
      <form class="inline-form" @submit.prevent="safely(calculate)">
        <label
          >原预计（分钟）<input
            v-model.number="original"
            type="number"
            min="1"
            max="10080"
            required /></label
        ><button class="secondary">查看建议</button>
      </form>
      <div v-if="estimate" class="estimate-grid">
        <div>
          <strong>{{ estimate.multiplier.toFixed(2) }}<small>×</small></strong
          ><span>平滑倍率</span>
        </div>
        <div>
          <strong>{{ estimate.sampleCount }}</strong
          ><span>有效样本</span>
        </div>
        <div>
          <strong>{{ estimate.minutes }}<small>min</small></strong
          ><span>建议耗时</span>
        </div>
      </div>
      <p class="form-hint">
        少于 3 个有效样本时保留原估计；倍率范围为
        0.5–2.0。建议不会自动覆盖已有任务耗时。
      </p>
    </section>
  </div>
  <section class="panel">
    <div class="panel-heading">
      <div>
        <h2>我的事实与证据</h2>
        <p class="muted">保留历史版本，可否认或删除当前事实。</p>
      </div>
      <span class="badge blue">{{ history.length }} 条历史记录</span>
    </div>
    <div v-if="!latest.length" class="empty-state compact">
      保存一个偏好，或完成一个任务，证据就会出现在这里。
    </div>
    <div v-else class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>内容</th>
            <th>来源</th>
            <th>状态 / 版本</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="f in latest" :key="f.key + '|' + f.scope">
            <td>
              <strong>{{ description(f) }}</strong
              ><small class="table-sub break-word">{{
                f.evidenceIds.join(" · ")
              }}</small>
            </td>
            <td>
              {{
                f.source === "TASK_COMPLETION"
                  ? "任务完成反馈"
                  : f.source === "USER_EXPLICIT"
                    ? "用户明确设置"
                    : "用户反馈"
              }}
            </td>
            <td>
              <span
                :class="['badge', f.state === 'ACTIVE' ? 'green' : 'neutral']"
                >{{
                  { ACTIVE: "生效中", DENIED: "已否认", DELETED: "已删除" }[
                    f.state
                  ]
                }}</span
              ><small class="table-sub">v{{ f.version }}</small>
            </td>
            <td>
              <div v-if="f.state === 'ACTIVE'" class="row-actions">
                <button :disabled="busy" @click="changeFact(f, 'DENY')">
                  否认</button
                ><button :disabled="busy" @click="changeFact(f, 'DELETE')">
                  删除
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

