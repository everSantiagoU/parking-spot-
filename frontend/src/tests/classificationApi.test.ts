import { afterEach, describe, expect, it, vi } from 'vitest'
import { classifyVehicle } from '../features/vehicles/classificationApi'

afterEach(() => vi.unstubAllGlobals())

function withCsrf(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mockResolvedValueOnce({
    ok: true,
    json: async () => ({ headerName: 'X-CSRF-TOKEN', token: 'test-token' }),
  })
}


describe('cliente de clasificación', () => {
  it('envía la placa y devuelve la clasificación del backend', async () => {
    const fetchMock = withCsrf(vi.fn()).mockResolvedValue({
      ok: true,
      json: async () => ({ plate: 'ABC123', type: 'CARRO' }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(classifyVehicle(' abc123 ')).resolves.toEqual({ plate: 'ABC123', type: 'CARRO' })
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/vehicles/classify'),
      expect.objectContaining({ method: 'POST', body: JSON.stringify({ plate: ' abc123 ' }) }),
    )
  })

  it('muestra el error informado por el backend', async () => {
    vi.stubGlobal('fetch', withCsrf(vi.fn()).mockResolvedValue({
      ok: false,
      json: async () => ({ code: 'INVALID_PLATE', message: 'Placa inválida' }),
    }))

    await expect(classifyVehicle('BAD')).rejects.toThrow('Placa inválida')
  })
})
