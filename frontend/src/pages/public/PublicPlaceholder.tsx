import { Link, useLocation } from 'react-router-dom'

export default function PublicPlaceholder() {
  const location = useLocation()

  return (
    <main className="mx-auto flex min-h-[60vh] max-w-2xl flex-col items-center justify-center px-4 text-center">
      <i className="bi bi-hourglass-split text-5xl text-slate-300" />
      <h1 className="mt-4 text-2xl font-black tracking-wide">Próximamente</h1>
      <p className="mt-2 text-slate-500">
        La sección <span className="font-mono">{location.pathname}</span> todavía no está disponible.
      </p>
      <Link to="/" className="mt-6 rounded-full bg-black px-6 py-2.5 text-sm font-bold uppercase text-white transition hover:bg-slate-800">
        Volver al inicio
      </Link>
    </main>
  )
}
