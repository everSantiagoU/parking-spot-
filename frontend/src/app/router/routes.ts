import type { Role } from '../../features/auth/AuthProvider'

/** Rutas de la aplicación en un solo lugar para no repetir cadenas en los componentes. */
export const ROUTES = {
  login: '/login',
  home: '/',
  operator: '/operator',
  admin: '/admin',
  classifyVehicle: '/vehicles/classify',
  charges: '/operator/charges',
  cashier: '/operator/cashier',
  pass: '/operator/pass',
  entry: '/operator/entry',
  exit: '/operator/exit',
  parkingConfiguration: '/admin/configuration',
} as const

export function panelForRole(role: Role) {
  return role === 'ADMIN' ? ROUTES.admin : ROUTES.operator
}
