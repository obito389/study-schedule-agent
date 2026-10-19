<template>
  <div class="profile-fact-list">
    <el-table :data="facts" v-loading="loading" border stripe>
      <el-table-column prop="key" label="事实键" min-width="220" />
      <el-table-column label="值" min-width="140">
        <template #default="{ row }">
          <span class="value-cell">{{ formatValue(row.value) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="scope" label="范围" width="120" />
      <el-table-column label="置信度" width="100">
        <template #default="{ row }">
          <el-progress
            :percentage="Math.round((row.strength ?? 0) * 100)"
            :stroke-width="8"
            :show-text="false"
          />
          <span class="strength-text">{{ (row.strength ?? 0).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="source" label="来源" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="stateTagType(row.state)" size="small">
            {{ stateLabel(row.state) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="版本" width="70" prop="version" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            type="primary"
            link
            :disabled="row.key !== 'planning.dailyLimitMinutes' || row.state === 'DELETED' || row.state === 'EXPIRED'"
            @click="onEdit(row)"
          >
            修改
          </el-button>
          <el-button
            size="small"
            type="warning"
            link
            :disabled="row.state !== 'ACTIVE'"
            @click="onDeny(row)"
          >
            否认
          </el-button>
          <el-button
            size="small"
            type="danger"
            link
            :disabled="row.state === 'DELETED'"
            @click="onDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 修改弹窗 -->
    <el-dialog v-model="editVisible" title="修改用户事实" width="480px">
      <el-form label-width="80px">
        <el-form-item label="事实键">
          <el-input v-model="editForm.key" disabled />
        </el-form-item>
        <el-form-item label="值">
          <el-input v-model="editForm.valueText" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="置信度">
          <el-slider disabled v-model="editForm.strength" :min="0" :max="1" :step="0.05" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ProfileFact, ProfileFactState } from '@/types/contract'
import {
  PROFILE_FACT_STATE_LABEL,
  PROFILE_FACT_STATE_TAG_TYPE
} from '@/types/contract'
import {
  listProfileFacts,
  updateProfileFact,
  denyProfileFact,
  deleteProfileFact
} from '@/api/profile'

const props = defineProps<{ facts: ProfileFact[]; loading: boolean }>()
const emit = defineEmits<{ (e: 'changed'): void }>()

const editVisible = ref(false)
const saving = ref(false)
const editForm = reactive({
  key: '',
  valueText: '',
  strength: 0.5
})

function formatValue(v: unknown): string {
  if (v === null || v === undefined) return '—'
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

function stateLabel(s: ProfileFactState) {
  return PROFILE_FACT_STATE_LABEL[s]
}
function stateTagType(s: ProfileFactState) {
  return PROFILE_FACT_STATE_TAG_TYPE[s]
}

function onEdit(row: ProfileFact) {
  editForm.key = row.key
  editForm.valueText = formatValue(row.value)
  editForm.strength = 1
  editVisible.value = true
}

async function onSave() {
  const target = props.facts.find((f) => f.key === editForm.key)
  if (!target) return
  saving.value = true
  try {
    let parsedValue: unknown = editForm.valueText
    try {
      parsedValue = JSON.parse(editForm.valueText)
    } catch {
      // 非 JSON 时按字符串保留
    }
    await updateProfileFact({ ...target, value: parsedValue, strength: editForm.strength })
    ElMessage.success('已保存')
    editVisible.value = false
    emit('changed')
  } catch (err) {
    ElMessage.error((err as Error).message)
  } finally {
    saving.value = false
  }
}

async function onDeny(row: ProfileFact) {
  try {
    await ElMessageBox.confirm(
      `否认后该事实将不再用于排程，确认否认「${row.key}」？`,
      '确认否认',
      { type: 'warning' }
    )
    await denyProfileFact(row.key, row.version)
    ElMessage.success('已否认')
    emit('changed')
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') ElMessage.error((err as Error).message)
  }
}

async function onDelete(row: ProfileFact) {
  try {
    await ElMessageBox.confirm(
      `删除后该事实及其证据将标记为已删除，确认删除「${row.key}」？`,
      '确认删除',
      { type: 'warning' }
    )
    await deleteProfileFact(row.key, row.version)
    ElMessage.success('已删除')
    emit('changed')
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') ElMessage.error((err as Error).message)
  }
}

// 重新加载由父页面触发，这里保留 list 供父级调用
defineExpose({ reload: listProfileFacts })
</script>

<style scoped>
.profile-fact-list {
  background: #fff;
  padding: 16px;
  border-radius: 4px;
}
.value-cell {
  font-family: monospace;
  font-size: 12px;
}
.strength-text {
  margin-left: 6px;
  font-size: 12px;
}
</style>
