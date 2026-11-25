import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ROUTES } from '../../app/router/routes'
import { classifyVehicle, type VehicleClassification } from './classificationApi'

export function ClassifyVehiclePage() {
  const [plate, setPlate] = useState('')
  const [result, setResult] = useState<VehicleClassification | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setResult(null)
    setError('')
    setLoading(true)
    try {
      setResult(await classifyVehicle(plate))
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
        <h1 className="mt-6 text-3xl font-bold tracking-tight sm:text-4xl">Clasificar vehículo</h1>
        <p className="mt-3 text-slate-300">Ingresa una placa colombiana para consultar su tipo.</p>
        <form onSubmit={handleSubmit} className="mt-8 space-y-5">
          <div>
            <label htmlFor="plate" className="block text-sm font-semibold">Placa</label>
            <input
              id="plate"
              name="plate"
              value={plate}
              onChange={(event) => { setPlate(event.target.value.toUpperCase()); setResult(null) }}
              autoComplete="off"
              placeholder="ABC123 o ABC12D"
              className="mt-2 w-full rounded-lg border border-slate-500 bg-slate-950 px-4 py-3 text-slate-100 placeholder:text-slate-400 focus:border-teal-300 focus:outline-none"
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-teal-300 px-5 py-3 font-semibold text-slate-950 hover:bg-teal-200 disabled:cursor-wait disabled:opacity-60"
          >
            {loading ? 'Clasificando…' : 'Clasificar vehículo'}
          </button>
        </form>
        {result && (
          <div role="status" className="mt-6 rounded-lg border border-teal-700 bg-teal-950 p-4">
            <p>Placa: <strong>{result.plate}</strong></p>
            <p>Tipo: <strong>{result.type}</strong></p>
            <Link to={ROUTES.entry} state={{ plate: result.plate }} className="text-teal-300 underline">Registrar ingreso de {result.plate}</Link>
          </div>
        )}
        {error && <p role="alert" className="mt-6 rounded-lg border border-red-700 bg-red-950 p-4 text-red-100">{error}</p>}
      </section>
    </div>
  )
}
