import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiClient } from '../../lib/apiClient'
import type { Brand, Category, Genero, Product } from '../../types'
import ProductCard from '../../components/public/ProductCard'

export default function Catalogo() {
  const [searchParams, setSearchParams] = useSearchParams()

  const [productos, setProductos] = useState<Product[]>([])
  const [categorias, setCategorias] = useState<Category[]>([])
  const [marcas, setMarcas] = useState<Brand[]>([])
  const [cargando, setCargando] = useState(true)
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false)

  // Filtros que se comparten por URL (llegan desde el navbar/enlaces externos)
  const genero = searchParams.get('genero') as Genero | null
  const subcategoria = searchParams.get('subcategoria')
  const marcaNombre = searchParams.get('marca')
  const sale = searchParams.get('sale') === 'true'
  const query = searchParams.get('q') ?? ''

  // Refinamientos locales (no viajan por URL, igual que en el catálogo original)
  const [precioMin, setPrecioMin] = useState('')
  const [precioMax, setPrecioMax] = useState('')
  const [soloStock, setSoloStock] = useState(true)
  const [soloDestacados, setSoloDestacados] = useState(false)

  useEffect(() => {
    setCargando(true)
    Promise.all([
      apiClient.get<Product[]>('/api/public/productos'),
      apiClient.get<Category[]>('/api/public/categorias'),
      apiClient.get<Brand[]>('/api/public/marcas'),
    ])
      .then(([prodRes, catRes, marcaRes]) => {
        setProductos((prodRes.data as Product[]) ?? [])
        setCategorias((catRes.data as Category[]) ?? [])
        setMarcas((marcaRes.data as Brand[]) ?? [])
      })
      .catch(() => {
        setProductos([])
        setCategorias([])
        setMarcas([])
      })
      .finally(() => setCargando(false))
  }, [])

  const categoriaSeleccionada = useMemo(
    () => (subcategoria ? categorias.find((c) => c.nombre.toLowerCase() === subcategoria.toLowerCase()) : undefined),
    [categorias, subcategoria],
  )
  const marcaSeleccionada = useMemo(
    () => (marcaNombre ? marcas.find((m) => m.nombre === marcaNombre) : undefined),
    [marcas, marcaNombre],
  )

  const productosFiltrados = useMemo(() => {
    let resultado = [...productos]

    if (genero) resultado = resultado.filter((p) => p.genero === genero)
    if (categoriaSeleccionada) resultado = resultado.filter((p) => p.category?.id === categoriaSeleccionada.id)
    if (marcaSeleccionada) resultado = resultado.filter((p) => p.brand?.id === marcaSeleccionada.id)
    if (sale) resultado = resultado.filter((p) => !!p.descuento && p.descuento > 0)
    if (query.trim()) {
      const texto = query.trim().toLowerCase()
      resultado = resultado.filter((p) => p.nombre.toLowerCase().includes(texto))
    }

    const min = parseFloat(precioMin)
    const max = parseFloat(precioMax)
    if (!isNaN(min)) resultado = resultado.filter((p) => p.precio >= min)
    if (!isNaN(max)) resultado = resultado.filter((p) => p.precio <= max)
    if (soloStock) resultado = resultado.filter((p) => p.stock > 0)
    if (soloDestacados) resultado = resultado.filter((p) => p.destacado === true)

    return resultado
  }, [productos, genero, categoriaSeleccionada, marcaSeleccionada, sale, query, precioMin, precioMax, soloStock, soloDestacados])

  const titulo = marcaSeleccionada?.nombre
    ? marcaSeleccionada.nombre
    : sale
      ? 'SALE - Ofertas'
      : genero && categoriaSeleccionada
        ? `${genero} - ${categoriaSeleccionada.nombre}`
        : genero
          ? `Catálogo ${genero}`
          : query
            ? `Resultados para "${query}"`
            : 'Catálogo de Productos'

  function actualizarParam(key: string, value: string | null) {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    setSearchParams(next)
  }

  function limpiarFiltros() {
    setSearchParams({})
    setPrecioMin('')
    setPrecioMax('')
    setSoloStock(true)
    setSoloDestacados(false)
  }

  return (
    <main className="mx-auto max-w-7xl px-4 py-8 lg:px-8">
      <nav className="mb-6 flex items-center gap-2 text-xs text-slate-500">
        <a href="/" className="hover:text-black">
          <i className="bi bi-house-door" /> Inicio
        </a>
        <span>/</span>
        <span className="text-slate-800">{titulo}</span>
      </nav>

      <div className="flex flex-col gap-8 lg:flex-row">
        <button
          onClick={() => setMobileFiltersOpen((v) => !v)}
          className="flex items-center justify-between rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm font-semibold uppercase tracking-wide lg:hidden"
        >
          <span>
            <i className="bi bi-sliders me-2" /> Filtros
          </span>
          <i className={`bi bi-chevron-down transition ${mobileFiltersOpen ? 'rotate-180' : ''}`} />
        </button>

        <aside className={`w-full shrink-0 space-y-6 lg:block lg:w-64 ${mobileFiltersOpen ? 'block' : 'hidden'}`}>
          <div className="rounded-xl border border-slate-200 bg-white p-5">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-xs font-black uppercase tracking-widest">Filtros</h3>
              <button onClick={limpiarFiltros} className="text-xs font-semibold text-brand-danger hover:underline">
                Limpiar
              </button>
            </div>

            <div className="mb-5">
              <label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500">Género</label>
              <select
                value={genero ?? ''}
                onChange={(e) => actualizarParam('genero', e.target.value || null)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-black focus:outline-none"
              >
                <option value="">Todos</option>
                <option value="HOMBRE">Hombre</option>
                <option value="MUJER">Mujer</option>
              </select>
            </div>

            <div className="mb-5">
              <label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500">Categoría</label>
              <select
                value={categoriaSeleccionada?.nombre ?? ''}
                onChange={(e) => actualizarParam('subcategoria', e.target.value || null)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-black focus:outline-none"
              >
                <option value="">Todas</option>
                {categorias.map((c) => (
                  <option key={c.id} value={c.nombre}>
                    {c.nombre}
                  </option>
                ))}
              </select>
            </div>

            <div className="mb-5">
              <label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500">Marca</label>
              <select
                value={marcaSeleccionada?.nombre ?? ''}
                onChange={(e) => actualizarParam('marca', e.target.value || null)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-black focus:outline-none"
              >
                <option value="">Todas</option>
                {marcas.map((m) => (
                  <option key={m.id} value={m.nombre}>
                    {m.nombre}
                  </option>
                ))}
              </select>
            </div>

            <div className="mb-5">
              <label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500">Rango de precio</label>
              <div className="flex gap-2">
                <input
                  type="number"
                  placeholder="Mín"
                  value={precioMin}
                  onChange={(e) => setPrecioMin(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-black focus:outline-none"
                />
                <input
                  type="number"
                  placeholder="Máx"
                  value={precioMax}
                  onChange={(e) => setPrecioMax(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-black focus:outline-none"
                />
              </div>
            </div>

            <div className="space-y-3 border-t border-slate-100 pt-4">
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <input type="checkbox" checked={sale} onChange={(e) => actualizarParam('sale', e.target.checked ? 'true' : null)} />
                Solo en oferta
              </label>
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <input type="checkbox" checked={soloStock} onChange={(e) => setSoloStock(e.target.checked)} />
                Con stock disponible
              </label>
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <input type="checkbox" checked={soloDestacados} onChange={(e) => setSoloDestacados(e.target.checked)} />
                Solo destacados
              </label>
            </div>
          </div>
        </aside>

        <div className="flex-1">
          <div className="mb-6 flex flex-col justify-between gap-2 sm:flex-row sm:items-end">
            <div>
              <h1 className="text-2xl font-black tracking-tight">{titulo}</h1>
              <p className="mt-1 text-sm text-slate-500">
                {cargando ? 'Cargando productos...' : `${productosFiltrados.length} producto${productosFiltrados.length !== 1 ? 's' : ''} encontrado${productosFiltrados.length !== 1 ? 's' : ''}`}
              </p>
            </div>
          </div>

          {cargando ? (
            <div className="flex min-h-[40vh] items-center justify-center">
              <i className="bi bi-arrow-repeat animate-spin text-3xl text-slate-300" />
            </div>
          ) : productosFiltrados.length === 0 ? (
            <div className="flex min-h-[40vh] flex-col items-center justify-center text-center">
              <i className="bi bi-box-seam text-5xl text-slate-300" />
              <h4 className="mt-4 text-lg font-bold">No se encontraron productos</h4>
              <p className="mt-1 text-sm text-slate-500">Intenta ajustar los filtros de búsqueda</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 xl:grid-cols-4">
              {productosFiltrados.map((p) => (
                <ProductCard key={p.id} producto={p} />
              ))}
            </div>
          )}
        </div>
      </div>
    </main>
  )
}
