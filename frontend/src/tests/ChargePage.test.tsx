// @vitest-environment jsdom
import { act } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AppRouter } from '../app/router/AppRouter'

const operator = { username: 'operador', role: 'OPERADOR' }

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

async function renderAsOperator(path: string) {
  fetchMock.mockResolvedValueOnce(response(200, operator))
  await act(async () => root.render(
    <MemoryRouter initialEntries={[path]}>
      <AppRouter />
    </MemoryRouter>,
  ))
}

async function typePlate(value: string) {
  const input = container.querySelector<HTMLInputElement>('#charge-plate')!
  const setValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!
  await act(async () => {
    setValue.call(input, value)
    input.dispatchEvent(new Event('input', { bubbles: true }))
  })
}

async function click(button: Element) {
  await act(async () => {
    button.dispatchEvent(new MouseEvent('click', { bubbles: true }))
  })
}

describe('pantalla de cobro del día', () => {
  it('el panel de operador enlaza a la pantalla de cobros', async () => {
    await renderAsOperator('/operator')
    expect(container.querySelector('a[href="/operator/charges"]')).toBeTruthy()
  })

  it('muestra el valor y el estado reales del cobro calculado por el backend (CA-07)', async () => {
    await renderAsOperator('/operator/charges')
    expect(container.querySelector('h1')?.textContent).toBe('Cobro del día')

    fetchMock
      .mockResolvedValueOnce(response(200, { headerName: 'X-CSRF-TOKEN', token: 't' }))
      .mockResolvedValueOnce(response(201, {
        plate: 'ABC123', vehicleType: 'CARRO', operatingDay: '2026-11-23', timeSlot: 'DIURNA',
        outcome: 'CREADO', chargeId: 1, appliedTariff: 10000, amount: 10000, status: 'PENDIENTE',
        generatedAt: '2026-11-23T10:00:00-05:00',
      }))
    await typePlate('abc123')
    await click(container.querySelector('button[type="submit"]')!)

    const summary = container.querySelector('[role="status"]')?.textContent ?? ''
    expect(summary).toContain('Cobro generado')
    expect(summary).toContain('ABC123')
    expect(summary).toContain('10.000')
    expect(summary).toContain('PENDIENTE')
  })

  it('informa cuando una moto queda exenta', async () => {
    await renderAsOperator('/operator/charges')
    fetchMock
      .mockResolvedValueOnce(response(200, { headerName: 'X-CSRF-TOKEN', token: 't' }))
      .mockResolvedValueOnce(response(200, {
        plate: 'ABC12D', vehicleType: 'MOTO', operatingDay: '2026-11-23', timeSlot: 'DIURNA',
        outcome: 'EXENTO', chargeId: null, appliedTariff: null, amount: 0, status: null, generatedAt: null,
      }))
    await typePlate('ABC12D')
    await click(container.querySelector('button[type="submit"]')!)

    expect(container.querySelector('[role="status"]')?.textContent).toContain('sin obligación de pago')
  })

  it('muestra el error controlado cuando no hay tarifa', async () => {
    await renderAsOperator('/operator/charges')
    fetchMock
      .mockResolvedValueOnce(response(200, { headerName: 'X-CSRF-TOKEN', token: 't' }))
      .mockResolvedValueOnce(response(422, { code: 'TARIFA_NO_CONFIGURADA', message: 'No hay tarifa DIURNA configurada para CARRO.' }))
    await typePlate('ABC123')
    await click(container.querySelector('button[type="submit"]')!)

    expect(container.querySelector('[role="alert"]')?.textContent).toContain('No hay tarifa DIURNA')
  })
})
