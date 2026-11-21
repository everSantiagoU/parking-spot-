import { Navigate, Route, Routes } from 'react-router'
import { AppLayout } from '../../components/layout/AppLayout'
import { AdminHomePage } from '../../features/administration/AdminHomePage'
import { HomePage } from '../../features/home/HomePage'
import { OperatorHomePage } from '../../features/parking/OperatorHomePage'
import { ClassifyVehiclePage } from '../../features/vehicles/ClassifyVehiclePage'
import { ROUTES } from './routes'

export function AppRouter() {
  return (
    <Routes>
      <Route path={ROUTES.home} element={<HomePage />} />
      <Route element={<AppLayout />}>
        <Route path={ROUTES.operator} element={<OperatorHomePage />} />
        <Route path={ROUTES.admin} element={<AdminHomePage />} />
      </Route>
      <Route path={ROUTES.classifyVehicle} element={<ClassifyVehiclePage />} />
      <Route path="/classify" element={<Navigate to={ROUTES.classifyVehicle} replace />} />
    </Routes>
  )
}
