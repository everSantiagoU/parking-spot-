// @vitest-environment jsdom
import { act } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AppRouter } from '../app/router/AppRouter'

const operator = { username: 'operador', role: 'OPERADOR' }
const validPass = {
  plate: 'ABC123', chargeId: 7, siteId: 1, operatingDay: '2026-11-23',
  validUntil: '2026-11-24T06:00:00-05:00', status: 'VIGENTE', freeReentry: true,
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

async function render(path: string) {
  fetchMock.mockResolvedValueOnce(response(200, operator))
  await act(async () => root.render(
    <MemoryRouter initialEntries={[path]}>
      <AppRouter />
    </MemoryRouter>,
  ))
}

async function search(plate: string) {
  const input = container.querySelector<HTMLInputElement>('#pass-plate')!
  const setValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!
  await act(async () => {
    setValue.call(input, plate)
    input.dispatchEvent(new Event('input', { bubbles: true }))
  })
  await act(async () => container.querySelector('form')!.dispatchEvent(
    new Event('submit', { bubbles: true, cancelable: true }),
  ))
}

describe('consulta de pase (HU-17)', () => {
  it('muestra el estado, la vigencia y que el reingreso no tiene cobro (CA-06)', async () => {
    await render('/operator/pass')
    fetchMock.mockResolvedValueOnce(response(200, validPass))
    await search('abc123')

    const [url] = fetchMock.mock.calls.at(-1)!
    expect(String(url)).toContain('/api/v1/passes?plate=abc123')
    const result = container.querySelector('[role="status"]')?.textContent ?? ''
    expect(result).toContain('Placa: ABC123 X')
    expect(result).toContain('Estado: Vigente')
    expect(result).toContain('Vigente hasta')
    expect(result).toContain('sin un cobro adicional')
  })

  it('un pase vencido indica que el reingreso genera cobro (CA-03)', async () => {
    await render('/operator/pass')
    fetchMock.mockResolvedValueOnce(response(200, { ...validPass, status: 'VENCIDO', freeReentry: false }))
    await search('ABC123')

    const result = container.querySelector('[role="status"]')?.textContent ?? ''
    expect(result).toContain('Estado: Vencido')
    expect(result).toContain('genera un cobro nuevo')
  })

  it('un pase de otra sede no habilita la exención (CA-05)', async () => {
    await render('/operator/pass')
    fetchMock.mockResolvedValueOnce(response(200, { ...validPass, status: 'OTRA_SEDE', freeReentry: false }))
    await search('ABC123')

    expect(container.querySelector('[role="status"]')?.textContent).toContain('De otra sede')
  })

  it('muestra el mensaje del backend cuando la placa no tiene pase', async () => {
    await render('/operator/pass')
    fetchMock.mockResolvedValueOnce(response(404, {
      code: 'PASE_NO_ENCONTRADO', message: 'La placa ABC123 no tiene un pase registrado.',
    }))
    await search('ABC123')

    expect(container.querySelector('[role="alert"]')?.textContent).toContain('no tiene un pase registrado')
  })

  it('el panel de operador enlaza a la consulta de pases', async () => {
    await render('/operator')
    expect(container.querySelector('a[href="/operator/pass"]')).toBeTruthy()
  })
})
