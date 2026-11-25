import { useEffect, useState } from 'react'
import { findSiteOccupancy, type Occupancy } from './exitsApi'

type SiteOccupancyProps = { revision: number; onRefresh: () => void }

export function SiteOccupancy({ revision, onRefresh }: SiteOccupancyProps) {
  const [occupancy, setOccupancy] = useState<Occupancy[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setFailed(false)
    void findSiteOccupancy(controller.signal)
      .then((snapshot) => {
        if (!controller.signal.aborted) setOccupancy(snapshot)
      })
      .catch(() => {
        if (!controller.signal.aborted) setFailed(true)
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [revision])

  return (
    <section aria-labelledby="site-occupancy" className="mt-8 border-t border-slate-700 pt-5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 id="site-occupancy" className="text-lg font-semibold">Disponibilidad de la sede</h2>
        <button
          type="button"
          onClick={onRefresh}
          disabled={loading}
          className="rounded-lg border border-slate-600 px-3 py-2 text-sm font-medium text-teal-200 disabled:opacity-50"
        >
          Actualizar cupos
        </button>
      </div>
      <div aria-live="polite" className="mt-3">
        {loading && <p className="text-sm text-slate-300">Consultando disponibilidad…</p>}
        {failed && <p role="alert" className="text-sm text-rose-300">
          No se pudo actualizar la disponibilidad.{occupancy?.length ? ' Se muestran los últimos datos consultados.' : ''}
        </p>}
        {occupancy?.length === 0 && <p className="text-sm text-slate-300">La sede no tiene cupos configurados.</p>}
        {!!occupancy?.length && (
          <table aria-labelledby="site-occupancy" className="mt-2 w-full text-left text-sm">
            <thead className="text-slate-300">
              <tr>
                <th scope="col" className="py-2">Tipo</th>
                <th scope="col">Capacidad</th>
                <th scope="col">Ocupados</th>
                <th scope="col">Disponibles</th>
              </tr>
            </thead>
            <tbody>
              {occupancy.map(({ vehicleType, capacity, occupied }) => (
                <tr key={vehicleType} className="border-t border-slate-700">
                  <th scope="row" className="py-2 font-normal">{vehicleType === 'CARRO' ? 'Carros' : 'Motos'}</th>
                  <td>{capacity}</td>
                  <td>{occupied}</td>
                  <td className="font-semibold">{capacity - occupied}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  )
}
