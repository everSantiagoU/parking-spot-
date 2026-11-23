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

const sessionEvents = new EventTarget()

export function onUnauthorized(listener: () => void) {
  sessionEvents.addEventListener('unauthorized', listener)
  return () => sessionEvents.removeEventListener('unauthorized', listener)
}

const DEFAULT_ERROR_MESSAGE = 'No se pudo completar la solicitud.'

/**
 * Cliente HTTP único de la aplicación. Todas las features lo usan para hablar
 * con el backend, así la URL base y el manejo de errores viven en un solo lugar.
 */
export async function request<T>(path: string, init: RequestOptions = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers: Record<string, string> = {
    ...(init.body == null ? {} : { 'Content-Type': 'application/json' }),
    ...init.headers,
  }
  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
    const csrf = await request<{ headerName: string; token: string }>('/api/v1/auth/csrf', {
      signal: init.signal,
    })
    headers[csrf.headerName] = csrf.token
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    credentials: 'include',
    headers,
  })

  if (!response.ok) {
    if (response.status === 401 && !init.signal?.aborted) {
      sessionEvents.dispatchEvent(new Event('unauthorized'))
    }
    if (response.status === 403) {
      throw new ApiError(403, 'Acceso denegado.', 'ACCESS_DENIED')
    }
    const body = (await response.json().catch(() => ({}))) as ErrorBody
    throw new ApiError(response.status, body.message ?? DEFAULT_ERROR_MESSAGE, body.code)
  }

  return response.status === 204 ? undefined as T : (await response.json()) as T
}

export function postJson<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, { method: 'POST', body: JSON.stringify(body) })
}
