import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { Product } from '../../types'

function precioFinal(precio: number, descuento: number | null) {
  if (!descuento) return precio
  return precio * (1 - descuento / 100)
}

export default function ProductCard({ producto }: { producto: Product }) {
  const [broken, setBroken] = useState(false)
  const tieneDescuento = !!producto.descuento && producto.descuento > 0

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
