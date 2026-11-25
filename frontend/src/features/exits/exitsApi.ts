import { postJson, request } from '../../services/httpClient'

export const exitReasons = {
  PAGO_CONFIRMADO: 'El cobro del día operativo está pagado.',
  EXENCION_COMPROBADA: 'El ingreso tiene una exención registrada para este día operativo.',
  PAGO_PENDIENTE: 'Falta el pago del cobro del día operativo.',
  COBRO_NO_SATISFECHO: 'El cobro del día operativo figura como vencido o anulado.',
  EXENCION_INDETERMINADA: 'No se encontró un cobro del día ni una exención comprobada.',
  SIN_INGRESO_ACTIVO: 'La placa no tiene un ingreso activo en esta sede.',
  ERROR_CONSULTA: 'No se pudo consultar la información de salida.',
  OCUPACION_INCONSISTENTE: 'La ocupación de la sede impide registrar la salida.',
  ERROR_REGISTRO_SALIDA: 'No se pudo registrar la salida.',
  ERROR_AUDITORIA: 'No se pudo registrar el intento de salida.',
} as const

type ExitCode = keyof typeof exitReasons

export function exitReason(code: string) {
  return Object.hasOwn(exitReasons, code) ? exitReasons[code as ExitCode] : undefined
}

export async function validateExit(plate: string) {
  const result = await postJson<{ barrierOpen: boolean; code: ExitCode }>('/api/v1/exits/validate', { plate })
  const reason = result && exitReason(result.code)
  const authorized = result?.code === 'PAGO_CONFIRMADO' || result?.code === 'EXENCION_COMPROBADA'
  if (!reason || result.barrierOpen !== authorized) {
    throw new Error('Respuesta de salida inválida.')
  }
  return { ...result, reason }
}

export type Occupancy = {
  vehicleType: 'CARRO' | 'MOTO'
  capacity: number
  occupied: number
}

export function findSiteOccupancy(signal: AbortSignal) {
  return request<Occupancy[]>('/api/v1/occupancy', { signal, cache: 'no-store' })
}
