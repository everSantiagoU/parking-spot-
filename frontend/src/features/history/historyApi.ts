import { request } from '../../services/httpClient'

export type HistoryKind = 'events' | 'charges'

export type HistoryFilters = {
  plate: string
  from: string
  to: string
  /** Tipo de evento (INGRESO, VALIDACION, SALIDA) o estado del cobro (PENDIENTE, PAGADO, ...). */
  state: string
  page: number
}

export type EventRow = {
  id: number
  siteId: number
  plate: string
  vehicleType: string
  eventType: string
  source: string
  result: string
  reason: string | null
  occurredAt: string
}

export type ChargeRow = {
  id: number
  siteId: number
  plate: string
  vehicleType: string
  operatingDay: string
  timeSlot: string
  amount: number
  status: string
  generatedAt: string
  validUntil: string | null
}

export type Page<T> = {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export const PAGE_SIZE = 10

/** Arma la URL solo con los filtros que tienen valor. */
function query(filters: HistoryFilters, stateParam: string) {
  const params = new URLSearchParams()
  if (filters.plate.trim()) params.set('plate', filters.plate.trim())
  if (filters.from) params.set('from', filters.from)
  if (filters.to) params.set('to', filters.to)
  if (filters.state) params.set(stateParam, filters.state)
  params.set('page', String(filters.page))
  params.set('size', String(PAGE_SIZE))
  return params.toString()
}

/** HU-20: historial de ingresos, validaciones y salidas. */
export function findEvents(filters: HistoryFilters): Promise<Page<EventRow>> {
  return request<Page<EventRow>>(`/api/v1/history/events?${query(filters, 'type')}`)
}

/** HU-20: historial de cobros. */
export function findCharges(filters: HistoryFilters): Promise<Page<ChargeRow>> {
  return request<Page<ChargeRow>>(`/api/v1/history/charges?${query(filters, 'status')}`)
}
