// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { OPERATOR, TestApp } from './support/testApp'

// HU-09 en la interfaz: formulario de clasificación, resultado y error del backend.
let app: TestApp

beforeEach(() => { app = new TestApp() })
afterEach(async () => { await app.unmount() })

describe('clasificar vehículo', () => {
  it('muestra la placa normalizada y el tipo que calculó el backend, con el enlace al ingreso', async () => {
    let sent: unknown
    app.on('POST', '/api/v1/vehicles/classify', (init) => {
      sent = JSON.parse(String(init.body))
      return { status: 200, body: { plate: 'ABC12D', type: 'MOTO' } }
    })
    await app.render('/vehicles/classify', OPERATOR)

    await app.type('#plate', 'abc12d')
    await app.submit()

    expect(sent).toEqual({ plate: 'ABC12D' })
    expect(app.role('status')[0]).toContain('Placa: ABC12D')
    expect(app.role('status')[0]).toContain('Tipo: MOTO')
    expect(app.container.querySelector('a[href="/operator/entry"]')?.textContent).toBe('Registrar ingreso de ABC12D')
  })

  it('muestra el mensaje del backend ante un formato inválido', async () => {
    const message = 'La placa debe tener el formato ABC123 (carro) o ABC12D (moto).'
    app.on('POST', '/api/v1/vehicles/classify', { status: 400, body: { code: 'INVALID_PLATE', message } })
    await app.render('/vehicles/classify', OPERATOR)

    await app.type('#plate', 'AB123')
    await app.submit()

    expect(app.role('alert')).toEqual([message])
    expect(app.role('status')).toEqual([])
  })
})
