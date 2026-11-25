import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { panelForRole } from '../../app/router/routes'
import { useAuth } from '../auth/AuthProvider'
import {
  findCharges,
  findEvents,
  type ChargeRow,
  type EventRow,
  type HistoryFilters,
  type HistoryKind,
  type Page,
} from './historyApi'

const dateTime = new Intl.DateTimeFormat('es-CO', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Bogota' })
const money = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

const STATE_OPTIONS: Record<HistoryKind, string[]> = {
  events: ['INGRESO', 'VALIDACION', 'SALIDA'],
  charges: ['PENDIENTE', 'PAGADO', 'VENCIDO', 'ANULADO'],
}

const EMPTY_FILTERS: HistoryFilters = { plate: '', from: '', to: '', state: '', page: 0 }

type Result = { kind: 'events'; page: Page<EventRow> } | { kind: 'charges'; page: Page<ChargeRow> }

const input = 'rounded-lg border border-slate-500 bg-slate-950 px-3 py-2 text-slate-100 placeholder:text-slate-400 focus:border-teal-300 focus:outline-none'
const cell = 'border-b border-slate-800 px-3 py-2'

/** HU-20: consulta de eventos y cobros con filtros, paginación y estados de carga, error y vacío. */
export function HistoryPage() {
  const { user } = useAuth()
  const [kind, setKind] = useState<HistoryKind>('events')
  const [filters, setFilters] = useState<HistoryFilters>(EMPTY_FILTERS)
  const [result, setResult] = useState<Result | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function load(target: HistoryKind, next: HistoryFilters) {
    setLoading(true)
    setError('')
    try {
      setResult(target === 'events'
        ? { kind: 'events', page: await findEvents(next) }
        : { kind: 'charges', page: await findCharges(next) })
      setFilters(next)
    } catch (cause) {
      setResult(null)
      setError(cause instanceof Error ? cause.message : 'No se pudo conectar con el servidor.')
    } finally {
      setLoading(false)
    }
  }

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void load(kind, { ...filters, page: 0 })
  }

  function changeKind(next: HistoryKind) {
    setKind(next)
    setResult(null)
    setError('')
    setFilters({ ...filters, state: '', page: 0 })
  }

  function update(field: keyof HistoryFilters, value: string) {
    setFilters({ ...filters, [field]: value })
  }

  const page = result?.page

  return (
    <section className="space-y-6">
      {user && <Link to={panelForRole(user.role)} className="text-sm font-medium text-teal-300 hover:text-teal-200">← Panel</Link>}
      <h1 className="text-3xl font-bold tracking-tight">Eventos y cobros</h1>

      <div role="tablist" className="flex gap-2">
        {(['events', 'charges'] as const).map((tab) => (
          <button
            key={tab}
            type="button"
            role="tab"
            aria-selected={kind === tab}
            onClick={() => changeKind(tab)}
            className={`rounded-lg px-4 py-2 font-semibold ${kind === tab ? 'bg-teal-300 text-slate-950' : 'border border-slate-600 text-slate-200'}`}
          >
            {tab === 'events' ? 'Eventos' : 'Cobros'}
          </button>
        ))}
      </div>

      <form onSubmit={search} className="grid gap-3 sm:grid-cols-5">
        <label className="flex flex-col gap-1 text-sm">Placa
          <input id="history-plate" value={filters.plate} onChange={(e) => update('plate', e.target.value)} placeholder="ABC123" className={input} />
        </label>
        <label className="flex flex-col gap-1 text-sm">Desde
          <input id="history-from" type="date" value={filters.from} onChange={(e) => update('from', e.target.value)} className={input} />
        </label>
        <label className="flex flex-col gap-1 text-sm">Hasta
          <input id="history-to" type="date" value={filters.to} onChange={(e) => update('to', e.target.value)} className={input} />
        </label>
        <label className="flex flex-col gap-1 text-sm">{kind === 'events' ? 'Tipo' : 'Estado'}
          <select id="history-state" value={filters.state} onChange={(e) => update('state', e.target.value)} className={input}>
            <option value="">Todos</option>
            {STATE_OPTIONS[kind].map((option) => <option key={option} value={option}>{option}</option>)}
          </select>
        </label>
        <button type="submit" disabled={loading} className="self-end rounded-lg bg-teal-300 px-4 py-2 font-semibold text-slate-950 disabled:opacity-60">
          {loading ? 'Consultando…' : 'Consultar'}
        </button>
      </form>

      {loading && <p role="status">Consultando…</p>}
      {error && <p role="alert" className="rounded-lg border border-red-700 bg-red-950 p-4 text-red-100">{error}</p>}

      {page && page.items.length === 0 && (
        <p data-testid="history-empty" className="rounded-lg border border-slate-700 p-4 text-slate-300">
          No hay registros que coincidan con los filtros.
        </p>
      )}

      {result?.kind === 'events' && result.page.items.length > 0 && (
        <table className="w-full text-left text-sm">
          <thead><tr>{['Fecha', 'Placa', 'Vehículo', 'Tipo', 'Origen', 'Resultado', 'Motivo'].map((h) => <th key={h} className={cell}>{h}</th>)}</tr></thead>
          <tbody>
            {result.page.items.map((row) => (
              <tr key={row.id}>
                <td className={cell}>{dateTime.format(new Date(row.occurredAt))}</td>
                <td className={cell}>{row.plate}</td>
                <td className={cell}>{row.vehicleType}</td>
                <td className={cell}>{row.eventType}</td>
                <td className={cell}>{row.source}</td>
                <td className={cell}>{row.result}</td>
                <td className={cell}>{row.reason ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {result?.kind === 'charges' && result.page.items.length > 0 && (
        <table className="w-full text-left text-sm">
          <thead><tr>{['Día operativo', 'Placa', 'Vehículo', 'Franja', 'Importe', 'Estado'].map((h) => <th key={h} className={cell}>{h}</th>)}</tr></thead>
          <tbody>
            {result.page.items.map((row) => (
              <tr key={row.id}>
                <td className={cell}>{row.operatingDay}</td>
                <td className={cell}>{row.plate}</td>
                <td className={cell}>{row.vehicleType}</td>
                <td className={cell}>{row.timeSlot}</td>
                <td className={cell}>{money.format(row.amount)}</td>
                <td className={cell}>{row.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {page && page.totalPages > 1 && (
        <nav aria-label="Paginación" className="flex items-center gap-3 text-sm">
          <button type="button" disabled={loading || page.page === 0} onClick={() => void load(kind, { ...filters, page: page.page - 1 })}
            className="rounded-lg border border-slate-600 px-3 py-1 disabled:opacity-40">Anterior</button>
          <span>Página {page.page + 1} de {page.totalPages} · {page.totalItems} registros</span>
          <button type="button" disabled={loading || page.page + 1 >= page.totalPages} onClick={() => void load(kind, { ...filters, page: page.page + 1 })}
            className="rounded-lg border border-slate-600 px-3 py-1 disabled:opacity-40">Siguiente</button>
        </nav>
      )}
    </section>
  )
}
