<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
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
const remaining = computed(
  () =>
    snapshot.value?.tasks.filter(
      (t) => !["DONE", "CANCELLED"].includes(t.status),
    ).length ?? 0,
);
const router = useRouter();
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
      <div class="sidebar-label">学习工作台</div>
      <nav aria-label="主导航">
        <router-link
          v-for="link in links.slice(0, 3)"
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
      <div class="sidebar-focus">
        <span class="eyebrow">一步一步，慢慢完成</span>
        <strong>{{ remaining }} <small>项待完成</small></strong>
        <p>把下一步安排好，留出专注的时间。</p>
        <router-link to="/tasks"
          >查看我的任务 <span aria-hidden="true">→</span></router-link
        >
      </div>
      <div class="sidebar-bottom">
        <span class="avatar">D</span>
        <div>我的学习空间<small>北京时间 · 本地保存</small></div>
        <span class="online-dot"></span>
      </div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <div class="breadcrumb">
          <span class="breadcrumb-home">学习工作台 / </span>
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
          <el-dropdown
            trigger="click"
            popper-class="workspace-settings"
            @command="router.push($event)"
          >
            <button
              class="settings-button"
              :class="{ active: ['/import', '/profile'].includes(route.path) }"
              aria-label="设置"
            >
              <span aria-hidden="true">⚙</span> 设置
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="link in links.slice(3)"
                  :key="link.path"
                  :command="link.path"
                >
                  {{ link.icon }} {{ link.name }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <div v-if="connectionError" class="connection-error" role="alert">
        {{ connectionError }} <button @click="safely(refresh)">重新连接</button>
      </div>
      <main id="main-content">
        <router-view v-if="snapshot" />
        <div v-else class="empty-state">
          {{
            loading ? "正在连接学习空间…" : "尚未连接后端，请启动服务后刷新。"
          }}
        </div>
      </main>
      <footer>
        学习日程 · 留一点时间给自己
        <span
          >数据保存在本机 · 日程版本
          {{ snapshot?.planningRevision ?? "—" }}</span
        >
      </footer>
    </div>
    <HistoryDrawer v-model="historyOpen" />
  </div>
</template>
