import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ROUTES } from '../../app/router/routes'
import { findPass, type PassStatus, type PassView } from './passesApi'

const dateTime = new Intl.DateTimeFormat('es-CO', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Bogota' })

const STATUS_TEXT: Record<PassStatus, string> = {
  VIGENTE: 'Vigente',
  VENCIDO: 'Vencido',
  OTRA_SEDE: 'De otra sede',
}

const STATUS_STYLE: Record<PassStatus, string> = {
  VIGENTE: 'border-teal-700 bg-teal-950',
  VENCIDO: 'border-amber-700 bg-amber-950',
  OTRA_SEDE: 'border-amber-700 bg-amber-950',
}

/** HU-17 CA-06: el operador consulta el pase de una placa, su estado y su vigencia. */
export function PassPage() {
  const [plate, setPlate] = useState('')
  const [pass, setPass] = useState<PassView | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPass(null)
    setError('')
    setLoading(true)
    try {
      setPass(await findPass(plate))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo conectar con el servidor.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex justify-center">
      <section className="w-full max-w-xl rounded-2xl border border-slate-700 bg-slate-900 p-8 shadow-xl sm:p-12">
        <Link to={ROUTES.operator} className="text-sm font-medium text-teal-300 hover:text-teal-200">← Operador</Link>
        <h1 className="mt-6 text-3xl font-bold tracking-tight sm:text-4xl">Consultar pase</h1>
        <p className="mt-3 text-slate-300">Revisa si una placa tiene pase vigente para reingresar sin cobro.</p>

        <form onSubmit={search} className="mt-8 flex gap-3">
          <label htmlFor="pass-plate" className="sr-only">Placa</label>
          <input
            id="pass-plate"
            name="plate"
            value={plate}
            onChange={(event) => setPlate(event.target.value.toUpperCase())}
            autoComplete="off"
            placeholder="ABC123"
            className="flex-1 rounded-lg border border-slate-500 bg-slate-950 px-4 py-3 text-slate-100 placeholder:text-slate-400 focus:border-teal-300 focus:outline-none"
          />
          <button type="submit" disabled={loading} className="rounded-lg border border-teal-300 px-5 py-3 font-semibold text-teal-200 hover:bg-slate-800 disabled:opacity-60">
            Consultar
          </button>
        </form>

        {pass && (
          <div role="status" className={`mt-6 space-y-1 rounded-lg border p-4 ${STATUS_STYLE[pass.status]}`}>
            <p>Placa: <strong>{pass.plate}</strong> · Día operativo {pass.operatingDay}</p>
            <p>Estado: <strong>{STATUS_TEXT[pass.status]}</strong></p>
            <p>Vigente hasta: <strong>{dateTime.format(new Date(pass.validUntil))}</strong></p>
            <p>{pass.freeReentry ? 'Puede reingresar sin un cobro adicional.' : 'El reingreso genera un cobro nuevo.'}</p>
          </div>
        )}

        {error && <p role="alert" className="mt-6 rounded-lg border border-red-700 bg-red-950 p-4 text-red-100">{error}</p>}
      </section>
    </div>
  )
}
