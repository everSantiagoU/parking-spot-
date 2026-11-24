// @vitest-environment jsdom
import { act } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AppRouter } from '../app/router/AppRouter'

const operator = { username: 'operador', role: 'OPERADOR' }
const pendingCharge = {
  plate: 'ABC123', vehicleType: 'CARRO', operatingDay: '2026-11-23', timeSlot: 'DIURNA', outcome: 'EXISTENTE',
  chargeId: 7, appliedTariff: 10000, amount: 10000, status: 'PENDIENTE', generatedAt: '2026-11-23T10:00:00-05:00',
}
const payment = {
  paymentId: 1, chargeId: 7, method: 'EFECTIVO', amount: 10000, operator: 'operador',
  registeredAt: '2026-11-23T15:00:00-05:00',
}

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

async function renderCashier() {
  fetchMock.mockResolvedValueOnce(response(200, operator))
  await act(async () => root.render(
    <MemoryRouter initialEntries={['/operator/cashier']}>
      <AppRouter />
    </MemoryRouter>,
  ))
}

async function search(plate: string) {
  const input = container.querySelector<HTMLInputElement>('#cashier-plate')!
  const setValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!
  await act(async () => {
    setValue.call(input, plate)
    input.dispatchEvent(new Event('input', { bubbles: true }))
  })
  await act(async () => container.querySelector('form')!.dispatchEvent(
    new Event('submit', { bubbles: true, cancelable: true }),
  ))
}

function payButton() {
  return [...container.querySelectorAll('button')].find((button) => button.textContent?.startsWith('Registrar pago'))
}

async function clickPay() {
  await act(async () => payButton()!.dispatchEvent(new MouseEvent('click', { bubbles: true })))
}

function stubCsrf() {
  fetchMock.mockResolvedValueOnce(response(200, { headerName: 'X-CSRF-TOKEN', token: 't' }))
}

function paymentBodies() {
  return fetchMock.mock.calls
    .filter(([url]) => String(url).endsWith('/api/v1/payments'))
    .map(([, init]) => JSON.parse(String((init as RequestInit).body)))
}

describe('caja: pago en efectivo (HU-16)', () => {
  it('registra el pago del cobro pendiente y muestra el comprobante con el pase', async () => {
    await renderCashier()
    fetchMock.mockResolvedValueOnce(response(200, pendingCharge))
    await search('ABC123')
    expect(payButton()?.textContent).toContain('10.000')

    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(201, {
      payment, plate: 'ABC123', chargeStatus: 'PAGADO', passValidUntil: '2026-11-24T06:00:00-05:00', replayed: false,
    }))
    await clickPay()

    const receipt = container.querySelector('[role="status"]')?.textContent ?? ''
    expect(receipt).toContain('Pago registrado')
    expect(receipt).toContain('Operador: operador')
    expect(receipt).toContain('Pase vigente hasta')
    expect(container.textContent).toContain('Estado: PAGADO')
    expect(paymentBodies()[0]).toMatchObject({ chargeId: 7, amount: 10000 })
  })

  it('reutiliza la misma clave de idempotencia al reintentar tras un error', async () => {
    await renderCashier()
    fetchMock.mockResolvedValueOnce(response(200, pendingCharge))
    await search('ABC123')

    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(500, { message: 'Error temporal' }))
    await clickPay()
    expect(container.querySelector('[role="alert"]')?.textContent).toContain('Error temporal')

    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(200, {
      payment, plate: 'ABC123', chargeStatus: 'PAGADO', passValidUntil: '2026-11-24T06:00:00-05:00', replayed: true,
    }))
    await clickPay()

    const [first, second] = paymentBodies()
    expect(first.idempotencyKey).toBeTruthy()
    expect(second.idempotencyKey).toBe(first.idempotencyKey)
    expect(container.querySelector('[role="status"]')?.textContent).toContain('ya estaba registrado')
  })

  it('para un cobro ya pagado muestra el pago registrado y no ofrece pagar otra vez (CA-03 y CA-06)', async () => {
    await renderCashier()
    fetchMock
      .mockResolvedValueOnce(response(200, { ...pendingCharge, status: 'PAGADO' }))
      .mockResolvedValueOnce(response(200, payment))
    await search('ABC123')

    expect(payButton()).toBeUndefined()
    expect(container.querySelector('[role="status"]')?.textContent).toContain('Importe')
  })

  it('el panel de operador enlaza a la caja', async () => {
    fetchMock.mockResolvedValueOnce(response(200, operator))
    await act(async () => root.render(
      <MemoryRouter initialEntries={['/operator']}>
        <AppRouter />
      </MemoryRouter>,
    ))
    expect(container.querySelector('a[href="/operator/cashier"]')).toBeTruthy()
  })
})
