import { ROUTES } from '../../app/router/routes'
import { ModuleCard, type ModuleCardProps } from '../../components/ui/ModuleCard'

const OPERATOR_MODULES: ModuleCardProps[] = [
  { title: 'Clasificar placa', description: 'Identifica si una placa es de carro o de moto.', to: ROUTES.classifyVehicle },
  { title: 'Carril de entrada', description: 'Registra el ingreso de un vehículo por su placa.', story: 'HU-10' },
  { title: 'Carril de salida', description: 'Valida el cobro del día y abre o mantiene cerrada la barrera.', story: 'HU-16' },
  { title: 'Caja', description: 'Registra el pago en efectivo del cobro de una placa.', story: 'HU-15' },
]

/** Panel base del rol operador. */
export function OperatorHomePage() {
  return (
    <section>
      <h1 className="text-3xl font-bold tracking-tight">Panel de operador</h1>
      <p className="mt-2 text-slate-300">Operación diaria de los carriles y la caja.</p>
      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        {OPERATOR_MODULES.map((item) => (
          <ModuleCard key={item.title} {...item} />
        ))}
      </div>
    </section>
  )
}
