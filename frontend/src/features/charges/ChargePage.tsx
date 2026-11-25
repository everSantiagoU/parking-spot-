import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ROUTES } from '../../app/router/routes'
import { findTodayCharge, generateCharge, type ChargeResult } from './chargesApi'

const OUTCOME_TEXT: Record<ChargeResult['outcome'], string> = {
  CREADO: 'Cobro generado para el día operativo.',
  EXISTENTE: 'La placa ya tiene cobro en este día operativo. No se generó otro.',
  EXENTO: 'Vehículo sin obligación de pago: la tarifa es $0.',
}

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

type Action = 'generate' | 'consult'

export function ChargePage() {
  const [plate, setPlate] = useState('')
  const [result, setResult] = useState<ChargeResult | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState<Action | null>(null)

  async function run(action: Action) {
    setResult(null)
    setError('')
    setLoading(action)
    try {
      setResult(action === 'generate' ? await generateCharge(plate) : await findTodayCharge(plate))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo conectar con el servidor.')
    } finally {
      setLoading(null)
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void run('generate')
  }

  return (
    <div className="flex justify-center">
      <section className="w-full max-w-xl rounded-2xl border border-slate-700 bg-slate-900 p-8 shadow-xl sm:p-12">
        <Link to={ROUTES.operator} className="text-sm font-medium text-teal-300 hover:text-teal-200">← Operador</Link>
        <h1 className="mt-6 text-3xl font-bold tracking-tight sm:text-4xl">Cobro del día</h1>
        <p className="mt-3 text-slate-300">
          Para un vehículo que está entrando, registra primero su ingreso en el{' '}
          <Link to={ROUTES.entry} className="text-teal-300 underline">Carril de entrada</Link>.
        </p>
        <form onSubmit={handleSubmit} className="mt-8 space-y-5">
          <div>
            <label htmlFor="charge-plate" className="block text-sm font-semibold">Placa</label>
            <input
              id="charge-plate"
              name="plate"
              value={plate}
              onChange={(event) => setPlate(event.target.value)}
              autoComplete="off"
              placeholder="ABC123 o ABC12D"
              className="mt-2 w-full rounded-lg border border-slate-500 bg-slate-950 px-4 py-3 text-slate-100 placeholder:text-slate-400 focus:border-teal-300 focus:outline-none"
            />
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              type="submit"
              disabled={loading !== null}
              className="flex-1 rounded-lg bg-teal-300 px-5 py-3 font-semibold text-slate-950 hover:bg-teal-200 disabled:cursor-wait disabled:opacity-60"
            >
              {loading === 'generate' ? 'Calculando…' : 'Generar cobro'}
            </button>
            <button
              type="button"
              onClick={() => void run('consult')}
              disabled={loading !== null}
              className="flex-1 rounded-lg border border-teal-300 px-5 py-3 font-semibold text-teal-200 hover:bg-slate-800 disabled:cursor-wait disabled:opacity-60"
            >
              {loading === 'consult' ? 'Consultando…' : 'Consultar cobro de hoy'}
            </button>
          </div>
        </form>
        {result && <ChargeSummary result={result} />}
        {error && <p role="alert" className="mt-6 rounded-lg border border-red-700 bg-red-950 p-4 text-red-100">{error}</p>}
      </section>
    </div>
  )
}

function ChargeSummary({ result }: { result: ChargeResult }) {
  return (
    <div role="status" className="mt-6 space-y-1 rounded-lg border border-teal-700 bg-teal-950 p-4">
      <p className="font-semibold">{OUTCOME_TEXT[result.outcome]}</p>
      <p>Placa: <strong>{result.plate}</strong> · {result.vehicleType}</p>
      <p>Día operativo: <strong>{result.operatingDay}</strong> · Franja {result.timeSlot}</p>
      <p>Valor: <strong>{pesos.format(result.amount)}</strong></p>
      {result.status && <p>Estado: <strong>{result.status}</strong></p>}
      {result.appliedTariff !== null && <p>Tarifa aplicada: {pesos.format(result.appliedTariff)}</p>}
    </div>
  )
}
