import { createRouter, createWebHistory } from 'vue-router'
import SchedulePage from '@/pages/SchedulePage.vue'
import ProfilePage from '@/pages/ProfilePage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'schedule', component: SchedulePage },
    { path: '/profile', name: 'profile', component: ProfilePage }
  ]
})

export default router
