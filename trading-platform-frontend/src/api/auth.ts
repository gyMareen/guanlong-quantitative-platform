import { request } from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
  phone?: string
  nickname?: string
}

export interface UserInfo {
  id: number
  username: string
  email: string
  nickname: string
  avatar: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

export const login = (data: LoginRequest): Promise<LoginResponse> => {
  return request.post('/auth/login', data)
}

export const register = (data: RegisterRequest): Promise<void> => {
  return request.post('/auth/register', data)
}

export const logout = (): Promise<void> => {
  return request.post('/auth/logout')
}

export const getCurrentUser = (): Promise<UserInfo> => {
  return request.get('/auth/me')
}
