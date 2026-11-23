import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiBaseUrl } from '../app/config/env'
import { ApiError, postJson, request } from '../services/httpClient'

afterEach(() => vi.unstubAllGlobals())

function withCsrf(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mockResolvedValueOnce({
    ok: true,
    json: async () => ({ headerName: 'X-CSRF-TOKEN', token: 'test-token' }),
  })
}


describe('cliente HTTP', () => {
  it('usa la URL base configurada y envía JSON', async () => {
    const fetchMock = withCsrf(vi.fn()).mockResolvedValue({ ok: true, json: async () => ({ ok: 1 }) })
    vi.stubGlobal('fetch', fetchMock)

    await expect(postJson('/api/v1/demo', { a: 1 })).resolves.toEqual({ ok: 1 })
    expect(fetchMock).toHaveBeenCalledWith(
      `${apiBaseUrl}/api/v1/demo`,
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({ a: 1 }),
        headers: expect.objectContaining({ 'Content-Type': 'application/json', 'X-CSRF-TOKEN': 'test-token' }),
      }),
    )
  })

  it('la URL base no termina en barra', () => {
    expect(apiBaseUrl.endsWith('/')).toBe(false)
  })

  it('convierte la respuesta de error del backend en ApiError', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ code: 'INVALID_PLATE', message: 'Placa inválida' }),
    }))

    const error = await request('/api/v1/demo').catch((cause: unknown) => cause)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 400, code: 'INVALID_PLATE', message: 'Placa inválida' })
  })

  it('usa un mensaje por defecto si el error no trae cuerpo JSON', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => { throw new Error('sin cuerpo') },
    }))

    await expect(request('/api/v1/demo')).rejects.toThrow('No se pudo completar la solicitud.')
  })
})
