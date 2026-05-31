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
  phone: string
  departmentCode: string | null
  departmentName: string | null
  roles: string[]
  status: string
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

export const fetchRoles = () => {
  return http.get<unknown, SystemRole[]>('/system/roles')
}

export const assignUserRoles = (userId: number, request: AssignUserRolesRequest) => {
  return http.put<unknown, SystemUser, AssignUserRolesRequest>(`/system/users/${userId}/roles`, request)
}
