import { Link } from 'react-router'

export type ModuleCardProps = {
  title: string
  description: string
  /** Ruta del módulo. Si no existe, la tarjeta se muestra como pendiente. */
  to?: string
  /** Historia de usuario que implementará el módulo, por ejemplo "HU-16". */
  story?: string
}

/** Tarjeta reutilizable para los accesos de los paneles de operador y administrador. */
export function ModuleCard({ title, description, to, story }: ModuleCardProps) {
  const content = (
    <>
      <div className="flex items-start justify-between gap-3">
        <h3 className="text-lg font-semibold">{title}</h3>
        <span className={`shrink-0 rounded-full border px-2 py-0.5 text-xs ${to
          ? 'border-teal-700 text-teal-300'
          : 'border-slate-600 text-slate-300'}`}>
          {to ? 'Disponible' : `Pendiente${story ? ` · ${story}` : ''}`}
        </span>
      </div>
      <p className="mt-2 text-sm text-slate-300">{description}</p>
    </>
  )

  if (!to) {
    return (
      <article className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
        {content}
      </article>
    )
  }

  return (
    <Link
      to={to}
      className="block rounded-xl border border-slate-700 bg-slate-900 p-5 hover:border-teal-300 focus-visible:outline-2 focus-visible:outline-teal-300"
    >
      {content}
    </Link>
  )
}
