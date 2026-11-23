import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { ApiError, onUnauthorized, request } from '../../services/httpClient'

export type Role = 'OPERADOR' | 'ADMIN'
type SessionUser = { username: string; role: Role }

export type SessionStatus = 'checking' | 'ready' | 'unavailable' | 'forbidden'

type AuthState = {
  user: SessionUser | null
  sessionStatus: SessionStatus
  retrySession: () => void
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null)
  const [sessionStatus, setSessionStatus] = useState<SessionStatus>('checking')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    const unsubscribe = onUnauthorized(() => setUser(null))
    request<SessionUser>('/api/v1/auth/me', { signal: controller.signal })
      .then((session) => {
        if (controller.signal.aborted) return
        setUser(session)
        setSessionStatus('ready')
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted) return
        const status = cause instanceof ApiError ? cause.status : undefined
        if (status === 401) {
          setUser(null)
          setSessionStatus('ready')
        } else {
          setSessionStatus(status === 403 ? 'forbidden' : 'unavailable')
        }
      })
    return () => {
      controller.abort()
      unsubscribe()
    }
  }, [attempt])

  function retrySession() {
    setSessionStatus('checking')
    setAttempt((current) => current + 1)
  }

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

  return <AuthContext.Provider value={{ user, sessionStatus, retrySession, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const session = useContext(AuthContext)
  if (!session) throw new Error('Falta AuthProvider')
  return session
}
