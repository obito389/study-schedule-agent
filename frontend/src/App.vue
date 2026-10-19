<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="logo">学习日程 Agent</div>
      <el-menu
        mode="horizontal"
        :default-active="activeMenu"
        router
        class="app-menu"
      >
        <el-menu-item index="/">日程日历</el-menu-item>
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
</style>
