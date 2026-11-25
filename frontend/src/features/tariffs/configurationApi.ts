import { request } from '../../services/httpClient'

export type Site = { id: number; name: string }
export type Rates = { day: number | null; night: number | null; capacity: number | null }
export type Configuration = { dayStart: string | null; nightStart: string | null; car: Rates; motorcycle: Rates }

export function listSites(signal: AbortSignal) {
  return request<Site[]>('/api/v1/admin/sites', { signal })
}
export function loadConfiguration(site: number, signal: AbortSignal) {
  return request<Configuration>(`/api/v1/admin/sites/${site}/configuration`, { signal })
}
export function saveConfiguration(site: number, data: Configuration) {
  return request<void>(`/api/v1/admin/sites/${site}/configuration`, { method: 'PUT', body: JSON.stringify(data) })
}
