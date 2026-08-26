import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../../lib/apiClient'
import type { PersonalizacionSlot, Product, ProductosDestacados } from '../../types'
import HeroCarousel from '../../components/public/HeroCarousel'
import ProductCard from '../../components/public/ProductCard'

const FALLBACK_SLIDES = [1, 2, 3, 4, 5, 6].map((n) => ({ key: n, imagenUrl: `/web/images/${n}.png` }))

const TABS = [
  { key: 'zapatillas', label: 'Zapatillas' },
  { key: 'ropa', label: 'Ropa' },
  { key: 'accesorios', label: 'Accesorios' },
] as const
type TabKey = (typeof TABS)[number]['key']

function precioFinal(precio: number, descuento: number | null) {
  if (!descuento) return precio
  return precio * (1 - descuento / 100)
}

export default function Landing() {
  const [slides, setSlides] = useState<PersonalizacionSlot[]>([])
  const [destacados, setDestacados] = useState<ProductosDestacados>({ zapatillas: [], ropa: [], accesorios: [] })
  const [masVendidas, setMasVendidas] = useState<Product[]>([])
  const [tab, setTab] = useState<TabKey>('zapatillas')
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    apiClient
      .get<PersonalizacionSlot[]>('/personalizacion/api/public/slides')
      .then((res) => setSlides((res.data as PersonalizacionSlot[]) ?? []))
      .catch(() => setSlides([]))

    apiClient
      .get<ProductosDestacados>('/api/public/productos/destacados')
      .then((res) => setDestacados((res.data as ProductosDestacados) ?? { zapatillas: [], ropa: [], accesorios: [] }))
      .catch(() => {})

    apiClient
      .get<Product[]>('/api/public/productos/mas-vendidos')
      .then((res) => setMasVendidas((res.data as Product[]) ?? []))
      .catch(() => {})
  }, [])

  const heroSlides =
    slides.length >= 2
      ? slides.map((s) => ({ key: s.id, imagenUrl: s.imagenUrl as string, marcaId: s.marca?.id }))
      : FALLBACK_SLIDES

  const productosDelTab = destacados[tab].slice(0, 8)
  const hayDestacados = destacados.zapatillas.length + destacados.ropa.length + destacados.accesorios.length > 0

  function scrollZapatillas(direction: 1 | -1) {
    scrollRef.current?.scrollBy({ left: direction * 280, behavior: 'smooth' })
  }

  return (
    <main>
      <HeroCarousel slides={heroSlides} />

      <section className="mx-auto max-w-7xl px-4 py-12 text-center lg:px-8">
        <h2 className="mb-8 text-2xl font-black tracking-widest">NOVEDADES</h2>
        <div className="flex flex-wrap items-center justify-center gap-8">
          <img src="/web/images/adidas.gif" alt="Novedades Adidas" className="h-16 w-auto object-contain" />
          <img src="/web/images/nike.gif" alt="Novedades Nike" className="h-16 w-auto object-contain" />
          <img src="/web/images/nb.gif" alt="Novedades New Balance" className="h-16 w-auto object-contain" />
          <img src="/web/images/jordan.gif" alt="Novedades Jordan" className="h-16 w-auto object-contain" />
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-8 lg:px-8">
        <h2 className="mb-8 text-center text-2xl font-black tracking-widest">DESTACADOS</h2>

        <div className="mb-8 flex justify-center gap-2">
          {TABS.map((t) => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`rounded-full px-5 py-2 text-sm font-semibold uppercase tracking-wide transition ${
                tab === t.key ? 'bg-black text-white' : 'bg-white text-slate-600 hover:bg-slate-100'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {hayDestacados ? (
          productosDelTab.length > 0 ? (
            <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 lg:grid-cols-4">
              {productosDelTab.map((p) => (
                <ProductCard key={p.id} producto={p} />
              ))}
            </div>
          ) : (
            <p className="text-center text-slate-400">No hay productos destacados en esta categoría</p>
          )
        ) : (
          <p className="text-center text-slate-400">No hay productos destacados en este momento</p>
        )}

        <div className="mt-10 flex justify-center">
          <Link
            to="/catalogo"
            className="flex items-center gap-2 rounded-full border-2 border-black px-8 py-3 text-sm font-bold uppercase tracking-widest transition hover:bg-black hover:text-white"
          >
            Ver más <i className="bi bi-arrow-right" />
          </Link>
        </div>
      </section>

      <section className="relative overflow-hidden bg-gradient-to-br from-black to-[#1a1a1a] py-14 text-white">
        <div className="mx-auto max-w-7xl px-4 lg:px-8">
          <h2 className="mb-1 text-center text-2xl font-black tracking-widest">ZAPATILLAS DEL MES</h2>
          <p className="mb-8 text-center text-sm text-slate-300">Las más vendidas de la temporada</p>

          {masVendidas.length > 0 ? (
            <div className="relative">
              <div ref={scrollRef} className="flex snap-x snap-mandatory gap-5 overflow-x-auto pb-4 [scrollbar-width:none]">
                {masVendidas.slice(0, 5).map((z, i) => {
                  const tieneDescuento = !!z.descuento && z.descuento > 0
                  return (
                    <div key={z.id} className="relative w-64 shrink-0 snap-start rounded-xl bg-white p-4 text-[#222]">
                      <span className="absolute left-3 top-3 z-10 flex h-8 w-8 items-center justify-center rounded-full bg-black text-sm font-bold text-white">
                        #{i + 1}
                      </span>
                      {tieneDescuento && (
                        <span className="absolute right-3 top-3 z-10 rounded-full bg-brand-danger px-2 py-1 text-xs font-bold text-white">
                          -{Math.round(z.descuento!)}%
                        </span>
                      )}
                      <div className="flex h-40 items-center justify-center">
                        {z.imagen ? (
                          <img src={z.imagen} alt={z.nombre} className="h-full w-full object-contain" />
                        ) : (
                          <i className="bi bi-image text-4xl text-slate-300" />
                        )}
                      </div>
                      <p className="mt-3 text-xs uppercase text-slate-400">{z.brand?.nombre}</p>
                      <h3 className="truncate text-sm font-bold">{z.nombre}</h3>
                      <div className="mt-1 flex items-baseline gap-2">
                        {tieneDescuento && <span className="text-xs text-slate-400 line-through">S/. {z.precio.toFixed(2)}</span>}
                        <span className="text-sm font-bold">S/. {precioFinal(z.precio, z.descuento).toFixed(2)}</span>
                      </div>
                      <Link
                        to={`/producto/${z.id}`}
                        className="mt-3 flex items-center justify-center gap-2 rounded-full bg-black py-2 text-xs font-bold uppercase text-white transition hover:bg-slate-800"
                      >
                        Ver producto <i className="bi bi-arrow-right" />
                      </Link>
                    </div>
                  )
                })}
              </div>
              <button
                onClick={() => scrollZapatillas(-1)}
                aria-label="Anterior"
                className="absolute -left-3 top-1/2 hidden h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-white text-black shadow-lg sm:flex"
              >
                <i className="bi bi-chevron-left" />
              </button>
              <button
                onClick={() => scrollZapatillas(1)}
                aria-label="Siguiente"
                className="absolute -right-3 top-1/2 hidden h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-white text-black shadow-lg sm:flex"
              >
                <i className="bi bi-chevron-right" />
              </button>
            </div>
          ) : (
            <p className="text-center text-slate-300">Aún no hay suficientes ventas para mostrar un ranking</p>
          )}
        </div>
      </section>
    </main>
  )
}
