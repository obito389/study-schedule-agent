<template>
  <div class="schedule-page">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="revision-tag">当前 revision：{{ snapshot?.planningRevision ?? '—' }}</span>
          <el-tag v-if="loading" type="info" size="small">加载中…</el-tag>
        </div>
        <div class="toolbar-right">
          <el-button @click="calendarSettings=true">课程 / 固定时段</el-button>
          <el-input
            v-model="prompt"
            placeholder="例如：明晚八点复习数据库一小时"
            class="prompt-input"
            clearable
            @keyup.enter="onRequestAction"
          />
          <el-button :disabled="loading || !snapshot" :loading="actionLoading" @click="onRulePlan">规则排程</el-button>
          <el-button type="primary" :disabled="loading || !snapshot" :loading="actionLoading" @click="onRequestAction">
            生成动作预览
          </el-button>
        </div>
      </div>
    </el-card>

    <CalendarView :snapshot="snapshot" />
    <CalendarSettingsDialog v-model="calendarSettings" @changed="loadSnapshot" />

    <ActionPreviewDialog
      v-model="dialogVisible"
      :action="currentAction"
      @confirmed="onActionConfirmed"
      @undone="onActionUndone"
      @refresh="onConflictRefresh"
      @close="dialogVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ActionSpec, ScheduleSnapshot } from '@/types/contract'
import { getScheduleSnapshot } from '@/api/schedule'
import { requestAction, requestRulePlan } from '@/api/actions'
import CalendarView from '@/components/CalendarView.vue'
import CalendarSettingsDialog from '@/components/CalendarSettingsDialog.vue'
import ActionPreviewDialog from '@/components/ActionPreviewDialog.vue'

const snapshot = ref<ScheduleSnapshot | null>(null)
const calendarSettings = ref(false)
const loading = ref(false)
const actionLoading = ref(false)
const prompt = ref('')
const dialogVisible = ref(false)
const currentAction = ref<ActionSpec | null>(null)

async function loadSnapshot() {
  loading.value = true
  try {
    snapshot.value = await getScheduleSnapshot()
  } catch (err) {
    ElMessage.error((err as Error).message)
  } finally {
    loading.value = false
  }
}

async function onRequestAction() {
  if (!snapshot.value) return
  if (!prompt.value.trim()) { ElMessage.warning('请输入学习要求'); return }
  actionLoading.value = true
  try {
    const action = await requestAction(
      prompt.value.trim(),
      snapshot.value.planningRevision
    )
    currentAction.value = action
    dialogVisible.value = true
  } catch (err) {
    ElMessage.error((err as Error).message)
  } finally {
    actionLoading.value = false
  }
}

function onActionConfirmed(action: ActionSpec) {
  currentAction.value = action
  // 确认执行后刷新快照以同步最新 revision
  loadSnapshot()
}

function onActionUndone(_action: ActionSpec) {
  dialogVisible.value = false
  currentAction.value = null
  loadSnapshot()
}
async function onRulePlan() {
  if (!snapshot.value) return
  actionLoading.value = true
  try { currentAction.value = await requestRulePlan(snapshot.value.planningRevision); dialogVisible.value = true }
  catch (err) { ElMessage.error((err as Error).message) }
  finally { actionLoading.value = false }
}
async function onConflictRefresh() {
  await loadSnapshot()
  if (currentAction.value?.type === 'GENERATE_SCHEDULE') await onRulePlan()
  else await onRequestAction()
}

onMounted(() => {
  loadSnapshot()
})
</script>

<style scoped>
.schedule-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.toolbar-card {
  border: none;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.toolbar-right {
  display: flex;
  gap: 8px;
  flex: 1;
  max-width: 900px;
  flex-wrap: wrap;
}
.prompt-input {
  flex: 1;
}
.revision-tag {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
