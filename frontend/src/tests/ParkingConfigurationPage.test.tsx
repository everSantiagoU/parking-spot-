// @vitest-environment jsdom
import { act } from 'react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { ADMIN, OPERATOR, TestApp } from './support/testApp'

// HU-12 (tarifas, franjas y cupos) en la interfaz: carga, guardado, errores del backend y acceso por rol.
const CONFIG_PATH = '/api/v1/admin/sites/1/configuration'
const configuration = {
  dayStart: '06:00:00', nightStart: '18:00:00',
  car: { day: 10000, night: 6000, capacity: 50 },
  motorcycle: { day: 0, night: 0, capacity: 30 },
}
let app: TestApp

beforeEach(() => {
  app = new TestApp()
    .on('GET', '/api/v1/admin/sites', { status: 200, body: [{ id: 1, name: 'Sede Principal' }] })
    .on('GET', CONFIG_PATH, { status: 200, body: configuration })
})
afterEach(async () => { await app.unmount() })

function inputs() {
  return [...app.container.querySelectorAll<HTMLInputElement>('form input')]
}

async function setNumber(index: number, value: string) {
  const input = inputs()[index]
  const setValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!
  await act(async () => {
    setValue.call(input, value)
    input.dispatchEvent(new Event('input', { bubbles: true }))
  })
}

describe('configuración del parqueadero', () => {
  it('carga la sede, las franjas, las tarifas y los cupos guardados (CA-01, CA-03)', async () => {
    await app.render('/admin/configuration', ADMIN)

    expect(app.text()).toContain('Tarifas, franjas y cupos')
    expect(app.container.querySelector('select')?.textContent).toContain('Sede Principal')
    expect(inputs().map((input) => input.value)).toEqual(['06:00:00', '18:00:00', '10000', '6000', '50', '0', '0', '30'])
  })

  it('guarda la nueva tarifa y confirma el resultado (CA-01)', async () => {
    let saved: unknown
    app.on('PUT', CONFIG_PATH, (init) => {
      saved = JSON.parse(String(init.body))
      return { status: 204 }
    })
    await app.render('/admin/configuration', ADMIN)

    await setNumber(3, '7000')
    await app.submit()

    expect(saved).toMatchObject({ car: { day: 10000, night: 7000, capacity: 50 } })
    expect(app.role('status')).toContain('Configuración guardada.')
  })

  it('muestra el rechazo del backend cuando la nocturna es mayor o igual a la diurna (CA-02, escenario 7)', async () => {
    const message = 'La tarifa nocturna debe ser menor que la diurna. El carro requiere tarifas positivas.'
    app.on('PUT', CONFIG_PATH, { status: 422, body: { code: 'CONFIGURACION_INVALIDA', message } })
    await app.render('/admin/configuration', ADMIN)

    await setNumber(3, '12000')
    await app.submit()

    expect(app.role('alert')).toEqual([message])
    expect(app.role('status')).not.toContain('Configuración guardada.')
  })

  it('muestra el rechazo de un cupo inválido (CA-04)', async () => {
    const message = 'Revisa los importes y la capacidad de CARRO.'
    app.on('PUT', CONFIG_PATH, { status: 422, body: { code: 'CONFIGURACION_INVALIDA', message } })
    await app.render('/admin/configuration', ADMIN)

    await setNumber(4, '-1')
    await app.submit()

    expect(app.role('alert')).toEqual([message])
  })

  it('informa el fallo al cargar la configuración', async () => {
    app.on('GET', CONFIG_PATH, { status: 500 })
    await app.render('/admin/configuration', ADMIN)

    expect(app.role('alert')).toEqual(['No se pudo cargar la configuración.'])
  })

  it('un operador no puede abrir la configuración (CA-05)', async () => {
    await app.render('/admin/configuration', OPERATOR)

    expect(app.role('alert')).toEqual(['Acceso denegado.'])
    expect(app.container.querySelector('form')).toBeNull()
    expect(app.sent('GET', CONFIG_PATH)).toHaveLength(0)
  })
})
