import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ROUTES } from '../../app/router/routes'
import { findTodayCharge, type ChargeResult } from '../charges/chargesApi'
import { findPayment, payInCash, type PaymentView } from './paymentsApi'

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
const dateTime = new Intl.DateTimeFormat('es-CO', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Bogota' })

type Receipt = { payment: PaymentView; passValidUntil?: string; replayed?: boolean }

/** HU-16: caja del operador para registrar el pago en efectivo del cobro del día. */
export function CashierPage() {
  const [plate, setPlate] = useState('')
  const [charge, setCharge] = useState<ChargeResult | null>(null)
  const [receipt, setReceipt] = useState<Receipt | null>(null)
  const [paymentKey, setPaymentKey] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setCharge(null)
    setReceipt(null)
    setError('')
    setLoading(true)
    try {
      const found = await findTodayCharge(plate)
      setCharge(found)
      // Una clave por cobro: si el operador reintenta, el backend devuelve el mismo pago.
      setPaymentKey(crypto.randomUUID())
      if (found.status === 'PAGADO' && found.chargeId !== null) {
        setReceipt({ payment: await findPayment(found.chargeId) })
      }
    } catch (cause) {
      setError(messageOf(cause))
    } finally {
      setLoading(false)
    }
  }

  async function pay() {
    if (!charge || charge.chargeId === null) return
    setError('')
    setLoading(true)
    try {
      const result = await payInCash(charge.chargeId, charge.amount, paymentKey)
      setReceipt({ payment: result.payment, passValidUntil: result.passValidUntil, replayed: result.replayed })
      setCharge({ ...charge, status: result.chargeStatus })
    } catch (cause) {
      setError(messageOf(cause))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex justify-center">
      <section className="w-full max-w-xl rounded-2xl border border-slate-700 bg-slate-900 p-8 shadow-xl sm:p-12">
        <Link to={ROUTES.operator} className="text-sm font-medium text-teal-300 hover:text-teal-200">← Operador</Link>
        <h1 className="mt-6 text-3xl font-bold tracking-tight sm:text-4xl">Caja</h1>
        <p className="mt-3 text-slate-300">Busca el cobro de hoy de una placa y registra el pago en efectivo.</p>

        <form onSubmit={search} className="mt-8 flex gap-3">
          <label htmlFor="cashier-plate" className="sr-only">Placa</label>
          <input
            id="cashier-plate"
            name="plate"
            value={plate}
            onChange={(event) => setPlate(event.target.value)}
            autoComplete="off"
            placeholder="ABC123"
            className="flex-1 rounded-lg border border-slate-500 bg-slate-950 px-4 py-3 text-slate-100 placeholder:text-slate-400 focus:border-teal-300 focus:outline-none"
          />
          <button type="submit" disabled={loading} className="rounded-lg border border-teal-300 px-5 py-3 font-semibold text-teal-200 hover:bg-slate-800 disabled:opacity-60">
            Buscar
          </button>
        </form>

        {charge && (
          <div aria-label="Cobro" className="mt-6 space-y-1 rounded-lg border border-slate-600 p-4">
            <p>Placa: <strong>{charge.plate}</strong> · {charge.vehicleType} · Día operativo {charge.operatingDay}</p>
            <p>Valor: <strong>{pesos.format(charge.amount)}</strong></p>
            <p>Estado: <strong>{charge.status}</strong></p>
            {charge.status === 'PENDIENTE' && !receipt && (
              <button
                type="button"
                onClick={() => void pay()}
                disabled={loading}
                className="mt-3 w-full rounded-lg bg-teal-300 px-5 py-3 font-semibold text-slate-950 hover:bg-teal-200 disabled:cursor-wait disabled:opacity-60"
              >
                {loading ? 'Registrando…' : `Registrar pago en efectivo de ${pesos.format(charge.amount)}`}
              </button>
            )}
          </div>
        )}

        {receipt && (
          <div role="status" className="mt-6 space-y-1 rounded-lg border border-teal-700 bg-teal-950 p-4">
            <p className="font-semibold">{receipt.replayed ? 'Este pago ya estaba registrado.' : 'Pago registrado.'}</p>
            <p>Importe: <strong>{pesos.format(receipt.payment.amount)}</strong> · {receipt.payment.method}</p>
            <p>Fecha: {dateTime.format(new Date(receipt.payment.registeredAt))}</p>
            <p>Operador: {receipt.payment.operator}</p>
            {receipt.passValidUntil && (
              <p>Pase vigente hasta: <strong>{dateTime.format(new Date(receipt.passValidUntil))}</strong></p>
            )}
          </div>
        )}

        {error && <p role="alert" className="mt-6 rounded-lg border border-red-700 bg-red-950 p-4 text-red-100">{error}</p>}
      </section>
    </div>
  )
}

function messageOf(cause: unknown) {
  return cause instanceof Error ? cause.message : 'No se pudo conectar con el servidor.'
}
