import { useLocation } from 'react-router-dom'

export default function Placeholder() {
  const location = useLocation()

  return (
    <div className="p-4 md:p-6">
      <div className="rounded-xl border border-dashed border-slate-300 bg-white p-10 text-center shadow-sm">
        <p className="text-lg font-semibold text-slate-700">Próximamente</p>
        <p className="mt-1 text-sm text-slate-500">
          La sección <span className="font-mono">{location.pathname}</span> todavía no fue migrada a React.
        </p>
      </div>
    </div>
  )
}
