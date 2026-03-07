import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, logout, getCurrentUser } from '@/api/auth'
import type { LoginRequest, UserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const clearToken = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  const loginAction = async (data: LoginRequest) => {
    const res = await login(data)
    setToken(res.token)
    userInfo.value = res.user
    return res
  }

  const logoutAction = async () => {
    try {
      await logout()
    } finally {
      clearToken()
    }
  }

  const getUserInfo = async () => {
    if (!token.value) return null
    try {
      const res = await getCurrentUser()
      userInfo.value = res
      return res
    } catch {
      clearToken()
      return null
    }
  }

  return {
    token,
    userInfo,
    setToken,
    clearToken,
    loginAction,
    logoutAction,
    getUserInfo
  }
})
