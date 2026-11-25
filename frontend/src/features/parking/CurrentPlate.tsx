import { useEffect, useState } from 'react'
import { request } from '../../services/httpClient'

export function CurrentPlate() {
  const [plate, setPlate] = useState<string | null | undefined>(undefined)
  const [failed, setFailed] = useState(false)
  useEffect(() => {
    const controller = new AbortController()
    request<{ plate: string | null }>('/api/v1/entries/latest', { signal: controller.signal, cache: 'no-store' })
      .then(data => { if (!controller.signal.aborted) setPlate(data.plate) })
      .catch(() => { if (!controller.signal.aborted) setFailed(true) })
    return () => controller.abort()
  }, [])
  return <section className="mt-6">
    <h2 className="text-lg font-semibold">Placa actual</h2>
    <div aria-live="polite" className="mt-2">
      {failed ? 'No se pudo consultar la última placa.' : plate === undefined ? 'Consultando placa…'
        : plate === null ? 'Aún no hay una placa registrada'
        : <strong className="inline-block rounded-lg border-4 border-black bg-yellow-400 px-4 py-1 font-mono text-3xl tracking-widest text-black">{plate}</strong>}
    </div>
  </section>
}
