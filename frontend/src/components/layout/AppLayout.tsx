import { NavLink, Outlet } from 'react-router'
import { ROUTES } from '../../app/router/routes'

const NAV_ITEMS = [
  { to: ROUTES.home, label: 'Inicio', end: true },
  { to: ROUTES.operator, label: 'Operador', end: false },
  { to: ROUTES.admin, label: 'Administración', end: false },
]

function navClass({ isActive }: { isActive: boolean }) {
  return isActive
    ? 'rounded-lg bg-teal-300 px-3 py-2 text-sm font-semibold text-slate-950'
    : 'rounded-lg px-3 py-2 text-sm font-medium text-slate-300 hover:bg-slate-800 hover:text-slate-100'
}

/** Estructura común de las pantallas de operador y administrador. */
export function AppLayout() {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-4 px-6 py-4">
          <span className="text-lg font-bold tracking-tight">Parking Spot</span>
          <nav aria-label="Navegación principal" className="flex gap-2">
            {NAV_ITEMS.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end} className={navClass}>
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-6 py-10">
        <Outlet />
      </main>
    </div>
  )
}
