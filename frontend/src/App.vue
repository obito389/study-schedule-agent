<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import {
  snapshot,
  loading,
  connectionError,
  refresh,
  safely,
} from "./composables/usePlanner";
import HistoryDrawer from "./components/HistoryDrawer.vue";
import { api } from "./api/client";
const aiConfigured = ref(false);
const route = useRoute(),
  historyOpen = ref(false);
const links = [
  { path: "/ai", icon: "✧", name: "AI 学习助手", sub: "DEEPSEEK AI" },
  { path: "/", icon: "▦", name: "学习日历", sub: "CALENDAR" },
  { path: "/tasks", icon: "☑", name: "任务清单", sub: "TASKS" },
  { path: "/import", icon: "↥", name: "导入与校对", sub: "IMPORT" },
  { path: "/profile", icon: "◉", name: "偏好与反馈", sub: "PROFILE" },
];
onMounted(() => {
  safely(refresh);
  api
    .get("/ai/status")
    .then((r) => {
      aiConfigured.value = r.data.configured;
    })
    .catch(() => {});
});
</script>
<template>
  <div class="app-shell">
    <aside class="sidebar">
      <router-link to="/" class="brand"
        ><span class="brand-mark">学</span>
        <div>学习日程<small>STUDY PLANNER</small></div></router-link
      >
      <div class="sidebar-label">我的学习空间</div>
      <nav>
        <router-link
          v-for="link in links"
          :key="link.path"
          :to="link.path"
          :class="{
            active: route.path === link.path,
            'primary-nav': ['/ai', '/', '/tasks'].includes(link.path),
          }"
          ><span class="nav-icon">{{ link.icon }}</span
          ><span
            >{{ link.name }}<small>{{ link.sub }}</small></span
          ></router-link
        >
      </nav>
      <div class="sidebar-bottom">
        <span class="avatar">D</span>
        <div>本地演示用户<small>Asia / Shanghai</small></div>
        <span class="online-dot"></span>
      </div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <div class="breadcrumb">
          我的学习空间 <span>/</span>
          {{ links.find((l) => l.path === route.path)?.name }}
        </div>
        <div class="topbar-actions">
          <span class="mode-tag">{{
            aiConfigured ? "DeepSeek 已配置" : "AI 未配置 · 规则可用"
          }}</span
          ><button class="text-button" @click="historyOpen = true">
            执行记录 ↗</button
          ><button
            class="icon-button"
            aria-label="刷新数据"
            :disabled="loading"
            @click="safely(refresh)"
          >
            ↻
          </button>
        </div>
      </header>
      <div v-if="connectionError" class="connection-error" role="alert">
        {{ connectionError }} <button @click="safely(refresh)">重新连接</button>
      </div>
      <main>
        <router-view v-if="snapshot" />
        <div v-else class="empty-state">
          {{
            loading ? "正在连接学习空间…" : "尚未连接后端，请启动服务后刷新。"
          }}
        </div>
      </main>
      <footer>
        学习日程 Agent · 本地课程设计演示
        <span
          >数据保存在本机 · 日程版本
          {{ snapshot?.planningRevision ?? "—" }}</span
        >
      </footer>
    </div>
    <HistoryDrawer v-model="historyOpen" />
  </div>
</template>
