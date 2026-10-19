<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="logo">学习日程 Agent · 团队版</div>
      <el-menu
        mode="horizontal"
        :ellipsis="false"
        :default-active="activeMenu"
        router
        class="app-menu"
      >
        <el-menu-item index="/">日程日历</el-menu-item>
        <el-menu-item index="/tasks">任务管理</el-menu-item>
        <el-menu-item index="/import">导入与校对</el-menu-item>
        <el-menu-item index="/profile">用户事实</el-menu-item>
      </el-menu>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.app-container {
  min-height: 100vh;
}
.app-header {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-light);
  padding: 0 24px;
}
.logo {
  font-size: 18px;
  font-weight: 600;
  margin-right: 40px;
  white-space: nowrap;
}
.app-menu {
  border-bottom: none;
  flex: 1;
}
.app-main {
  background: var(--el-bg-color-page);
  padding: 24px;
}
@media (max-width: 700px) {
  .app-header { height: auto; flex-direction: column; align-items: stretch; padding: 12px 16px 0; }
  .logo { margin-right: 0; padding-bottom: 8px; }
  .app-menu { width: 100%; --el-menu-horizontal-height: 44px; }
  .app-menu :deep(.el-menu-item) { flex: 1; padding: 0 6px; font-size: 13px; }
  .app-main { padding: 12px; }
}
:global(.el-dialog) { max-width: calc(100vw - 24px); }
</style>
