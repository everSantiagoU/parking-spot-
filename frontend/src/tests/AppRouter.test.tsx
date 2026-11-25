// @vitest-environment jsdom
import { act } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { MemoryRouter, useLocation } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AppRouter } from '../app/router/AppRouter'

const operator = { username: 'operador', role: 'OPERADOR' }
const admin = { username: 'administrador', role: 'ADMIN' }

function response(status: number, body: unknown = {}) {
  return { ok: status >= 200 && status < 300, status, json: async () => body }
}

function CurrentLocation() {
  return <output aria-label="Ruta de prueba">{useLocation().pathname}</output>
}

let container: HTMLDivElement
let root: Root
type FetchStub = (url: string, init?: RequestInit) => Promise<ReturnType<typeof response>>

let fetchMock: ReturnType<typeof vi.fn<FetchStub>>
let occupancyFetchMock: ReturnType<typeof vi.fn<FetchStub>>

function occupancyResponse(cars: number, motorcycles = 2) {
  return response(200, [
    { vehicleType: 'CARRO', capacity: 10, occupied: cars },
    { vehicleType: 'MOTO', capacity: 5, occupied: motorcycles },
  ])
}

beforeEach(() => {
  vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true)
  fetchMock = vi.fn<FetchStub>()
  occupancyFetchMock = vi.fn<FetchStub>().mockResolvedValue(occupancyResponse(10))
  vi.stubGlobal('fetch', (url: string, init?: RequestInit) =>
    (url.endsWith('/api/v1/occupancy') ? occupancyFetchMock : fetchMock)(url, init),
  )
  container = document.createElement('div')
  document.body.append(container)
  root = createRoot(container)
})

afterEach(async () => {
  await act(async () => root.unmount())
  container.remove()
  vi.unstubAllGlobals()
})

async function render(path: string, session?: typeof operator) {
  fetchMock.mockResolvedValueOnce(session ? response(200, session) : response(401))
  await act(async () => root.render(
    <MemoryRouter initialEntries={[path]}>
      <AppRouter />
      <CurrentLocation />
    </MemoryRouter>,
  ))
}

function currentPath() {
  return container.querySelector('output')?.textContent
}

function stubCsrf() {
  fetchMock.mockResolvedValueOnce(response(200, { headerName: 'X-CSRF-TOKEN', token: 'token' }))
}

describe('rutas y sesión', () => {
  it('muestra la bienvenida con el enlace de inicio de sesión', async () => {
    await render('/')
    expect(container.querySelector('h1')?.textContent).toBe('Parking Spot')
    expect(container.querySelector('a')?.getAttribute('href')).toBe('/login')
  })

  it.each(['/operator', '/admin', '/vehicles/classify', '/classify'])(
    'envía a iniciar sesión desde %s ante un 401 al recuperar la sesión',
    async (path) => {
      await render(path)
      expect(currentPath()).toBe('/login')
      expect(container.querySelector('h1')?.textContent).toBe('Iniciar sesión')
    },
  )

  it('recupera la sesión del operador y solo enlaza sus módulos', async () => {
    await render('/operator', operator)
    expect(container.textContent).toContain('Panel de operador')
    expect(container.querySelector('a[href="/vehicles/classify"]')).toBeTruthy()
    expect(container.querySelector('a[href="/admin"]')).toBeNull()
  })

  it('recupera la sesión de administrador y muestra sus módulos', async () => {
    await render('/admin', admin)
    expect(container.textContent).toContain('Tarifas, franjas y cupos')
    expect(container.querySelector('a[href="/operator"]')).toBeNull()
  })

  it('bloquea el acceso directo al panel del otro rol y enlaza al panel propio', async () => {
    await render('/admin', operator)
    expect(container.querySelector('[role="alert"]')?.textContent).toBe('Acceso denegado.')
    expect(container.querySelector('a')?.getAttribute('href')).toBe('/operator')
    expect(container.textContent).not.toContain('Tarifas, franjas y cupos')
  })

  it.each([401, 403])('gestiona HTTP %s al clasificar con una sesión previamente válida', async (status) => {
    await render('/vehicles/classify', operator)
    expect(container.querySelector('input[name="plate"]')).toBeTruthy()
    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(status))
    await act(async () => container.querySelector('form')?.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    ))

    if (status === 401) {
      expect(currentPath()).toBe('/login')
      expect(container.querySelector('input[name="plate"]')).toBeNull()
    } else {
      expect(currentPath()).toBe('/vehicles/classify')
      expect(container.querySelector('[role="alert"]')?.textContent).toBe('Acceso denegado.')
      expect(container.textContent).toContain('Cerrar sesión')
    }
  })

  it.each([
    [operator, '/operator'],
    [admin, '/admin'],
  ] as const)('abre el panel de %o al iniciar sesión y vuelve a login al salir', async (session, panel) => {
    await render('/login')
    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(200, session))
    const form = container.querySelector('form')!
    form.querySelector<HTMLInputElement>('[name="username"]')!.value = session.username
    form.querySelector<HTMLInputElement>('[name="password"]')!.value = crypto.randomUUID()
    await act(async () => form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })))
    expect(currentPath()).toBe(panel)

    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(204))
    await act(async () => container.querySelector('nav button')!.dispatchEvent(
      new MouseEvent('click', { bubbles: true }),
    ))
    expect(currentPath()).toBe('/login')
  })
})

