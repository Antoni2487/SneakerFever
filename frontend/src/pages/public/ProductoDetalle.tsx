import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Swal from 'sweetalert2'
import { apiClient, ApiError } from '../../lib/apiClient'
import { useCart } from '../../context/CartContext'
import type { Product } from '../../types'

const TALLAS = ['US 7', 'US 8', 'US 9', 'US 10', 'US 11']

function precioFinal(precio: number, descuento: number | null) {
  if (!descuento) return precio
  return precio * (1 - descuento / 100)
}

function notify(title: string, icon: 'success' | 'error') {
  Swal.fire({
    title,
    icon,
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 2500,
    timerProgressBar: true,
  })
}

export default function ProductoDetalle() {
  const { id } = useParams<{ id: string }>()
  const { agregar } = useCart()
  const [producto, setProducto] = useState<Product | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [cargando, setCargando] = useState(true)
  const [imagenActiva, setImagenActiva] = useState<string | null>(null)
  const [tallaActiva, setTallaActiva] = useState<string | null>(null)
  const [agregando, setAgregando] = useState(false)

  async function handleAgregar() {
    setAgregando(true)
    try {
      await agregar(Number(id))
      notify('Agregado al carrito', 'success')
    } catch (err) {
      notify(err instanceof ApiError ? err.message : 'No se pudo agregar', 'error')
    } finally {
      setAgregando(false)
    }
  }

  useEffect(() => {
    setCargando(true)
    setNotFound(false)
    setTallaActiva(null)
    apiClient
      .get<Product>(`/api/public/productos/${id}`)
      .then((res) => {
        const data = res.data as Product
        setProducto(data)
        setImagenActiva(data.imagen)
      })
      .catch((err) => {
        if (err instanceof ApiError && err.status === 404) setNotFound(true)
        setProducto(null)
      })
      .finally(() => setCargando(false))
  }, [id])

  if (cargando) {
    return (
      <main className="flex min-h-[60vh] items-center justify-center">
        <i className="bi bi-arrow-repeat animate-spin text-3xl text-slate-300" />
      </main>
    )
  }

  if (notFound || !producto) {
    return (
      <main className="mx-auto flex min-h-[60vh] max-w-2xl flex-col items-center justify-center px-4 text-center">
        <i className="bi bi-exclamation-circle text-5xl text-slate-300" />
        <h1 className="mt-4 text-2xl font-black tracking-wide">Producto no encontrado</h1>
        <Link to="/catalogo" className="mt-6 rounded-full bg-black px-6 py-2.5 text-sm font-bold uppercase text-white transition hover:bg-slate-800">
          Volver al catálogo
        </Link>
      </main>
    )
  }

  const tieneDescuento = !!producto.descuento && producto.descuento > 0
  const miniaturas = [producto.imagen, ...producto.imagenes].filter((img): img is string => !!img)
  const agotado = producto.stock === 0

  return (
    <main className="mx-auto max-w-7xl px-4 py-8 lg:px-8">
      <nav className="mb-6 flex items-center gap-2 text-xs text-slate-500">
        <Link to="/" className="hover:text-black">
          Inicio
        </Link>
        <span>/</span>
        <Link to="/catalogo" className="hover:text-black">
          Catálogo
        </Link>
        <span>/</span>
        <span className="text-slate-800">{producto.nombre}</span>
      </nav>

      <div className="grid grid-cols-1 gap-10 lg:grid-cols-[80px_1fr_1fr]">
        {miniaturas.length > 1 && (
          <div className="order-2 flex gap-2 lg:order-1 lg:flex-col">
            {miniaturas.map((img, i) => (
              <button
                key={i}
                onClick={() => setImagenActiva(img)}
                className={`aspect-square w-16 shrink-0 overflow-hidden rounded-lg border-2 bg-white lg:w-full ${
                  imagenActiva === img ? 'border-black' : 'border-transparent'
                }`}
              >
                <img src={img} alt="" className="h-full w-full object-cover" />
              </button>
            ))}
          </div>
        )}

        <div className={`order-1 flex min-h-[400px] items-center justify-center rounded-xl bg-[#f8f8f8] p-8 lg:order-2 ${miniaturas.length <= 1 ? 'lg:col-start-1' : ''}`}>
          {imagenActiva ? (
            <img src={imagenActiva} alt={producto.nombre} className="max-h-[420px] w-full object-contain" />
          ) : (
            <i className="bi bi-image text-6xl text-slate-300" />
          )}
        </div>

        <div className="order-3">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <span className="rounded-full bg-black px-3 py-1 text-xs font-bold uppercase text-white">{producto.brand?.nombre}</span>
            {tieneDescuento && <span className="rounded-full bg-brand-danger px-3 py-1 text-xs font-bold uppercase text-white">Oferta</span>}
          </div>

          <h1 className="text-2xl font-black uppercase tracking-tight">{producto.nombre}</h1>
          <p className="mt-1 text-sm text-slate-500">{producto.category?.nombre}</p>

          <div className="mt-5 flex items-baseline gap-3">
            <span className="text-3xl font-black">S/ {precioFinal(producto.precio, producto.descuento).toFixed(2)}</span>
            {tieneDescuento && <span className="text-lg text-slate-400 line-through">S/ {producto.precio.toFixed(2)}</span>}
          </div>

          <div className="mt-5 rounded-lg border border-slate-200 bg-slate-50 p-4 text-xs text-slate-500">
            <p>
              <i className="bi bi-truck me-2" /> Envío gratis a todo el Perú.
            </p>
            <p className="mt-1">
              <i className="bi bi-shield-check me-2" /> Garantía de tienda oficial.
            </p>
          </div>

          <p className="mb-2 mt-6 text-xs font-bold uppercase tracking-wide text-slate-500">Seleccionar talla</p>
          <div className="grid grid-cols-4 gap-2">
            {TALLAS.map((talla) => (
              <button
                key={talla}
                onClick={() => setTallaActiva(talla)}
                className={`rounded-lg border py-2 text-sm font-semibold transition ${
                  tallaActiva === talla ? 'border-black bg-black text-white' : 'border-slate-300 text-slate-700 hover:border-black'
                }`}
              >
                {talla}
              </button>
            ))}
          </div>

          <button
            onClick={handleAgregar}
            disabled={agotado || agregando}
            className="mt-6 flex w-full items-center justify-center gap-2 rounded-full bg-black py-3.5 text-sm font-bold uppercase tracking-wide text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {agotado ? (
              <>
                Agotado <i className="bi bi-x-circle" />
              </>
            ) : (
              <>
                Agregar al carrito <i className="bi bi-bag" />
              </>
            )}
          </button>

          {producto.descripcion && (
            <div className="mt-8 border-t border-slate-200 pt-6">
              <h3 className="mb-2 text-xs font-black uppercase tracking-widest">Descripción</h3>
              <p className="text-sm leading-relaxed text-slate-600">{producto.descripcion}</p>
            </div>
          )}
        </div>
      </div>
    </main>
  )
}
