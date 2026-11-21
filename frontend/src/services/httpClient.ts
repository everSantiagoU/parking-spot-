import { apiBaseUrl } from '../app/config/env'

/** Error devuelto por el backend con el formato { code, message }. */
export class ApiError extends Error {
  readonly status: number
  readonly code?: string

  constructor(status: number, message: string, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

type ErrorBody = { code?: string; message?: string }

export type RequestOptions = Omit<RequestInit, 'headers'> & { headers?: Record<string, string> }

const DEFAULT_ERROR_MESSAGE = 'No se pudo completar la solicitud.'

/**
 * Cliente HTTP único de la aplicación. Todas las features lo usan para hablar
 * con el backend, así la URL base y el manejo de errores viven en un solo lugar.
 */
export async function request<T>(path: string, init: RequestOptions = {}): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init.headers },
  })

  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as ErrorBody
    throw new ApiError(response.status, body.message ?? DEFAULT_ERROR_MESSAGE, body.code)
  }

  return (await response.json()) as T
}

export function postJson<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, { method: 'POST', body: JSON.stringify(body) })
}
