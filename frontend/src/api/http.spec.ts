import { AxiosHeaders, type AxiosAdapter, type AxiosResponse } from 'axios'
import { describe, expect, it } from 'vitest'

import { http } from './http'

const createAdapter = (response: Omit<AxiosResponse, 'config'>): AxiosAdapter => {
  return async (config) => ({
    ...response,
    config
  })
}

describe('http response interceptor', () => {
  it('returns blob data directly without unwrapping an envelope', async () => {
    const file = new Blob(['avatar-bytes'], { type: 'image/png' })
    const adapter = createAdapter({
      data: file,
      status: 200,
      statusText: 'OK',
      headers: new AxiosHeaders({ 'content-type': 'image/png' }),
      request: {}
    })

    const result = await http.get<unknown, Blob>('/files/1', {
      responseType: 'blob',
      adapter
    })

    expect(result).toBe(file)
  })

  it('still unwraps JSON envelope responses', async () => {
    const user = { id: 1, name: 'admin' }
    const adapter = createAdapter({
      data: { success: true, data: user },
      status: 200,
      statusText: 'OK',
      headers: new AxiosHeaders({ 'content-type': 'application/json; charset=utf-8' }),
      request: {}
    })

    const result = await http.get<unknown, typeof user>('/auth/me', { adapter })

    expect(result).toBe(user)
  })
})
