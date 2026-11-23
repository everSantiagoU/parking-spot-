import { afterEach, describe, expect, it, vi } from 'vitest'
import { findTodayCharge, generateCharge } from '../features/charges/chargesApi'

afterEach(() => vi.unstubAllGlobals())

const charge = {
  plate: 'ABC123',
  vehicleType: 'CARRO',
  operatingDay: '2026-11-23',
  timeSlot: 'DIURNA',
  outcome: 'CREADO',
  chargeId: 7,
  appliedTariff: 10000,
  amount: 10000,
  status: 'PENDIENTE',
  generatedAt: '2026-11-23T10:00:00-05:00',
}

function ok(body: unknown) {
  return { ok: true, status: 200, json: async () => body }
}

describe('cliente de cobros', () => {
  it('registra el cobro del ingreso enviando la placa con token CSRF', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(ok({ headerName: 'X-CSRF-TOKEN', token: 't' }))
      .mockResolvedValueOnce(ok(charge))
    vi.stubGlobal('fetch', fetchMock)

    await expect(generateCharge('abc123')).resolves.toEqual(charge)
    expect(fetchMock).toHaveBeenLastCalledWith(
      expect.stringContaining('/api/v1/charges'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ plate: 'abc123' }),
        headers: expect.objectContaining({ 'X-CSRF-TOKEN': 't' }),
      }),
    )
  })

  it('consulta el cobro de hoy con la placa en la URL', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(ok(charge))
    vi.stubGlobal('fetch', fetchMock)

    await findTodayCharge(' ABC123 ')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/charges/today?plate=ABC123'),
      expect.anything(),
    )
  })

  it('propaga el mensaje de error del backend', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce({
      ok: false,
      status: 404,
      json: async () => ({ code: 'COBRO_NO_ENCONTRADO', message: 'La placa ABC123 no tiene cobro.' }),
    }))

    await expect(findTodayCharge('ABC123')).rejects.toThrow('La placa ABC123 no tiene cobro.')
  })
})
