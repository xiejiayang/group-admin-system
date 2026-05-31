import { http } from './http'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest extends LoginRequest {
  phone: string
  departmentCode: 'PARTY_HR' | 'GENERAL_ADMIN'
}

export interface AuthUser {
  userId: number
  username: string
  phone: string
  departmentCode: string | null
  departmentName: string | null
  roles: string[]
  permissions: string[]
  token: string
}

export const login = (request: LoginRequest) => {
  return http.post<unknown, AuthUser, LoginRequest>('/auth/login', request)
}

export const register = (request: RegisterRequest) => {
  return http.post<unknown, AuthUser, RegisterRequest>('/auth/register', request)
}

export const fetchCurrentUser = () => {
  return http.get<unknown, AuthUser>('/auth/me')
}
