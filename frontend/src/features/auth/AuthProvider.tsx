import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { ApiError, onUnauthorized, request } from '../../services/httpClient'

export type Role = 'OPERADOR' | 'ADMIN'
type SessionUser = { username: string; role: Role }

type AuthState = {
  user: SessionUser | null
  loading: boolean
  error: string
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    const unsubscribe = onUnauthorized(() => setUser(null))
    request<SessionUser>('/api/v1/auth/me', { signal: controller.signal })
      .then((session) => {
        if (!controller.signal.aborted) setUser(session)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted && !(cause instanceof ApiError && cause.status === 401)) {
          setError('No se pudo consultar la sesión. Recarga la página para volver a intentar.')
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => {
      controller.abort()
      unsubscribe()
    }
  }, [])

  async function login(username: string, password: string) {
    const session = await request<SessionUser>('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ username, password }),
    })
    setUser(session)
  }

  async function logout() {
    await request<void>('/api/v1/auth/logout', { method: 'POST' })
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, loading, error, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const session = useContext(AuthContext)
  if (!session) throw new Error('Falta AuthProvider')
  return session
}
