import { useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ROUTES } from '../../app/router/routes'
import { ApiError } from '../../services/httpClient'
import { SiteOccupancy } from './SiteOccupancy'
import { exitReason, validateExit } from './exitsApi'

export function ExitLanePage() {
  const [plate, setPlate] = useState('')
  const [pending, setPending] = useState(false)
  const [result, setResult] = useState<Awaited<ReturnType<typeof validateExit>> | null>(null)
  const [error, setError] = useState('')
  const [occupancyRevision, setOccupancyRevision] = useState(0)
  const submitting = useRef(false)

  function refreshOccupancy() {
    setOccupancyRevision((revision) => revision + 1)
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting.current || !plate.trim()) return

    submitting.current = true
    setPending(true)
    setResult(null)
    setError('')
    try {
      const decision = await validateExit(plate.trim())
      setResult(decision)
      if (decision.barrierOpen) refreshOccupancy()
    } catch (failure) {
      setError(failure instanceof ApiError
        ? (exitReason(failure.code ?? '') ?? failure.message)
        : 'No se pudo validar la salida. Comprueba la conexión e inténtalo de nuevo.')
    } finally {
      submitting.current = false
      setPending(false)
    }
  }

  return (
    <section className="max-w-xl">
      <Link to={ROUTES.operator} className="text-teal-300 hover:underline">Volver al panel</Link>
      <h1 className="mt-4 text-3xl font-bold tracking-tight">Carril de salida</h1>
      <form className="mt-6 flex flex-wrap items-end gap-3" onSubmit={submit}>
        <label className="flex flex-col gap-2 font-medium" htmlFor="exit-plate">
          Placa
          <input
            id="exit-plate"
            name="plate"
            autoComplete="off"
            required
            disabled={pending}
            value={plate}
            onChange={(event) => {
              setPlate(event.target.value.toUpperCase())
              setResult(null)
              setError('')
            }}
            className="w-40 rounded-lg border border-slate-600 bg-slate-800 px-3 py-2 uppercase"
          />
        </label>
        <button
          type="submit"
          disabled={pending || !plate.trim()}
          className="rounded-lg bg-teal-300 px-4 py-2 font-semibold text-slate-950 disabled:opacity-50"
        >
          {pending ? 'Validando…' : 'Solicitar salida'}
        </button>
      </form>
      <div role="status" className="mt-6 border-t border-slate-700 pt-4">
        <p className={result?.barrierOpen ? 'font-semibold text-teal-300' : 'font-semibold text-slate-100'}>
          {result?.barrierOpen ? 'Barrera simulada abierta' : 'Barrera simulada cerrada'}
        </p>
        {pending && <p>Consultando ingreso y cobro del día operativo…</p>}
        {result && <p>{result.reason}</p>}
      </div>
      {result?.code === 'SIN_INGRESO_ACTIVO' && (
        <p className="mt-3 text-slate-300">
          Si el vehículo está entrando, registra su ingreso.{' '}
          <Link to={ROUTES.entry} className="text-teal-300 underline">Ir al carril de entrada</Link>
        </p>
      )}
      {error && <p role="alert" className="mt-2 text-rose-300">{error}</p>}
      <SiteOccupancy revision={occupancyRevision} onRefresh={refreshOccupancy} />
    </section>
  )
}
