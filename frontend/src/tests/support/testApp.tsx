import { act } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { MemoryRouter } from 'react-router'
import { vi } from 'vitest'
import { AppRouter } from '../../app/router/AppRouter'

/**
 * Soporte para las pruebas de páginas (HU-23). Monta la aplicación real con su enrutador y simula el
 * backend por método y ruta, así una prueba no depende del orden exacto de las llamadas HTTP.
 */
export type FakeResponse = { status: number; body?: unknown } | Error
type Handler = FakeResponse | ((init: RequestInit) => FakeResponse | Promise<FakeResponse>)

export const OPERATOR = { username: 'operador', role: 'OPERADOR' }
export const ADMIN = { username: 'admin', role: 'ADMIN' }
export const CSRF = { headerName: 'X-CSRF-TOKEN', token: 'token-de-prueba' }

export class TestApp {
  readonly container = document.createElement('div')
  readonly calls: { method: string; path: string; init: RequestInit }[] = []
  private readonly root: Root
  private readonly routes = new Map<string, Handler>()

  constructor() {
    vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true)
    document.body.append(this.container)
    this.root = createRoot(this.container)
    this.on('GET', '/api/v1/auth/csrf', { status: 200, body: CSRF })
    vi.stubGlobal('fetch', vi.fn(async (url: string, init: RequestInit = {}) => {
      const method = (init.method ?? 'GET').toUpperCase()
      const path = new URL(url, 'http://localhost').pathname
      this.calls.push({ method, path, init })
      const handler = this.routes.get(`${method} ${path}`)
      if (!handler) throw new Error(`Sin respuesta simulada para ${method} ${path}`)
      const result = typeof handler === 'function' ? await handler(init) : handler
      if (result instanceof Error) throw result
      return {
        ok: result.status >= 200 && result.status < 300,
        status: result.status,
        json: async () => result.body ?? {},
      }
    }))
  }

  /** Respuesta simulada del backend para un método y una ruta. */
  on(method: string, path: string, handler: Handler) {
    this.routes.set(`${method} ${path}`, handler)
    return this
  }

  /** Sesión: un usuario, null (sin sesión → 401) o undefined para conservar una respuesta ya definida. */
  async render(path: string, session?: typeof OPERATOR | null) {
    if (session !== undefined) {
      this.on('GET', '/api/v1/auth/me', session ? { status: 200, body: session } : { status: 401 })
    }
    await act(async () => this.root.render(
      <MemoryRouter initialEntries={[path]}>
        <AppRouter />
      </MemoryRouter>,
    ))
  }

  async type(selector: string, value: string) {
    const input = this.container.querySelector<HTMLInputElement>(selector)
    if (!input) throw new Error(`No existe ${selector}`)
    const setValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!
    await act(async () => {
      setValue.call(input, value)
      input.dispatchEvent(new Event('input', { bubbles: true }))
    })
  }

  async submit(selector = 'form') {
    await act(async () => this.container.querySelector(selector)!.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    ))
  }

  text() {
    return this.container.textContent ?? ''
  }

  role(name: 'alert' | 'status') {
    return [...this.container.querySelectorAll(`[role="${name}"]`)].map((node) => node.textContent ?? '')
  }

  sent(method: string, path: string) {
    return this.calls.filter((call) => call.method === method && call.path === path)
  }

  async unmount() {
    await act(async () => this.root.unmount())
    this.container.remove()
    vi.unstubAllGlobals()
  }
}
