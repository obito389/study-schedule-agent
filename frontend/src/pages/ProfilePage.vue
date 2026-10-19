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
      <ProfileFactList :facts="facts" :loading="loading" @changed="loadFacts" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ProfileFact } from '@/types/contract'
import { listProfileFacts } from '@/api/profile'
import ProfileFactList from '@/components/ProfileFactList.vue'

const facts = ref<ProfileFact[]>([])
const loading = ref(false)

async function loadFacts() {
  loading.value = true
  try {
    facts.value = await listProfileFacts()
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
