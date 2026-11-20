import { Link } from 'react-router'

export function HomePage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-950 px-6 py-16 text-slate-100">
      <section className="w-full max-w-2xl rounded-2xl border border-slate-700 bg-slate-900 p-8 shadow-xl sm:p-12">
        <p className="mb-4 text-sm font-semibold uppercase tracking-widest text-teal-300">
          Configuración inicial completada.
        </p>
        <h1 className="text-4xl font-bold tracking-tight sm:text-5xl">Parking Spot</h1>
        <h2 className="mt-4 text-xl font-medium text-slate-200 sm:text-2xl">
          Sistema de Gestión de Parqueaderos
        </h2>
        <p className="mt-6 max-w-prose text-base leading-relaxed text-slate-300">
          Frontend inicial desarrollado con React, TypeScript, Vite y Tailwind CSS.
        </p>
        <Link
          to="/vehicles/classify"
          className="mt-8 inline-flex rounded-lg bg-teal-300 px-5 py-3 font-semibold text-slate-950 hover:bg-teal-200 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-300"
        >
          Clasificar una placa
        </Link>
      </section>
    </main>
  )
}
