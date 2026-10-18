import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import ElementPlus from "element-plus";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import "element-plus/dist/index.css";
import "./style.css";
import App from "./App.vue";
import CalendarPage from "./pages/CalendarPage.vue";
import TasksPage from "./pages/TasksPage.vue";
import ImportPage from "./pages/ImportPage.vue";
import ProfilePage from "./pages/ProfilePage.vue";
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/", component: CalendarPage },
    { path: "/tasks", component: TasksPage },
    { path: "/import", component: ImportPage },
    { path: "/profile", component: ProfilePage },
  ],
});
createApp(App).use(router).use(ElementPlus, { locale: zhCn }).mount("#app");
