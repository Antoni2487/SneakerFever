import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../../lib/apiClient'
import type { Brand } from '../../types'

const GENERO_MENU = [
  { label: 'Hombre', key: 'hombre' },
  { label: 'Mujer', key: 'mujer' },
] as const

const SUBCATEGORIAS = ['Zapatillas', 'Ropa', 'Accesorios']

export default function Navbar() {
  const navigate = useNavigate()
  const [logoUrl, setLogoUrl] = useState<string | null>(null)
  const [marcas, setMarcas] = useState<Brand[]>([])
  const [mobileOpen, setMobileOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [mobileSection, setMobileSection] = useState<string | null>(null)

  useEffect(() => {
    apiClient
      .get<{ url: string | null }>('/personalizacion/api/public/logo')
      .then((res) => setLogoUrl((res.url as string) || null))
      .catch(() => setLogoUrl(null))

    apiClient
      .get<Brand[]>('/api/public/marcas')
      .then((res) => setMarcas((res.data as Brand[]) ?? []))
      .catch(() => setMarcas([]))
  }, [])

  function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!query.trim()) return
    navigate(`/catalogo?q=${encodeURIComponent(query.trim())}`)
    setSearchOpen(false)
  }

  return (
    <>
      <nav className="sticky top-0 z-40 border-b border-black/10 bg-white">
        <div className="mx-auto flex max-w-7xl items-center gap-6 px-4 py-3 lg:px-8">
          <Link to="/" className="shrink-0">
            {logoUrl ? (
              <img src={logoUrl} alt="Sneakers Fever" className="h-10 w-auto object-contain" />
            ) : (
              <span className="text-xl font-black tracking-tight">SNEAKERS FEVER</span>
            )}
          </Link>

          <div className="hidden flex-1 items-center gap-6 lg:flex">
            {GENERO_MENU.map((genero) => (
              <div key={genero.key} className="group relative">
                <Link
                  to={`/catalogo?genero=${genero.key.toUpperCase()}`}
                  className="flex items-center gap-1 py-2 text-sm font-semibold uppercase tracking-wide text-slate-800 hover:text-black"
                >
                  {genero.label} <i className="bi bi-chevron-down text-xs" />
                </Link>
                <div className="invisible absolute left-0 top-full z-10 min-w-[180px] rounded-lg border border-black/10 bg-white py-2 opacity-0 shadow-lg transition group-hover:visible group-hover:opacity-100">
                  {SUBCATEGORIAS.map((sub) => (
                    <Link
                      key={sub}
                      to={`/catalogo?genero=${genero.key.toUpperCase()}&subcategoria=${sub}`}
                      className="block px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
                    >
                      {sub}
                    </Link>
                  ))}
                </div>
              </div>
            ))}

            <div className="group relative">
              <Link
                to="/catalogo"
                className="flex items-center gap-1 py-2 text-sm font-semibold uppercase tracking-wide text-slate-800 hover:text-black"
              >
                Marcas <i className="bi bi-chevron-down text-xs" />
              </Link>
              {marcas.length > 0 && (
                <div className="invisible absolute left-0 top-full z-10 flex min-w-[280px] flex-wrap gap-4 rounded-lg border border-black/10 bg-white p-4 opacity-0 shadow-lg transition group-hover:visible group-hover:opacity-100">
                  {marcas.map((m) => (
                    <Link key={m.id} to={`/catalogo?marca=${encodeURIComponent(m.nombre)}`} title={m.nombre} className="grayscale transition hover:grayscale-0">
                      <img src={m.imagen ?? undefined} alt={m.nombre} className="h-8 w-auto object-contain" />
                    </Link>
                  ))}
                </div>
              )}
            </div>

            <Link to="/catalogo?sale=true" className="py-2 text-sm font-semibold uppercase tracking-wide text-brand-danger hover:text-red-700">
              Ofertas
            </Link>
          </div>

          <div className="ml-auto flex items-center gap-4">
            <div className="relative hidden sm:block">
              <button onClick={() => setSearchOpen((v) => !v)} aria-label="Buscar" className="text-lg text-slate-700 hover:text-black">
                <i className="bi bi-search" />
              </button>
              {searchOpen && (
                <form onSubmit={handleSearchSubmit} className="absolute right-0 top-full mt-2 flex w-64 overflow-hidden rounded-lg border border-black/10 bg-white shadow-lg">
                  <input
                    autoFocus
                    type="search"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Buscar productos..."
                    className="w-full px-3 py-2 text-sm focus:outline-none"
                  />
                  <button type="submit" className="px-3 text-slate-500 hover:text-black">
                    <i className="bi bi-search" />
                  </button>
                </form>
              )}
            </div>

            <Link to="/carrito" className="relative text-lg text-slate-700 hover:text-black" aria-label="Carrito">
              <i className="bi bi-bag" />
              <span className="absolute -right-2 -top-2 flex h-4 w-4 items-center justify-center rounded-full bg-black text-[10px] font-bold text-white">0</span>
            </Link>

            <button onClick={() => setMobileOpen(true)} className="text-2xl text-slate-800 lg:hidden" aria-label="Abrir menú">
              <i className="bi bi-list" />
            </button>
          </div>
        </div>
      </nav>

      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="absolute inset-0 bg-black/50" onClick={() => setMobileOpen(false)} />
          <div className="absolute right-0 top-0 h-full w-80 max-w-[85vw] overflow-y-auto bg-white p-5 shadow-xl">
            <div className="mb-4 flex items-center justify-between">
              <span className="text-lg font-bold">Menú</span>
              <button onClick={() => setMobileOpen(false)} aria-label="Cerrar menú" className="text-2xl">
                <i className="bi bi-x-lg" />
              </button>
            </div>

            <form onSubmit={handleSearchSubmit} className="mb-4 flex overflow-hidden rounded-lg border border-slate-300">
              <input
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="¿Qué estás buscando?"
                className="w-full px-3 py-2 text-sm focus:outline-none"
              />
              <button type="submit" className="px-3 text-slate-500">
                <i className="bi bi-search" />
              </button>
            </form>

            {GENERO_MENU.map((genero) => (
              <div key={genero.key} className="border-b border-slate-100 py-2">
                <button
                  onClick={() => setMobileSection((s) => (s === genero.key ? null : genero.key))}
                  className="flex w-full items-center justify-between py-2 text-sm font-semibold uppercase"
                >
                  {genero.label}
                  <i className={`bi bi-chevron-down transition ${mobileSection === genero.key ? 'rotate-180' : ''}`} />
                </button>
                {mobileSection === genero.key && (
                  <div className="pb-2 pl-3">
                    {SUBCATEGORIAS.map((sub) => (
                      <Link
                        key={sub}
                        to={`/catalogo?genero=${genero.key.toUpperCase()}&subcategoria=${sub}`}
                        onClick={() => setMobileOpen(false)}
                        className="block py-1.5 text-sm text-slate-600"
                      >
                        {sub}
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            ))}

            <div className="border-b border-slate-100 py-2">
              <button
                onClick={() => setMobileSection((s) => (s === 'marcas' ? null : 'marcas'))}
                className="flex w-full items-center justify-between py-2 text-sm font-semibold uppercase"
              >
                Marcas
                <i className={`bi bi-chevron-down transition ${mobileSection === 'marcas' ? 'rotate-180' : ''}`} />
              </button>
              {mobileSection === 'marcas' && (
                <div className="flex flex-wrap gap-3 pb-2 pl-3">
                  {marcas.map((m) => (
                    <Link key={m.id} to={`/catalogo?marca=${encodeURIComponent(m.nombre)}`} onClick={() => setMobileOpen(false)} title={m.nombre}>
                      <img src={m.imagen ?? undefined} alt={m.nombre} className="h-7 w-auto object-contain" />
                    </Link>
                  ))}
                </div>
              )}
            </div>

            <Link to="/catalogo?sale=true" onClick={() => setMobileOpen(false)} className="block py-3 text-sm font-semibold uppercase text-brand-danger">
              Ofertas
            </Link>
          </div>
        </div>
      )}
    </>
  )
}
