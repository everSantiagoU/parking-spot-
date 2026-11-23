import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router'
import { panelForRole } from '../../app/router/routes'
import { ApiError } from '../../services/httpClient'
import { useAuth } from './AuthProvider'

export function LoginPage() {
  const { user, loading, error: sessionError, login } = useAuth()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  if (user) return <Navigate to={panelForRole(user.role)} replace />

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const data = new FormData(form)
    setError('')
    setSubmitting(true)
    try {
      await login(String(data.get('username')), String(data.get('password')))
    } catch (cause) {
      setError(cause instanceof ApiError && cause.status === 401
        ? 'Credenciales inválidas.'
        : cause instanceof ApiError && cause.status === 403
          ? cause.message
          : 'No se pudo iniciar sesión. Intenta nuevamente.')
    } finally {
      form.reset()
      setSubmitting(false)
    }
  }

  const inputStyle = 'mt-2 block w-full rounded-lg border border-slate-500 bg-slate-950 px-4 py-3'
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-950 p-6 text-slate-100">
      <section className="w-full max-w-md rounded-2xl border border-slate-700 bg-slate-900 p-8">
        <p className="font-semibold text-teal-300">Parking Spot</p>
        <h1 className="mt-3 text-3xl font-bold">Iniciar sesión</h1>
        <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
          <label className="block">
            Usuario
            <input className={inputStyle} name="username" autoComplete="username" required />
          </label>
          <label className="block">
            Contraseña
            <input className={inputStyle} name="password" type="password" autoComplete="current-password" required />
          </label>
          <button className="w-full rounded-lg bg-teal-300 px-4 py-3 font-semibold text-slate-950 disabled:opacity-60"
            disabled={loading || submitting}>
            {loading ? 'Consultando sesión…' : submitting ? 'Iniciando sesión…' : 'Entrar'}
          </button>
        </form>
        {(error || sessionError) && <p role="alert" className="mt-4 text-red-200">{error || sessionError}</p>}
      </section>
    </main>
  )
}
