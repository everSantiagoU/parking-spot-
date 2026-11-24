import { postJson, request } from '../../services/httpClient'

export type PaymentView = {
  paymentId: number
  chargeId: number
  method: 'EFECTIVO'
  amount: number
  operator: string
  registeredAt: string
}

export type PaymentResult = {
  payment: PaymentView
  plate: string
  chargeStatus: 'PENDIENTE' | 'PAGADO' | 'VENCIDO' | 'ANULADO'
  passValidUntil: string
  replayed: boolean
}

/**
 * Registra el pago en efectivo de un cobro (HU-16).
 * La clave de idempotencia se reutiliza en los reintentos para no registrar dos pagos.
 */
export function payInCash(chargeId: number, amount: number, idempotencyKey: string): Promise<PaymentResult> {
  return postJson<PaymentResult>('/api/v1/payments', { chargeId, amount, idempotencyKey })
}

/** Consulta el pago registrado de un cobro (CA-03). */
export function findPayment(chargeId: number): Promise<PaymentView> {
  return request<PaymentView>(`/api/v1/payments/charge/${chargeId}`)
}
