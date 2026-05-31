import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import AppLayout from '@/layouts/AppLayout.vue'
import { accessibleLandingPath, canAccessPath } from '@/router/access'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import LoginView from '@/views/auth/LoginView.vue'
import RegisterView from '@/views/auth/RegisterView.vue'
import ForbiddenView from '@/views/ForbiddenView.vue'
import GeneralAdminView from '@/views/general-admin/GeneralAdminView.vue'
import PartyHrView from '@/views/party-hr/PartyHrView.vue'
import UserSettingsView from '@/views/settings/UserSettingsView.vue'

export { canAccessPath, defaultLandingPath } from '@/router/access'

const routes: RouteRecordRaw[] = [
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
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/party-hr'
      },
      {
        path: 'party-hr',
        component: PartyHrView,
        meta: { requiresAuth: true }
      },
      {
        path: 'general-admin',
        component: GeneralAdminView,
        meta: { requiresAuth: true }
      },
      {
        path: 'forbidden',
        component: ForbiddenView,
        meta: { requiresAuth: true }
      },
      {
        path: 'settings',
        redirect: '/settings/users',
        meta: { requiresAuth: true }
      },
      {
        path: 'settings/users',
        component: UserSettingsView,
        meta: { requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const shouldLoadCurrentUser = () => {
  const authStore = useAuthStore()
  return authStore.token && !authStore.user
}

const resolveLandingPath = async (roles: string[], permissions: string[], departmentCode?: string | null) => {
  const menuStore = useMenuStore()

  try {
    const menus = await menuStore.loadMenus()
    return accessibleLandingPath(roles, permissions, departmentCode, menus)
  } catch {
    return accessibleLandingPath(roles, permissions, departmentCode, menuStore.menus)
  }
}

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)

  if (!authStore.token) {
    return requiresAuth ? { path: '/login', query: { redirect: to.fullPath } } : true
  }

  if (shouldLoadCurrentUser()) {
    try {
      await authStore.loadCurrentUser()
    } catch {
      return requiresAuth ? { path: '/login', query: { redirect: to.fullPath } } : true
    }
  }

  const roles = authStore.user?.roles ?? []
  const permissions = authStore.user?.permissions ?? []
  const departmentCode = authStore.user?.departmentCode

  if (to.path === '/login') {
    const landingPath = await resolveLandingPath(roles, permissions, departmentCode)
    return { path: landingPath }
  }

  if (requiresAuth && to.path === '/') {
    const landingPath = await resolveLandingPath(roles, permissions, departmentCode)
    return { path: landingPath }
  }

  if (requiresAuth && !canAccessPath(to.path, roles, permissions, departmentCode)) {
    const landingPath = await resolveLandingPath(roles, permissions, departmentCode)
    return { path: landingPath, replace: true }
  }

  return true
})

export default router
