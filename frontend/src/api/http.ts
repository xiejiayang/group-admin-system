import axios, { type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'

interface ApiEnvelope<T> {
  success: boolean
  data: T
  message?: string
}

export const TOKEN_STORAGE_KEY = 'token'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

const messageOrDefault = (message: unknown, fallback: string) => {
  return typeof message === 'string' && message.trim().length > 0 ? message : fallback
}

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)

  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }

  return config
})

http.interceptors.response.use(
  (response: AxiosResponse<ApiEnvelope<unknown>>) => {
    const payload = response.data

    if (payload?.success) {
      return payload.data as never
    }

    throw new Error(messageOrDefault(payload?.message, '请求失败，请稍后重试'))
  },
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    const serverMessage = error.response?.data?.message

    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new Error('请求超时，请稍后重试'))
    }

    return Promise.reject(new Error(messageOrDefault(serverMessage, '网络异常，请稍后重试')))
  }
)
