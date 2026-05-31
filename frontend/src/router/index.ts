import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import AppLayout from '@/layouts/AppLayout.vue'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/auth/LoginView.vue'
import RegisterView from '@/views/auth/RegisterView.vue'
import GeneralAdminView from '@/views/general-admin/GeneralAdminView.vue'
import PartyHrView from '@/views/party-hr/PartyHrView.vue'
import UserSettingsView from '@/views/settings/UserSettingsView.vue'

const SUPER_ADMIN_ROLE = 'SUPER_ADMIN'
const PARTY_HR_DEPARTMENT = 'PARTY_HR'
const GENERAL_ADMIN_DEPARTMENT = 'GENERAL_ADMIN'
const PARTY_HR_ROLES = new Set(['PARTY_HR', 'PARTY_HR_USER'])
const GENERAL_ADMIN_ROLES = new Set(['GENERAL_ADMIN', 'GENERAL_ADMIN_USER'])

const normalizedPath = (path: string) => {
  return path.split(/[?#]/)[0] || '/'
}

const hasAnyRole = (roles: string[], acceptedRoles: Set<string>) => {
  return roles.some((role) => acceptedRoles.has(role))
}

const hasPartyHrAccess = (roles: string[], departmentCode?: string | null) => {
  return departmentCode === PARTY_HR_DEPARTMENT || hasAnyRole(roles, PARTY_HR_ROLES)
}

const hasGeneralAdminAccess = (roles: string[], departmentCode?: string | null) => {
  return departmentCode === GENERAL_ADMIN_DEPARTMENT || hasAnyRole(roles, GENERAL_ADMIN_ROLES)
}

export const defaultLandingPath = (roles: string[], departmentCode?: string | null) => {
  if (roles.includes(SUPER_ADMIN_ROLE)) {
    return '/party-hr'
  }

  if (hasPartyHrAccess(roles, departmentCode)) {
    return '/party-hr'
  }

  if (hasGeneralAdminAccess(roles, departmentCode)) {
    return '/general-admin'
  }

  return '/party-hr'
}

export const canAccessPath = (path: string, roles: string[], departmentCode?: string | null) => {
  const targetPath = normalizedPath(path)

  // 路由权限先放行超级管理员，再按业务域和部门身份做前端兜底校验。
  if (roles.includes(SUPER_ADMIN_ROLE)) {
    return true
  }

  if (targetPath.startsWith('/settings')) {
    return false
  }

  if (targetPath.startsWith('/party-hr')) {
    return hasPartyHrAccess(roles, departmentCode)
  }

  if (targetPath.startsWith('/general-admin')) {
    return hasGeneralAdminAccess(roles, departmentCode)
  }

  return true
}

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
  const departmentCode = authStore.user?.departmentCode
  const landingPath = defaultLandingPath(roles, departmentCode)

  if (to.path === '/login') {
    return { path: landingPath }
  }

  if (requiresAuth && !canAccessPath(to.path, roles, departmentCode)) {
    return to.path === landingPath ? true : { path: landingPath }
  }

  return true
})

export default router
