<template>
  <el-dialog
    v-model="visible"
    title="动作预览"
    width="560px"
    :close-on-click-modal="false"
    @closed="emit('closed')"
  >
    <!-- 版本冲突提示 -->
    <ConflictBanner
      :visible="conflictVisible"
      :code="conflictCode"
      :message="conflictMessage"
      @refresh="onRefresh"
    />

    <el-descriptions :column="1" border>
      <el-descriptions-item label="动作类型">
        {{ actionTypeLabel }}
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <ActionStatusTag :status="action.status" />
      </el-descriptions-item>
      <el-descriptions-item label="目标对象">
        <el-tag v-for="t in action.targets" :key="t" size="small" class="target-tag">
          {{ t }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="变更内容">
        <pre class="changes-pre">{{ changesText }}</pre>
      </el-descriptions-item>
      <el-descriptions-item label="理由依据">
        <ul v-if="action.reasonFacts.length" class="reason-list">
          <li v-for="(r, i) in action.reasonFacts" :key="i">{{ r }}</li>
        </ul>
        <span v-else>—</span>
      </el-descriptions-item>
      <el-descriptions-item label="基线版本">
        revision {{ action.baseRevision }}
      </el-descriptions-item>
    </el-descriptions>

    <!-- 需要补充信息 -->
    <el-alert
      v-if="action.status === 'NEEDS_CLARIFICATION'"
      type="info"
      title="信息不足，需要补充"
      class="mt-12"
      :closable="false"
    >
      <ul v-if="action.errors.length">
        <li v-for="(e, i) in action.errors" :key="i">
          [{{ e.code }}] {{ e.message }}
        </li>
      </ul>
    </el-alert>

    <!-- 失败 -->
    <el-alert
      v-if="action.status === 'FAILED'"
      type="error"
      title="动作执行失败"
      class="mt-12"
      :closable="false"
    >
      <span v-if="action.errors.length">
        {{ action.errors.map((e) => e.message).join('；') }}
      </span>
    </el-alert>

    <template #footer>
      <span class="dialog-footer">
        <!-- 已执行：提供撤销入口 -->
        <template v-if="action.status === 'APPLIED'">
          <el-button @click="emit('close')">关闭</el-button>
          <el-button type="warning" :loading="undoLoading" @click="onUndo">
            撤销此操作
          </el-button>
        </template>
        <!-- 待确认：确认/取消 -->
        <template v-else-if="action.status === 'NEEDS_CONFIRMATION'">
          <el-button @click="emit('close')">取消</el-button>
          <el-button type="primary" :loading="confirmLoading" @click="onConfirm">
            确认执行
          </el-button>
        </template>
        <!-- 其他状态（待补充/失败）：只有关闭 -->
        <el-button v-else @click="emit('close')">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { ActionSpec } from '@/types/contract'
import { ACTION_TYPE_LABEL } from '@/types/contract'
import { confirmAction, undoAction } from '@/api/actions'
import { ApiError } from '@/api/client'
import ActionStatusTag from './ActionStatusTag.vue'
import ConflictBanner from './ConflictBanner.vue'

const props = defineProps<{
  modelValue: boolean
  action: ActionSpec | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'confirmed', action: ActionSpec): void
  (e: 'undone', action: ActionSpec): void
  (e: 'refresh'): void
  (e: 'close'): void
  (e: 'closed'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const confirmLoading = ref(false)
const undoLoading = ref(false)
const conflictVisible = ref(false)
const conflictCode = ref('')
const conflictMessage = ref('')

const actionTypeLabel = computed(() =>
  props.action ? ACTION_TYPE_LABEL[props.action.type] : ''
)

const changesText = computed(() => {
  if (!props.action) return ''
  return JSON.stringify(props.action.changes, null, 2)
})

// 每次打开新动作时重置冲突提示
watch(
  () => props.action,
  () => {
    conflictVisible.value = false
  }
)

async function onConfirm() {
  if (!props.action) return
  confirmLoading.value = true
  conflictVisible.value = false
  try {
    const updated = await confirmAction(props.action)
    ElMessage.success('动作已执行')
    emit('confirmed', updated)
  } catch (err) {
    if (err instanceof ApiError && err.httpStatus === 409) {
      conflictVisible.value = true
      conflictCode.value = err.code
      conflictMessage.value = err.message
    } else {
      ElMessage.error((err as Error).message)
    }
  } finally {
    confirmLoading.value = false
  }
}

async function onUndo() {
  if (!props.action) return
  undoLoading.value = true
  try {
    await undoAction({
      operationKey: `undo-${props.action.operationKey}`,
      executionId: props.action.operationKey,
      baseRevision: props.action.baseRevision
    })
    ElMessage.success('已撤销')
    emit('undone', props.action)
  } catch (err) {
    ElMessage.error((err as Error).message)
  } finally {
    undoLoading.value = false
  }
}

function onRefresh() {
  conflictVisible.value = false
  emit('refresh')
}
</script>

<style scoped>
.target-tag {
  margin-right: 6px;
}
.changes-pre {
  margin: 0;
  font-size: 12px;
  background: var(--el-fill-color-light);
  padding: 8px;
  border-radius: 4px;
  max-height: 200px;
  overflow: auto;
}
.reason-list {
  margin: 0;
  padding-left: 20px;
}
.mt-12 {
  margin-top: 12px;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
