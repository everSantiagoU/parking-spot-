import { useEffect, useState, type FormEvent } from 'react'
import { request } from '../../services/httpClient'
import { adminButton, adminInput } from '../tariffs/ParkingConfigurationPage'

type Filters = { kind: 'EVENTOS' | 'COBROS'; plate: string; from: string; to: string; status: string }
type Row = { id: number; plate: string; site: string; kind: string; date: string; status: string; amount: number | null; reason: string | null }
const initial: Filters = { kind: 'EVENTOS', plate: '', from: '', to: '', status: '' }
const labels: Record<string, string> = { VALIDACION: 'Intento de salida', SALIDA: 'Salida efectiva', INGRESO: 'Ingreso', COBRO: 'Cobro' }

export function HistoryPage() {
  const [draft, setDraft] = useState(initial)
  const [query, setQuery] = useState({ filters: initial, page: 0 })
  const [rows, setRows] = useState<Row[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    const params = new URLSearchParams({ page: String(query.page) })
    for (const [key, value] of Object.entries(query.filters)) if (value) params.set(key, value)
    request<Row[]>(`/api/v1/admin/history?${params}`, { signal: controller.signal })
      .then(result => { if (!controller.signal.aborted) setRows(result) })
      .catch(() => { if (!controller.signal.aborted) setError('No se pudo consultar el historial.') })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [query])

  function search(event: FormEvent) {
    event.preventDefault()
    setQuery({ filters: draft, page: 0 })
  }
  return <section>
    <h1 className="text-3xl font-bold">Eventos y cobros</h1>
    <form onSubmit={search} className="my-5 flex flex-wrap items-end gap-3">
      <label>Listado<select className={adminInput} value={draft.kind}
        onChange={e => setDraft({ ...draft, kind: e.target.value as Filters['kind'], status: '' })}>
        <option value="EVENTOS">Eventos</option><option value="COBROS">Cobros</option>
      </select></label>
      <label>Placa<input className={adminInput} value={draft.plate} pattern="[A-Z]{3}[0-9]{2}[A-Z0-9]"
        onChange={e => setDraft({ ...draft, plate: e.target.value.toUpperCase() })} /></label>
      <label>Desde<input type="date" className={adminInput} value={draft.from} max={draft.to || undefined}
        onChange={e => setDraft({ ...draft, from: e.target.value })} /></label>
      <label>Hasta<input type="date" className={adminInput} value={draft.to} min={draft.from || undefined}
        onChange={e => setDraft({ ...draft, to: e.target.value })} /></label>
      {draft.kind === 'COBROS' && <label>Estado<select className={adminInput} value={draft.status}
        onChange={e => setDraft({ ...draft, status: e.target.value })}>
        <option value="">Todos</option>
        {['PENDIENTE', 'PAGADO', 'VENCIDO', 'ANULADO'].map(s => <option key={s}>{s}</option>)}
      </select></label>}
      <button className={adminButton}>Consultar</button>
    </form>
    {loading ? <p role="status">Consultando…</p> : error ? <p role="alert">{error}</p> : !rows.length
      ? <p role="status">Sin resultados.</p>
      : <div className="overflow-x-auto"><table className="w-full text-left text-sm">
        <thead><tr>{['Fecha', 'Sede', 'Placa', 'Tipo', 'Estado', query.filters.kind === 'COBROS' ? 'Importe' : 'Motivo'].map(c =>
          <th key={c} className="p-2">{c}</th>)}</tr></thead>
        <tbody>{rows.map(row => <tr key={row.id} className="border-t border-slate-700">
          <td className="p-2">{new Date(row.date).toLocaleString('es-CO', { timeZone: 'America/Bogota' })}</td>
          <td>{row.site}</td><td>{row.plate}</td><td>{labels[row.kind]}</td><td>{row.status}</td>
          <td>{row.amount === null ? row.reason ?? '—' : '$' + row.amount.toLocaleString('es-CO')}</td>
        </tr>)}</tbody></table></div>}
    <nav aria-label="Páginas del historial" className="mt-4 flex gap-4">
      <button disabled={loading || query.page === 0} onClick={() => setQuery({ ...query, page: query.page - 1 })}>Anterior</button>
      <span>Página {query.page + 1}</span>
      <button disabled={loading || !!error || rows.length < 25} onClick={() => setQuery({ ...query, page: query.page + 1 })}>Siguiente</button>
    </nav>
  </section>
}
