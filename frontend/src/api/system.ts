import { http } from './http'

export interface SystemMenu {
  id: number
  name: string
  path: string
  permissionCode: string | null
  sortOrder: number | null
}

export interface SystemUser {
  id: number
  username: string
  realName: string
  phone: string
  departmentCode: string | null
  departmentName: string | null
  roles: string[]
  status: string
}

export interface OperationLog {
  id: number
  operatorUsername: string
  realName: string
  department: string
  phone: string
  role: string
  operationRecord: string
}

export interface SystemRole {
  id: number
  code: string
  name: string
}

export interface AssignUserRolesRequest {
  roleCodes: string[]
}

export const fetchMenus = () => {
  return http.get<unknown, SystemMenu[]>('/system/menus')
}

export const fetchUsers = () => {
  return http.get<unknown, SystemUser[]>('/system/users')
}

export const fetchRoles = (targetUserId: number) => {
  // 角色可选范围由后端按“被分配人”计算，前端必须传 targetUserId 才能保持三层角色隔离。
  return http.get<unknown, SystemRole[]>('/system/roles', { params: { targetUserId } })
}

export const fetchOperationLogs = () => {
  return http.get<unknown, OperationLog[]>('/system/logs')
}

export const assignUserRoles = (userId: number, request: AssignUserRolesRequest) => {
  return http.put<unknown, SystemUser, AssignUserRolesRequest>(`/system/users/${userId}/roles`, request)
}
