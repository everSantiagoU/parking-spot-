import { Link } from 'react-router'
import { useAuth } from '../auth/AuthProvider'
import { panelForRole, ROUTES } from '../../app/router/routes'

export function HomePage() {
  const { user } = useAuth()
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-950 px-6 py-16 text-slate-100">
      <section className="w-full max-w-2xl rounded-2xl border border-slate-700 bg-slate-900 p-8 shadow-xl sm:p-12">
        <h1 className="text-4xl font-bold tracking-tight sm:text-5xl">Parking Spot</h1>
        <p className="mt-4 text-xl text-slate-200">Sistema de Gestión de Parqueaderos</p>
        <Link
          to={user ? panelForRole(user.role) : ROUTES.login}
          className="mt-8 inline-flex rounded-lg bg-teal-300 px-5 py-3 font-semibold text-slate-950 hover:bg-teal-200"
        >
          {user ? 'Ir a mi panel' : 'Iniciar sesión'}
        </Link>
      </section>
    </main>
  )
}
