// @vitest-environment jsdom
import { act } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AppRouter } from '../app/router/AppRouter'

const operator = { username: 'operador', role: 'OPERADOR' }
const admin = { username: 'administrador', role: 'ADMIN' }

const eventsPage = {
  items: [{
    id: 1, siteId: 1, plate: 'ABC123', vehicleType: 'CARRO', eventType: 'INGRESO', source: 'AUTOMATICA',
    result: 'AUTORIZADO', reason: null, occurredAt: '2026-11-23T08:00:00-05:00',
  }],
  page: 0, size: 10, totalItems: 1, totalPages: 1,
}

const chargesPage = {
  items: [{
    id: 7, siteId: 1, plate: 'XYZ789', vehicleType: 'CARRO', operatingDay: '2026-11-23', timeSlot: 'DIURNA',
    amount: 10000, status: 'PAGADO', generatedAt: '2026-11-23T08:00:00-05:00', validUntil: '2026-11-24T06:00:00-05:00',
  }],
  page: 0, size: 10, totalItems: 25, totalPages: 3,
}

const emptyPage = { items: [], page: 0, size: 10, totalItems: 0, totalPages: 0 }

function response(status: number, body: unknown = {}) {
  return { ok: status >= 200 && status < 300, status, json: async () => body }
}

let container: HTMLDivElement
let root: Root
let fetchMock: ReturnType<typeof vi.fn>

beforeEach(() => {
  vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true)
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
  container = document.createElement('div')
  document.body.append(container)
  root = createRoot(container)
})

afterEach(async () => {
  await act(async () => root.unmount())
  container.remove()
  vi.unstubAllGlobals()
})

async function render(path: string, session = operator) {
  fetchMock.mockResolvedValueOnce(response(200, session))
  await act(async () => root.render(
    <MemoryRouter initialEntries={[path]}>
      <AppRouter />
    </MemoryRouter>,
  ))
}

async function setField(selector: string, value: string) {
  const element = container.querySelector<HTMLInputElement | HTMLSelectElement>(selector)!
  const prototype = element instanceof HTMLSelectElement ? HTMLSelectElement.prototype : HTMLInputElement.prototype
  const setValue = Object.getOwnPropertyDescriptor(prototype, 'value')!.set!
  await act(async () => {
    setValue.call(element, value)
    element.dispatchEvent(new Event(element instanceof HTMLSelectElement ? 'change' : 'input', { bubbles: true }))
  })
}

async function submit() {
  await act(async () => container.querySelector('form')!.dispatchEvent(
    new Event('submit', { bubbles: true, cancelable: true }),
  ))
}

async function click(element: Element) {
  await act(async () => element.dispatchEvent(new MouseEvent('click', { bubbles: true })))
}

function lastUrl() {
  return String(fetchMock.mock.calls.at(-1)![0])
}

describe('historial de eventos y cobros (HU-20)', () => {
  it('lista los eventos filtrados por placa (CA-01, CA-03)', async () => {
    await render('/operator/history')
    fetchMock.mockResolvedValueOnce(response(200, eventsPage))
    await setField('#history-plate', 'abc123')
    await submit()

    expect(lastUrl()).toContain('/api/v1/history/events?plate=abc123')
    expect(lastUrl()).toContain('page=0')
    const table = container.querySelector('table')?.textContent ?? ''
    expect(table).toContain('ABC123')
    expect(table).toContain('INGRESO')
  })

  it('combina fecha y estado en los cobros y muestra importe y estado (CA-02, CA-04, CA-05)', async () => {
    await render('/operator/history')
    await click(container.querySelectorAll('[role="tab"]')[1])
    await setField('#history-from', '2026-11-23')
    await setField('#history-to', '2026-11-23')
    await setField('#history-state', 'PAGADO')
    fetchMock.mockResolvedValueOnce(response(200, chargesPage))
    await submit()

    expect(lastUrl()).toContain('/api/v1/history/charges?')
    expect(lastUrl()).toContain('from=2026-11-23')
    expect(lastUrl()).toContain('to=2026-11-23')
    expect(lastUrl()).toContain('status=PAGADO')
    const table = container.querySelector('table')?.textContent ?? ''
    expect(table).toContain('XYZ789')
    expect(table).toContain('PAGADO')
    expect(table).toMatch(/10\.000/)
  })

  it('pagina hacia la siguiente página con los mismos filtros', async () => {
    await render('/operator/history')
    await click(container.querySelectorAll('[role="tab"]')[1])
    fetchMock.mockResolvedValueOnce(response(200, chargesPage))
    await submit()
    expect(container.querySelector('nav[aria-label="Paginación"]')?.textContent).toContain('Página 1 de 3')

    fetchMock.mockResolvedValueOnce(response(200, { ...chargesPage, page: 1 }))
    const next = [...container.querySelectorAll('nav button')].find((button) => button.textContent === 'Siguiente')!
    await click(next)

    expect(lastUrl()).toContain('page=1')
    expect(container.querySelector('nav[aria-label="Paginación"]')?.textContent).toContain('Página 2 de 3')
  })

  it('muestra un estado vacío sin errores (CA-06)', async () => {
    await render('/operator/history')
    fetchMock.mockResolvedValueOnce(response(200, emptyPage))
    await submit()

    expect(container.querySelector('[data-testid="history-empty"]')).toBeTruthy()
    expect(container.querySelector('[role="alert"]')).toBeNull()
  })

  it('muestra el mensaje del backend ante un filtro inválido (CP-HU-20-X01)', async () => {
    await render('/operator/history')
    fetchMock.mockResolvedValueOnce(response(400, {
      code: 'FILTRO_INVALIDO', message: 'La fecha inicial no puede ser posterior a la fecha final.',
    }))
    await submit()

    expect(container.querySelector('[role="alert"]')?.textContent).toContain('posterior a la fecha final')
  })

  it('el administrador también tiene la consulta en su panel', async () => {
    await render('/admin', admin)
    expect(container.querySelector('a[href="/admin/history"]')).toBeTruthy()
  })
})
