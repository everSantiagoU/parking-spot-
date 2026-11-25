import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router'
import { useAuth } from '../../features/auth/AuthProvider'
import { panelForRole, ROUTES } from '../../app/router/routes'

function navClass({ isActive }: { isActive: boolean }) {
  return isActive
    ? 'rounded-lg bg-teal-300 px-3 py-2 text-sm font-semibold text-slate-950'
    : 'rounded-lg px-3 py-2 text-sm font-medium text-slate-300 hover:bg-slate-800 hover:text-slate-100'
}

/** Estructura común de las pantallas de operador y administrador. */
export function AppLayout() {
  const { user, logout } = useAuth()
  const [closing, setClosing] = useState(false)
  const [error, setError] = useState('')

  async function handleLogout() {
    setClosing(true)
    setError('')
    try {
      await logout()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo cerrar sesión.')
    } finally {
      setClosing(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-4 px-6 py-4">
          <Link className="text-lg font-bold tracking-tight" to={user ? panelForRole(user.role) : ROUTES.home}>Parking Spot</Link>
          <nav aria-label="Navegación principal" className="flex gap-2">
            <NavLink to={ROUTES.home} end className={navClass}>Inicio</NavLink>
            {user && (
              <NavLink to={panelForRole(user.role)} className={navClass}>
                {user.role === 'ADMIN' ? 'Administración' : 'Operador'}
              </NavLink>
            )}
            <button type="button" disabled={closing} onClick={handleLogout}
              className="rounded-lg px-3 py-2 text-sm text-teal-300 hover:bg-slate-800 disabled:opacity-60">
              {closing ? 'Cerrando sesión…' : 'Cerrar sesión'}
            </button>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-6 py-10">
        {error && <p role="alert" className="mb-4 text-red-200">{error}</p>}
        <Outlet />
      </main>
    </div>
  )
}
