import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { apiClient } from '../lib/apiClient'
import type { CarritoWeb } from '../types'

interface CartContextValue {
  carrito: CarritoWeb
  count: number
  loading: boolean
  agregar: (productoId: number, cantidad?: number) => Promise<void>
  actualizarCantidad: (productoId: number, cantidad: number) => Promise<void>
  eliminar: (productoId: number) => Promise<void>
  refrescar: () => Promise<void>
}

const CartContext = createContext<CartContextValue | null>(null)

const CARRITO_VACIO: CarritoWeb = { items: [], total: 0 }

export function CartProvider({ children }: { children: ReactNode }) {
  const [carrito, setCarrito] = useState<CarritoWeb>(CARRITO_VACIO)
  const [loading, setLoading] = useState(true)

  async function refrescar() {
    try {
      const data = await apiClient.get<never>('/carrito/api/ver')
      setCarrito({ items: (data.items as CarritoWeb['items']) ?? [], total: Number(data.total ?? 0) })
    } catch {
      setCarrito(CARRITO_VACIO)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    refrescar()
  }, [])

  async function agregar(productoId: number, cantidad = 1) {
    await apiClient.post(`/carrito/api/agregar/${productoId}?cantidad=${cantidad}`)
    await refrescar()
  }

  async function actualizarCantidad(productoId: number, cantidad: number) {
    await apiClient.put(`/carrito/api/actualizar/${productoId}?cantidad=${cantidad}`)
    await refrescar()
  }

  async function eliminar(productoId: number) {
    await apiClient.delete(`/carrito/api/eliminar/${productoId}`)
    await refrescar()
  }

  const count = carrito.items.reduce((acc, item) => acc + item.cantidad, 0)

  return (
    <CartContext.Provider value={{ carrito, count, loading, agregar, actualizarCantidad, eliminar, refrescar }}>
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart debe usarse dentro de CartProvider')
  return ctx
}
