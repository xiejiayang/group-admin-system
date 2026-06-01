import { http } from './http'

export interface FileUploadResponse {
  id: number
  originalName: string
  url: string
  sizeBytes: number
}

export const fileReadUrl = (id: number) => `/api/files/${id}`

export const fetchFileBlob = (id: number) => {
  return http.get<unknown, Blob>(`/files/${id}`, { responseType: 'blob' })
}

export const uploadIdPhoto = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)

  return http.post<unknown, FileUploadResponse, FormData>('/files/id-photo', formData)
}
