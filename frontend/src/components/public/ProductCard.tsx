import { useState } from 'react'
import type { MouseEvent } from 'react'
import { Link } from 'react-router-dom'
import Swal from 'sweetalert2'
import { useCart } from '../../context/CartContext'
import { ApiError } from '../../lib/apiClient'
import type { Product } from '../../types'

function precioFinal(precio: number, descuento: number | null) {
  if (!descuento) return precio
  return precio * (1 - descuento / 100)
}

export default function ProductCard({ producto }: { producto: Product }) {
  const { agregar } = useCart()
  const [broken, setBroken] = useState(false)
  const [agregando, setAgregando] = useState(false)
  const tieneDescuento = !!producto.descuento && producto.descuento > 0
  const agotado = producto.stock === 0

  async function handleAgregar(event: MouseEvent) {
    event.preventDefault()
    event.stopPropagation()
    setAgregando(true)
    try {
      await agregar(producto.id)
      Swal.fire({
        title: 'Agregado al carrito',
        icon: 'success',
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 2000,
        timerProgressBar: true,
      })
    } catch (err) {
      Swal.fire({
        title: err instanceof ApiError ? err.message : 'No se pudo agregar',
        icon: 'error',
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
      })
    } finally {
      setAgregando(false)
    }
  }

  return (
    <Link to={`/producto/${producto.id}`} className="group block">
      <div className="relative aspect-square overflow-hidden rounded-lg bg-white">
        {producto.imagen && !broken ? (
          <img
            src={producto.imagen}
            alt={producto.nombre}
            onError={() => setBroken(true)}
            className="h-full w-full object-contain p-4 transition duration-300 group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-slate-300">
            <i className="bi bi-image text-4xl" />
          </div>
        )}
        {tieneDescuento && (
          <span className="absolute right-2 top-2 rounded-full bg-brand-danger px-2 py-1 text-xs font-bold text-white">-{Math.round(producto.descuento!)}%</span>
        )}
        {!agotado && (
          <button
            onClick={handleAgregar}
            disabled={agregando}
            aria-label="Agregar al carrito"
            className="absolute bottom-2 right-2 flex h-9 w-9 items-center justify-center rounded-full bg-black text-white opacity-0 shadow-md transition hover:bg-slate-800 group-hover:opacity-100 disabled:opacity-60"
          >
            <i className="bi bi-bag-plus" />
          </button>
        )}
      </div>
      <div className="mt-3">
        <h4 className="truncate text-sm font-semibold text-[#222]">{producto.nombre}</h4>
        <p className="text-xs uppercase text-slate-400">{producto.brand?.nombre}</p>
        <div className="mt-1 flex items-baseline gap-2">
          {tieneDescuento && <span className="text-xs text-slate-400 line-through">S/. {producto.precio.toFixed(2)}</span>}
          <span className="text-sm font-bold text-[#222]">S/. {precioFinal(producto.precio, producto.descuento).toFixed(2)}</span>
        </div>
      </div>
    </Link>
  )
}
