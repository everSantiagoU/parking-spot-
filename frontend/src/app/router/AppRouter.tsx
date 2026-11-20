import { Navigate, Route, Routes } from 'react-router'
import { HomePage } from '../../features/home/HomePage'
import { ClassifyVehiclePage } from '../../features/vehicles/ClassifyVehiclePage'

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/vehicles/classify" element={<ClassifyVehiclePage />} />
      <Route path="/classify" element={<Navigate to="/vehicles/classify" replace />} />
    </Routes>
  )
}