describe('carril de salida', () => {
  async function openLane(plate = 'abc123') {
    await render('/operator', operator)
    const card = container.querySelector<HTMLAnchorElement>('a[href="/operator/exit"]')!
    expect(card.textContent).toContain('Carril de salida')
    await act(async () => card.click())
    const input = container.querySelector<HTMLInputElement>('#exit-plate')!
    const setValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!
    await act(async () => {
      setValue.call(input, plate)
      input.dispatchEvent(new Event('input', { bubbles: true }))
    })
  }

  function submitExit() {
    container.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }))
  }

  it.each([
    ['CP-HU-18-E01', 'abc123', true, 'PAGO_CONFIRMADO', 'El cobro del día operativo está pagado.'],
    ['CP-HU-18-E02', 'abc12d', true, 'EXENCION_COMPROBADA', 'El ingreso tiene una exención registrada'],
    ['CP-HU-18-X01', 'abc123', false, 'PAGO_PENDIENTE', 'Falta el pago del cobro del día operativo.'],
  ] as const)('%s muestra el estado y el motivo devuelto al solicitar salida', async (_, plate, barrierOpen, code, reason) => {
    await openLane(plate)
    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(200, { barrierOpen, code }))
    await act(async () => submitExit())

    expect(fetchMock).toHaveBeenLastCalledWith(expect.stringContaining('/api/v1/exits/validate'),
      expect.objectContaining({
        method: 'POST', body: JSON.stringify({ plate }), credentials: 'include',
        headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'token' }),
      }))
    const status = container.querySelector('[role="status"]')?.textContent
    expect(status).toContain(barrierOpen ? 'Barrera simulada abierta' : 'Barrera simulada cerrada')
    expect(status).toContain(reason)
  })

  it('CP-HU-18-X02 limpia la apertura al reintentar, evita duplicados y muestra el fallo de conexión', async () => {
    await openLane()
    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(200, { barrierOpen: true, code: 'PAGO_CONFIRMADO' }))
    await act(async () => submitExit())
    expect(container.textContent).toContain('Barrera simulada abierta')

    let rejectRequest!: (error: Error) => void
    stubCsrf()
    fetchMock.mockImplementationOnce(() => new Promise((_, reject) => { rejectRequest = reject }))
    await act(async () => {
      submitExit()
      submitExit()
    })
    expect(container.querySelector('button[type="submit"]')).toHaveProperty('disabled', true)
    expect(container.textContent).toContain('Validando')
    expect(container.textContent).not.toContain('Barrera simulada abierta')
    expect(fetchMock.mock.calls.filter(([url]) => url.endsWith('/api/v1/exits/validate'))).toHaveLength(2)

    await act(async () => rejectRequest(new TypeError('Failed to fetch')))
    expect(container.querySelector('[role="alert"]')?.textContent).toContain('No se pudo validar la salida')
    expect(container.textContent).toContain('Barrera simulada cerrada')
    expect(container.textContent).not.toContain('Barrera simulada abierta')
  })

  it('cierra la barrera si el backend responde con un error de auditoría', async () => {
    await openLane()
    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(503, { barrierOpen: false, code: 'ERROR_AUDITORIA' }))
    await act(async () => submitExit())
    expect(container.querySelector('[role="alert"]')?.textContent).toContain('No se pudo registrar el intento')
    expect(container.textContent).toContain('Barrera simulada cerrada')
  })

  it('exige una autorización explícita del backend', async () => {
    await openLane()
    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(200, { code: 'PAGO_CONFIRMADO' }))
    await act(async () => submitExit())
    expect(container.querySelector('[role="alert"]')).toBeTruthy()
    expect(container.textContent).toContain('Barrera simulada cerrada')
  })

  function occupancyRows() {
    return [...container.querySelectorAll('tbody tr')].map((row) =>
      [...row.querySelectorAll('th, td')].map((cell) => cell.textContent),
    )
  }

  it('muestra los cupos persistidos y los consulta otra vez tras una salida autorizada', async () => {
    await openLane()
    expect(occupancyRows()).toEqual([
      ['Carros', '10', '10', '0'],
      ['Motos', '5', '2', '3'],
    ])

    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(200, { barrierOpen: true, code: 'PAGO_CONFIRMADO' }))
    let receiveOccupancy!: (result: ReturnType<typeof response>) => void
    occupancyFetchMock.mockImplementationOnce(() => new Promise((resolve) => { receiveOccupancy = resolve }))
    await act(async () => submitExit())
    expect(container.textContent).toContain('Barrera simulada abierta')
    expect(container.textContent).toContain('Consultando disponibilidad')
    expect(occupancyRows()[0]).toEqual(['Carros', '10', '10', '0'])

    await act(async () => receiveOccupancy(occupancyResponse(9)))
    expect(occupancyRows()).toEqual([
      ['Carros', '10', '9', '1'],
      ['Motos', '5', '2', '3'],
    ])
    expect(occupancyFetchMock).toHaveBeenCalledTimes(2)
    expect(occupancyFetchMock).toHaveBeenLastCalledWith(expect.stringContaining('/api/v1/occupancy'),
      expect.objectContaining({ credentials: 'include', cache: 'no-store' }))
  })

  it('conserva la disponibilidad ante una salida rechazada', async () => {
    await openLane()
    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(200, { barrierOpen: false, code: 'PAGO_PENDIENTE' }))
    await act(async () => submitExit())

    expect(container.textContent).toContain('Barrera simulada cerrada')
    expect(occupancyRows()).toEqual([
      ['Carros', '10', '10', '0'],
      ['Motos', '5', '2', '3'],
    ])
    expect(occupancyFetchMock).toHaveBeenCalledTimes(1)
  })

  it('conserva el resultado de la salida si falla la consulta posterior de cupos y permite reintentar', async () => {
    await openLane()
    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(200, { barrierOpen: true, code: 'PAGO_CONFIRMADO' }))
    occupancyFetchMock.mockResolvedValueOnce(response(503))
    await act(async () => submitExit())

    expect(container.textContent).toContain('Barrera simulada abierta')
    expect(container.querySelector('[role="alert"]')?.textContent).toContain('No se pudo actualizar la disponibilidad')
    expect(occupancyRows()[0]).toEqual(['Carros', '10', '10', '0'])

    occupancyFetchMock.mockResolvedValueOnce(occupancyResponse(9))
    const refresh = [...container.querySelectorAll('button')].find((button) => button.textContent === 'Actualizar cupos')!
    await act(async () => refresh.click())
    expect(container.querySelector('[role="alert"]')).toBeNull()
    expect(container.textContent).toContain('Barrera simulada abierta')
    expect(occupancyRows()[0]).toEqual(['Carros', '10', '9', '1'])
  })

  it('bloquea la ruta a un administrador', async () => {
    await render('/operator/exit', admin)
    expect(container.querySelector('[role="alert"]')?.textContent).toBe('Acceso denegado.')
    expect(container.querySelector('#exit-plate')).toBeNull()
  })
})

