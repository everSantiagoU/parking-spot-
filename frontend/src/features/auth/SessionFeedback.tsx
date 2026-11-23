import { useAuth } from './AuthProvider'

export function SessionFeedback() {
  const { sessionStatus, retrySession } = useAuth()

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-950 p-6 text-slate-100">
      <section className="w-full max-w-md rounded-2xl border border-slate-700 bg-slate-900 p-8">
        {sessionStatus === 'checking' && <p role="status">Consultando sesión…</p>}
        {sessionStatus === 'forbidden' && <p role="alert">Acceso denegado.</p>}
        {sessionStatus === 'unavailable' && (
          <>
            <p role="alert">No se pudo consultar la sesión. Comprueba la conexión y vuelve a intentar.</p>
            <button type="button" onClick={retrySession}
              className="mt-4 rounded-lg bg-teal-300 px-4 py-2 font-semibold text-slate-950 hover:bg-teal-200">
              Reintentar
            </button>
          </>
        )}
      </section>
    </main>
  )
}
