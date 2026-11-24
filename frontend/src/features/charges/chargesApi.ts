import { postJson, request } from '../../services/httpClient'

export type ChargeOutcome = 'CREADO' | 'EXISTENTE' | 'EXENTO'
export type ChargeStatus = 'PENDIENTE' | 'PAGADO' | 'VENCIDO' | 'ANULADO'

export type ChargeResult = {
  plate: string
  vehicleType: 'CARRO' | 'MOTO'
  operatingDay: string
  timeSlot: 'DIURNA' | 'NOCTURNA'
  outcome: ChargeOutcome
  chargeId: number | null
  appliedTariff: number | null
  amount: number
  status: ChargeStatus | null
  generatedAt: string | null
}

/** Calcula y registra el cobro del día operativo para el ingreso de una placa (HU-14 y HU-15). */
export function generateCharge(plate: string): Promise<ChargeResult> {
  return postJson<ChargeResult>('/api/v1/charges', { plate })
}

/** Consulta el cobro de la placa en el día operativo actual. */
export function findTodayCharge(plate: string): Promise<ChargeResult> {
  return request<ChargeResult>(`/api/v1/charges/today?plate=${encodeURIComponent(plate.trim())}`)
}
