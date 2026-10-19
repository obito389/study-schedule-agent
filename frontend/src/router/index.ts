import { createRouter, createWebHistory } from 'vue-router'
import SchedulePage from '@/pages/SchedulePage.vue'
import TasksPage from '@/pages/TasksPage.vue'
import ImportPage from '@/pages/ImportPage.vue'
import ProfilePage from '@/pages/ProfilePage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'schedule', component: SchedulePage },
    { path: '/tasks', name: 'tasks', component: TasksPage },
    { path: '/import', name: 'import', component: ImportPage },
    { path: '/profile', name: 'profile', component: ProfilePage }
  ]
})

export default router
