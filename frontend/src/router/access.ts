import type { SystemMenu } from '@/api/system'

export const SUPER_ADMIN_ROLE = 'SUPER_ADMIN'
export const FORBIDDEN_PATH = '/forbidden'

const PARTY_HR_DEPARTMENT = 'PARTY_HR'
const GENERAL_ADMIN_DEPARTMENT = 'GENERAL_ADMIN'
const PARTY_HR_MENU_PERMISSION = 'menu:party-hr'
const GENERAL_ADMIN_MENU_PERMISSION = 'menu:general-admin'
const APPOINTMENT_MANAGE_PERMISSION = 'appointment:manage'
const SETTINGS_MENU_PERMISSION = 'menu:settings'
const SETTINGS_ADMIN_ROLES = ['PARTY_HR_ADMIN', 'GENERAL_ADMIN_ADMIN']
const SETTINGS_PATHS = ['/settings', '/settings/users', '/settings/logs']

const normalizedPath = (path: string) => {
  return path.split(/[?#]/)[0] || '/'
}

const hasAnyPermission = (permissions: string[], acceptedPermissions: string[]) => {
  return acceptedPermissions.some((permission) => permissions.includes(permission))
}

const hasAnyRole = (roles: string[], acceptedRoles: string[]) => {
  return acceptedRoles.some((role) => roles.includes(role))
}

const canAccessSettingsPath = (targetPath: string, roles: string[], permissions: string[]) => {
  // 设置页承载用户角色和操作日志，必须同时校验管理员角色层级与菜单权限，避免普通用户仅凭菜单码进入。
  return (
    SETTINGS_PATHS.includes(targetPath) &&
    hasAnyRole(roles, SETTINGS_ADMIN_ROLES) &&
    permissions.includes(SETTINGS_MENU_PERMISSION)
  )
}

export const defaultLandingPath = (roles: string[], menus: Pick<SystemMenu, 'path'>[] = []) => {
  const firstMenuPath = menus.map((menuItem) => normalizedPath(menuItem.path)).find((path) => path !== '/')
  if (firstMenuPath) {
    return firstMenuPath
  }

  if (roles.includes(SUPER_ADMIN_ROLE)) {
    return '/party-hr'
  }

  return FORBIDDEN_PATH
}

export const canAccessPath = (
  path: string,
  roles: string[],
  permissions: string[] = [],
  departmentCode?: string | null
) => {
  const targetPath = normalizedPath(path)

  // 前端路由必须和后端权限/菜单一致，避免页面可进但接口返回 403。
  if (roles.includes(SUPER_ADMIN_ROLE)) {
    return true
  }

  if (targetPath === '/' || targetPath.startsWith(FORBIDDEN_PATH)) {
    return true
  }

  if (targetPath.startsWith('/settings')) {
    return canAccessSettingsPath(targetPath, roles, permissions)
  }

  if (targetPath.startsWith('/party-hr')) {
    return (
      departmentCode === PARTY_HR_DEPARTMENT &&
      hasAnyPermission(permissions, [PARTY_HR_MENU_PERMISSION, APPOINTMENT_MANAGE_PERMISSION])
    )
  }

  if (targetPath.startsWith('/general-admin')) {
    return departmentCode === GENERAL_ADMIN_DEPARTMENT && permissions.includes(GENERAL_ADMIN_MENU_PERMISSION)
  }

  return true
}

export const accessibleLandingPath = (
  roles: string[],
  permissions: string[],
  departmentCode: string | null | undefined,
  menus: Pick<SystemMenu, 'path'>[] = []
) => {
  const landingPath = defaultLandingPath(roles, menus)
  if (canAccessPath(landingPath, roles, permissions, departmentCode)) {
    return landingPath
  }

  return roles.includes(SUPER_ADMIN_ROLE) ? '/party-hr' : FORBIDDEN_PATH
}
