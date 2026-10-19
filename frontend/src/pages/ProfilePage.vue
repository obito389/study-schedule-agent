<template>
  <div class="profile-page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="page-header">
          <span>用户事实管理</span>
          <el-button size="small" @click="loadFacts">刷新</el-button>
        </div>
      </template>
      <p class="page-desc">
        这里展示系统学到的用户偏好、耗时倍率和临时例外。每条事实都有来源、证据和版本；
        你可以修改、否认或删除它们。被删除或过期的事实不再参与排程。
      </p>
      <el-form inline @submit.prevent="saveLimit"><el-form-item label="每日学习上限（分钟）"><el-input-number v-model="dailyLimit" :min="30" :max="1440" /></el-form-item><el-form-item><el-button type="primary" @click="saveLimit">确认设置上限</el-button></el-form-item></el-form>
      <ProfileFactList :facts="facts" :loading="loading" @changed="loadFacts" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ProfileFact } from '@/types/contract'
import { listProfileFacts, setDailyLimit } from '@/api/profile'
import ProfileFactList from '@/components/ProfileFactList.vue'

const facts = ref<ProfileFact[]>([])
const loading = ref(false)
const dailyLimit = ref(240)
async function saveLimit() {
  try { await setDailyLimit(dailyLimit.value); ElMessage.success('已保存'); await loadFacts() }
  catch (err) { ElMessage.error((err as Error).message) }
}

async function loadFacts() {
  loading.value = true
  try {
    facts.value = await listProfileFacts()
    const limit = facts.value.find(f => f.key === 'planning.dailyLimitMinutes' && f.state === 'ACTIVE')
    dailyLimit.value = typeof limit?.value === 'number' ? limit.value : 240
  } catch (err) {
    ElMessage.error((err as Error).message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadFacts()
})
</script>

<style scoped>
.profile-page {
  max-width: 1100px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.page-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 0 0 16px;
}
</style>
