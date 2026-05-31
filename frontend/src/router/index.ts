import { defineComponent, h } from 'vue'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/auth/LoginView.vue'
import RegisterView from '@/views/auth/RegisterView.vue'

const placeholderView = (title: string) =>
  defineComponent({
    name: `${title}Placeholder`,
    setup() {
      return () =>
        h('main', { class: 'placeholder-page' }, [
          h('h1', title),
          h('p', '业务页面建设中')
        ])
    }
  })

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    component: LoginView,
    meta: { public: true }
  },
  {
    path: '/register',
    component: RegisterView,
    meta: { public: true }
  },
  {
    path: '/party-hr',
    component: placeholderView('党群人力部'),
    meta: { requiresAuth: true }
  },
  {
    path: '/general-admin',
    component: placeholderView('综合管理部'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) {
    return true
  }

  const authStore = useAuthStore()

  if (!authStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (!authStore.user) {
    try {
      await authStore.loadCurrentUser()
    } catch {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  return true
})

export default router
