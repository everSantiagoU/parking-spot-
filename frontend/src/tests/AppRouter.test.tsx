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
    expect(container.textContent).toContain('Tarifas y franjas')
    expect(container.querySelector('a[href="/operator"]')).toBeNull()
  })

  it('bloquea el acceso directo al panel del otro rol y enlaza al panel propio', async () => {
    await render('/admin', operator)
    expect(container.querySelector('[role="alert"]')?.textContent).toBe('Acceso denegado.')
    expect(container.querySelector('a')?.getAttribute('href')).toBe('/operator')
    expect(container.textContent).not.toContain('Tarifas y franjas')
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
