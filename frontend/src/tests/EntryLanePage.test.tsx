// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { OPERATOR, TestApp } from './support/testApp'

// HU-10, HU-11 y HU-13 en el carril de entrada: resultado autorizado, LLENO, ingreso activo y acceso denegado.
const ENTRY = '/api/v1/entries/manual'
let app: TestApp

beforeEach(() => { app = new TestApp() })
afterEach(async () => { await app.unmount() })

async function registerEntry(plate: string) {
  await app.render('/operator/entry', OPERATOR)
  await app.type('#entry-plate', plate)
  await app.submit()
}

describe('carril de entrada', () => {
  it('registra la placa digitada (origen MANUAL) y confirma el ingreso autorizado', async () => {
    let sent: unknown
    app.on('POST', ENTRY, (init) => {
      sent = JSON.parse(String(init.body))
      return { status: 201, body: { resultado: 'AUTORIZADO', barreraAbierta: true, plate: 'ABC123' } }
    })

    await registerEntry('abc123')

    expect(sent).toEqual({ plate: 'ABC123' })
    expect(app.role('status').join(' ')).toContain('ABC123 · Ingreso autorizado.')
    expect(app.container.querySelector('a[href="/operator/charges"]')).not.toBeNull()
  })

  it('HU-13: sin cupo muestra LLENO y no confirma el ingreso', async () => {
    app.on('POST', ENTRY, {
      status: 409, body: { resultado: 'LLENO', motivo: 'LLENO', barreraAbierta: false, plate: 'ABC123' },
    })

    await registerEntry('ABC123')

    expect(app.role('alert')).toEqual(['No hay cupo disponible para este tipo de vehículo.'])
    expect(app.text()).not.toContain('Ingreso autorizado.')
  })

  it('una placa con ingreso activo se rechaza con su motivo', async () => {
    app.on('POST', ENTRY, {
      status: 409, body: { resultado: 'INGRESO_ACTIVO', motivo: 'INGRESO_ACTIVO', barreraAbierta: false },
    })

    await registerEntry('ABC123')

    expect(app.role('alert')).toEqual(['La placa ya tiene un ingreso activo en esta sede.'])
  })

  it('no confirma el ingreso si el backend no autoriza explícitamente la barrera', async () => {
    app.on('POST', ENTRY, { status: 201, body: { resultado: 'AUTORIZADO', barreraAbierta: false, plate: 'ABC123' } })

    await registerEntry('ABC123')

    expect(app.role('alert')).toEqual(['El servidor no confirmó el registro del ingreso.'])
  })

  it('una placa inválida muestra el mensaje del backend', async () => {
    const message = 'La placa debe tener el formato ABC123 (carro) o ABC12D (moto).'
    app.on('POST', ENTRY, { status: 400, body: { code: 'INVALID_PLATE', message } })

    await registerEntry('AB123')

    expect(app.role('alert')).toEqual([message])
  })
})
