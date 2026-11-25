import { useEffect, useState, type FormEvent } from 'react'
import { listSites, loadConfiguration, saveConfiguration, type Configuration, type Site } from './configurationApi'

export const adminInput = 'mt-1 block w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-2 text-slate-100'
export const adminButton = 'rounded-lg bg-teal-300 px-4 py-2 font-semibold text-slate-950 disabled:opacity-50'

export function ParkingConfigurationPage() {
  const [sites, setSites] = useState<Site[]>([])
  const [site, setSite] = useState(0)
  const [data, setData] = useState<Configuration | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [saving, setSaving] = useState(false)
  useEffect(() => {
    const controller = new AbortController()
    listSites(controller.signal).then(items => {
      if (controller.signal.aborted) return
      setSites(items)
      setSite(items[0]?.id ?? 0)
      if (!items.length) setNotice('No hay sedes activas.')
    }).catch(() => { if (!controller.signal.aborted) setError('No se pudieron consultar las sedes.') })
    return () => controller.abort()
  }, [])
  useEffect(() => {
    if (!site) return
    const controller = new AbortController()
    loadConfiguration(site, controller.signal).then(result => {
      if (!controller.signal.aborted) setData(result)
    }).catch(() => { if (!controller.signal.aborted) setError('No se pudo cargar la configuración.') })
    return () => controller.abort()
  }, [site])

  async function save(event: FormEvent) {
    event.preventDefault()
    if (!data || saving) return
    setSaving(true)
    setNotice('')
    setError('')
    try {
      await saveConfiguration(site, data)
      setNotice('Configuración guardada.')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo guardar.')
    } finally { setSaving(false) }
  }

  return <section className="max-w-2xl">
    <h1 className="text-3xl font-bold">Tarifas, franjas y cupos</h1>
    {!!sites.length && <label className="mt-5 block">Sede
      <select className={adminInput} disabled={saving} value={site} onChange={e => {
        setData(null); setNotice(''); setError(''); setSite(Number(e.target.value))
      }}>{sites.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select>
    </label>}
    {!data && !error && !notice && <p role="status">Cargando configuración…</p>}
    {data && <form onSubmit={save} onChange={() => { setNotice(''); setError('') }}>
      <fieldset disabled={saving} className="mt-5 space-y-5">
        <legend className="text-xl font-semibold">Franjas</legend>
        <p>Cada franja termina al comenzar la siguiente.</p>
        <div className="grid grid-cols-2 gap-4">
          {(['dayStart', 'nightStart'] as const).map(key => <label key={key}>Inicio {key === 'dayStart' ? 'diurno' : 'nocturno'}
            <input required type="time" step="1" className={adminInput} value={data[key] ?? ''}
              onChange={e => setData({ ...data, [key]: e.target.value })} />
          </label>)}
        </div>
        {(['car', 'motorcycle'] as const).map(type => <fieldset key={type} className="grid gap-3 sm:grid-cols-3">
          <legend className="mb-2 text-xl font-semibold">{type === 'car' ? 'Carros' : 'Motos'}</legend>
          {(['day', 'night', 'capacity'] as const).map(key => <label key={key}>
            {{ day: 'Tarifa diurna ($)', night: 'Tarifa nocturna ($)', capacity: 'Capacidad' }[key]}
            <input required type="number" min="0" step={key === 'capacity' ? '1' : '0.01'} className={adminInput}
              value={data[type][key] ?? ''} onChange={e => setData({ ...data, [type]: {
                ...data[type], [key]: e.target.value === '' ? null : e.target.valueAsNumber,
              } })} />
          </label>)}
        </fieldset>)}
        <button className={adminButton}>{saving ? 'Guardando…' : 'Guardar configuración'}</button>
      </fieldset>
    </form>}
    {notice && <p className="mt-4 text-teal-300" role="status">{notice}</p>}
    {error && <p className="mt-4 text-rose-300" role="alert">{error}</p>}
  </section>
}
