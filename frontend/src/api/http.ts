import axios, {
  type AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'

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

type HeaderSource = Record<string, unknown> & {
  get?: (name: string) => unknown
}

const contentTypeOf = (response: AxiosResponse) => {
  const headers = response.headers as HeaderSource
  const contentType =
    typeof headers.get === 'function'
      ? headers.get('content-type')
      : headers['content-type'] ?? headers['Content-Type']

  return Array.isArray(contentType) ? contentType.join(';') : String(contentType ?? '')
}

const isJsonResponse = (response: AxiosResponse) => {
  const contentType = contentTypeOf(response).toLowerCase()

  return contentType.includes('application/json') || contentType.includes('+json')
}

const shouldReturnRawData = (response: AxiosResponse) => {
  const responseType = response.config.responseType

  return responseType === 'blob' || responseType === 'arraybuffer' || !isJsonResponse(response)
}

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)

  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }

  return config
})

http.interceptors.response.use(
  (response: AxiosResponse<ApiEnvelope<unknown> | unknown>) => {
    // 文件下载、图片预览等二进制或非 JSON 响应不走统一 JSON 包装，直接返回原始数据。
    if (shouldReturnRawData(response)) {
      return response.data as never
    }

    const payload = response.data as Partial<ApiEnvelope<unknown>>

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
