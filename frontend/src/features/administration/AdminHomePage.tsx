import { ModuleCard, type ModuleCardProps } from '../../components/ui/ModuleCard'

const ADMIN_MODULES: ModuleCardProps[] = [
  { title: 'Tarifas y franjas', description: 'Configura tarifas por tipo de vehículo y horarios de franja.', story: 'HU-12' },
  { title: 'Cupos', description: 'Define la capacidad por tipo de vehículo en la sede.', story: 'HU-12' },
  { title: 'Eventos y cobros', description: 'Consulta ingresos, salidas y cobros con filtros.', story: 'HU-17' },
]

/** Panel base del rol administrador. */
export function AdminHomePage() {
  return (
    <section>
      <h1 className="text-3xl font-bold tracking-tight">Panel de administración</h1>
      <p className="mt-2 text-slate-300">Configuración del parqueadero y consultas.</p>
      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        {ADMIN_MODULES.map((item) => (
          <ModuleCard key={item.title} {...item} />
        ))}
      </div>
    </section>
  )
}
