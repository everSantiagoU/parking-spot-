import { useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ROUTES } from '../../app/router/routes'
import { ApiError } from '../../services/httpClient'
import { registerManualEntry } from './entriesApi'

export function EntryLanePage() {
  const [plate, setPlate] = useState('')
  const [registeredPlate, setRegisteredPlate] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  const submitting = useRef(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting.current || registeredPlate || !plate.trim()) return

    submitting.current = true
    setPending(true)
    setError('')
    try {
      setRegisteredPlate(await registerManualEntry(plate.trim()))
    } catch (failure) {
      setError(failure instanceof ApiError && failure.code === 'LLENO'
        ? 'No hay cupo disponible para este tipo de vehículo.'
        : failure instanceof Error ? failure.message : 'No se pudo registrar el ingreso.')
    } finally {
      submitting.current = false
      setPending(false)
    }
  }

  return (
    <section className="max-w-xl">
      <Link to={ROUTES.operator} className="text-teal-300 hover:underline">Volver al panel</Link>
      <h1 className="mt-4 text-3xl font-bold tracking-tight">Carril de entrada</h1>
      <p className="mt-2 text-slate-300">Digita la placa del vehículo que está entrando a la sede.</p>
      <form className="mt-6 flex flex-wrap items-end gap-3" onSubmit={submit}>
        <label className="flex flex-col gap-2 font-medium" htmlFor="entry-plate">
          Placa
          <input
            id="entry-plate"
            name="plate"
            autoComplete="off"
            required
            disabled={pending}
            value={plate}
            onChange={(event) => {
              setPlate(event.target.value)
              setRegisteredPlate('')
              setError('')
            }}
            className="w-40 rounded-lg border border-slate-600 bg-slate-800 px-3 py-2 uppercase"
          />
        </label>
        <button
          type="submit"
          disabled={pending || !!registeredPlate || !plate.trim()}
          className="rounded-lg bg-teal-300 px-4 py-2 font-semibold text-slate-950 disabled:opacity-50"
        >
          {pending ? 'Registrando…' : 'Registrar ingreso'}
        </button>
      </form>
      {pending && <p role="status" className="mt-4">Validando cupo y registrando el ingreso…</p>}
      {registeredPlate && (
        <div className="mt-6 border-t border-slate-700 pt-4">
          <p role="status"><strong>{registeredPlate}</strong> · <span>Ingreso autorizado.</span></p>
          <Link to={ROUTES.charges} className="mt-2 inline-block text-teal-300 hover:underline">Ir a Cobro del día</Link>
        </div>
      )}
      {error && <p role="alert" className="mt-4 text-rose-300">{error}</p>}
    </section>
  )
}
