import { Link, Navigate, Outlet, Route, Routes } from 'react-router'
import { AppLayout } from '../../components/layout/AppLayout'
import { AdminHomePage } from '../../features/administration/AdminHomePage'
import { AuthProvider, useAuth, type Role } from '../../features/auth/AuthProvider'
import { LoginPage } from '../../features/auth/LoginPage'
import { HomePage } from '../../features/home/HomePage'
import { OperatorHomePage } from '../../features/parking/OperatorHomePage'
import { ClassifyVehiclePage } from '../../features/vehicles/ClassifyVehiclePage'
import { panelForRole, ROUTES } from './routes'

function RequireRole({ role }: { role: Role }) {
  const { user, loading } = useAuth()
  if (loading) return <p role="status">Consultando sesión…</p>
  if (!user) return <Navigate to={ROUTES.login} replace />
  if (user.role !== role) {
    return (
      <main className="min-h-screen bg-slate-950 p-8 text-slate-100">
        <h1 role="alert" className="text-2xl font-bold">Acceso denegado.</h1>
        <Link to={panelForRole(user.role)} className="mt-4 inline-block text-teal-300">Volver a mi panel</Link>
      </main>
    )
  }
  return <Outlet />
}

export function AppRouter() {
  return (
    <AuthProvider>
      <Routes>
        <Route path={ROUTES.home} element={<HomePage />} />
        <Route path={ROUTES.login} element={<LoginPage />} />
        <Route element={<RequireRole role="OPERADOR" />}>
          <Route element={<AppLayout />}>
            <Route path={ROUTES.operator} element={<OperatorHomePage />} />
            <Route path={ROUTES.classifyVehicle} element={<ClassifyVehiclePage />} />
          </Route>
        </Route>
        <Route element={<RequireRole role="ADMIN" />}>
          <Route element={<AppLayout />}>
            <Route path={ROUTES.admin} element={<AdminHomePage />} />
          </Route>
        </Route>
        <Route path="/classify" element={<Navigate to={ROUTES.classifyVehicle} replace />} />
      </Routes>
    </AuthProvider>
  )
}
