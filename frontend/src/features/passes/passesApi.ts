import { request } from '../../services/httpClient'

export type PassStatus = 'VIGENTE' | 'VENCIDO' | 'OTRA_SEDE'

export type PassView = {
  plate: string
  chargeId: number
  siteId: number
  operatingDay: string
  validUntil: string
  status: PassStatus
  freeReentry: boolean
}

/** Consulta el pase de una placa en la sede del operador (HU-17). */
export function findPass(plate: string): Promise<PassView> {
  return request<PassView>(`/api/v1/passes?plate=${encodeURIComponent(plate.trim())}`)
}
