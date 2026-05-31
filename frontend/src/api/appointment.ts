import { http } from './http'

import type { AppointmentDetail, AppointmentFormPayload, AppointmentPage } from '@/types/appointment'

export interface AppointmentPageParams {
  page: number
  size: number
}

export const fetchAppointments = (params: AppointmentPageParams) => {
  return http.get<unknown, AppointmentPage>('/party-hr/appointments', { params })
}

export const fetchAppointmentDetail = (id: number) => {
  return http.get<unknown, AppointmentDetail>(`/party-hr/appointments/${id}`)
}

export const createAppointment = (payload: AppointmentFormPayload) => {
  return http.post<unknown, AppointmentDetail, AppointmentFormPayload>('/party-hr/appointments', payload)
}

export const updateAppointment = (id: number, payload: AppointmentFormPayload) => {
  return http.put<unknown, AppointmentDetail, AppointmentFormPayload>(`/party-hr/appointments/${id}`, payload)
}

export const deleteAppointment = (id: number) => {
  return http.delete<unknown, void>(`/party-hr/appointments/${id}`)
}