describe('registro manual de ingreso', () => {
  async function typeEntryPlate() {
    const input = container.querySelector<HTMLInputElement>('#entry-plate')!
    const setValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!
    await act(async () => {
      setValue.call(input, 'abc123')
      input.dispatchEvent(new Event('input', { bubbles: true }))
    })
  }

  async function submitEntry() {
    await act(async () => container.querySelector('form')!.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    ))
  }

  it('permite registrar el ingreso desde el panel y continuar a cobros', async () => {
    await render('/operator', operator)
    const card = container.querySelector<HTMLAnchorElement>('a[href="/operator/entry"]')
    expect(card?.textContent).toContain('Carril de entrada')
    await act(async () => card!.click())
    expect(currentPath()).toBe('/operator/entry')

    await typeEntryPlate()
    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(201, {
      resultado: 'AUTORIZADO', barreraAbierta: true, eventoId: 12, plate: 'ABC123',
    }))
    await submitEntry()

    expect(fetchMock).toHaveBeenLastCalledWith(expect.stringContaining('/api/v1/entries/manual'),
      expect.objectContaining({
        method: 'POST', body: JSON.stringify({ plate: 'abc123' }), credentials: 'include',
        headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'token' }),
      }))
    expect(container.querySelector('[role="status"]')?.textContent).toContain('Ingreso autorizado.')
    expect(container.querySelector('[role="status"]')?.textContent).toContain('ABC123')
    expect(container.querySelector('button[type="submit"]')).toHaveProperty('disabled', true)

    const chargeLink = container.querySelector<HTMLAnchorElement>('a[href="/operator/charges"]')!
    await act(async () => chargeLink.click())
    expect(currentPath()).toBe('/operator/charges')
    expect(container.querySelector('a[href="/operator/entry"]')?.textContent).toBe('Carril de entrada')
  })

  it('muestra que no hay cupo cuando el ingreso se rechaza', async () => {
    await render('/operator/entry', operator)
    await typeEntryPlate()
    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(409, {
      resultado: 'LLENO', motivo: 'LLENO', barreraAbierta: false, eventoId: 13, plate: 'ABC123',
    }))
    await submitEntry()

    expect(container.querySelector('[role="alert"]')?.textContent).toContain('No hay cupo')
    expect(container.textContent).not.toContain('Ingreso autorizado.')
    expect(container.querySelector('a[href="/operator/charges"]')).toBeNull()
  })

  it('bloquea el acceso al ingreso para un administrador', async () => {
    await render('/operator/entry', admin)
    expect(container.querySelector('[role="alert"]')?.textContent).toBe('Acceso denegado.')
    expect(container.querySelector('#entry-plate')).toBeNull()
  })

  it('enlaza al carril de entrada cuando la validación de salida no encuentra un ingreso', async () => {
    await render('/operator/exit', operator)
    const input = container.querySelector<HTMLInputElement>('#exit-plate')!
    const setValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!
    await act(async () => {
      setValue.call(input, 'abc123')
      input.dispatchEvent(new Event('input', { bubbles: true }))
    })
    stubCsrf()
    fetchMock.mockResolvedValueOnce(response(200, { code: 'SIN_INGRESO_ACTIVO', barrierOpen: false }))
    await act(async () => container.querySelector('form')!.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    ))

    expect(container.textContent).toContain('La placa no tiene un ingreso activo en esta sede.')
    expect(container.textContent).toContain('Barrera simulada cerrada')
    const entryLink = container.querySelector<HTMLAnchorElement>('a[href="/operator/entry"]')!
    await act(async () => entryLink.click())
    expect(currentPath()).toBe('/operator/entry')
    expect(fetchMock.mock.calls.some(([url]) => url.endsWith('/api/v1/entries/manual'))).toBe(false)
  })
})
