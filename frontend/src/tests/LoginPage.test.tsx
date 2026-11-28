// @vitest-environment jsdom
import { act } from 'react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { ADMIN, CSRF, OPERATOR, TestApp } from './support/testApp'

// HU-06 CA-01 y HU-07 CA-01/CA-02/CA-03: formulario de inicio de sesión, resultado y errores.
let app: TestApp

beforeEach(() => { app = new TestApp() })
afterEach(async () => { await app.unmount() })

async function login(username: string, password: string) {
  await app.type('input[name="username"]', username)
  await app.type('input[name="password"]', password)
  await app.submit()
}

describe('inicio de sesión', () => {
  it('un login válido de operador envía las credenciales con CSRF y abre el panel de operador', async () => {
    app.on('POST', '/api/v1/auth/login', { status: 200, body: OPERATOR })
    await app.render('/login', null)

    await login('operador', 'clave-secreta')

    const [call] = app.sent('POST', '/api/v1/auth/login')
    expect(String(call.init.body)).toBe('username=operador&password=clave-secreta')
    expect((call.init.headers as Record<string, string>)[CSRF.headerName]).toBe(CSRF.token)
    expect(app.text()).toContain('Panel de operador')
  })

  it('un login válido de administrador abre el panel de administración', async () => {
    app.on('POST', '/api/v1/auth/login', { status: 200, body: ADMIN })
    await app.render('/login', null)

    await login('admin', 'clave-secreta')

    expect(app.text()).toContain('Panel de administración')
  })

  it('credenciales inválidas muestran el error, no entran al panel y limpian la contraseña', async () => {
    app.on('POST', '/api/v1/auth/login', {
      status: 401, body: { code: 'INVALID_CREDENTIALS', message: 'Credenciales inválidas.' },
    })
    await app.render('/login', null)

    await login('operador', 'equivocada')

    expect(app.role('alert')).toEqual(['Credenciales inválidas.'])
    expect(app.text()).not.toContain('Panel de operador')
    expect(app.container.querySelector<HTMLInputElement>('input[name="password"]')!.value).toBe('')
  })

  it('un fallo de conexión muestra un mensaje para reintentar', async () => {
    app.on('POST', '/api/v1/auth/login', new TypeError('Failed to fetch'))
    await app.render('/login', null)

    await login('operador', 'clave')

    expect(app.role('alert')).toEqual(['No se pudo iniciar sesión. Intenta nuevamente.'])
  })

  it('mientras se consulta la sesión el botón está deshabilitado y lo indica', async () => {
    let release: (value: { status: number }) => void = () => {}
    app.on('GET', '/api/v1/auth/me', () => new Promise((resolve) => { release = resolve }))
    await app.render('/login')

    const button = app.container.querySelector('button')
    expect(button?.textContent).toBe('Consultando sesión…')
    expect(button?.disabled).toBe(true)

    await act(async () => release({ status: 401 }))
    expect(app.container.querySelector('button')?.textContent).toBe('Entrar')
  })

  it('un usuario con sesión activa que abre /login va directo a su panel', async () => {
    await app.render('/login', OPERATOR)

    expect(app.text()).toContain('Panel de operador')
    expect(app.container.querySelector('input[name="password"]')).toBeNull()
  })
})
