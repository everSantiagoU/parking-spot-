const DEFAULT_API_BASE_URL = 'http://localhost:8080'

/**
 * URL base del backend. Se toma de VITE_API_BASE_URL (archivo .env.local o
 * variable de compilación) y se elimina la barra final para concatenar rutas.
 */
export const apiBaseUrl: string = (import.meta.env.VITE_API_BASE_URL ?? DEFAULT_API_BASE_URL).replace(/\/$/, '')
