import { useEffect, useMemo, useState } from 'react'
import Swal from 'sweetalert2'
import Modal from '../ui/Modal'
import ImageThumbnail from '../ui/ImageThumbnail'
import { apiClient, ApiError } from '../../lib/apiClient'
import type { ProductoDisponible } from '../../types'

interface ProductPickerModalProps {
  yaEnCarrito: number[]
  onClose: () => void
  onConfirm: (productos: ProductoDisponible[]) => void
}

function notifyError(message: string) {
  Swal.fire({ title: message, icon: 'error', toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 })
}

export default function ProductPickerModal({ yaEnCarrito, onClose, onConfirm }: ProductPickerModalProps) {
  const [productos, setProductos] = useState<ProductoDisponible[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [seleccionados, setSeleccionados] = useState<ProductoDisponible[]>([])

  useEffect(() => {
    // Este endpoint devuelve el array directamente, sin el sobre {success, data} habitual.
    apiClient
      .get<ProductoDisponible[]>('/productos/api/listarDisponibles')
      .then((res) => setProductos(Array.isArray(res) ? (res as unknown as ProductoDisponible[]) : []))
      .catch((error) => notifyError(error instanceof ApiError ? error.message : 'Error al cargar productos disponibles'))
      .finally(() => setLoading(false))
  }, [])

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return productos
    return productos.filter((p) => p.nombre.toLowerCase().includes(term) || p.codigo.toLowerCase().includes(term))
  }, [productos, search])

  function toggleSeleccion(producto: ProductoDisponible) {
    setSeleccionados((prev) => {
      if (prev.some((p) => p.id === producto.id)) {
        return prev.filter((p) => p.id !== producto.id)
      }
      return [...prev, producto]
    })
  }

  function handleConfirmar() {
    if (seleccionados.length === 0) return
    onConfirm(seleccionados)
  }

  return (
    <Modal
      title="Seleccionar Producto"
      onClose={onClose}
      footer={
        <>
          <button type="button" onClick={onClose} className="rounded-lg bg-slate-200 px-4 py-2 font-medium text-slate-700 transition hover:bg-slate-300">
            Cancelar
          </button>
          <button
            type="button"
            onClick={handleConfirmar}
            disabled={seleccionados.length === 0}
            className="rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover disabled:opacity-60"
          >
            Agregar Productos ({seleccionados.length})
          </button>
        </>
      }
    >
      <input
        type="search"
        placeholder="Buscar producto por código o nombre..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="mb-3 w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
      />

      <div className="max-h-96 overflow-y-auto rounded-lg border border-slate-200">
        <table className="w-full text-center text-sm">
          <thead className="sticky top-0 bg-slate-50">
            <tr>
              <th className="px-2 py-2 font-semibold">Imagen</th>
              <th className="px-2 py-2 font-semibold">Código</th>
              <th className="px-2 py-2 font-semibold">Nombre</th>
              <th className="px-2 py-2 font-semibold">Stock</th>
              <th className="px-2 py-2 font-semibold">Precio</th>
              <th className="px-2 py-2 font-semibold">Acción</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} className="px-2 py-6 text-slate-500">
                  Cargando...
                </td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-2 py-6 text-slate-500">
                  No hay productos disponibles
                </td>
              </tr>
            ) : (
              filtered.map((p) => {
                const yaAgregado = yaEnCarrito.includes(p.id)
                const seleccionado = seleccionados.some((s) => s.id === p.id)
                return (
                  <tr key={p.id} className="border-t">
                    <td className="px-2 py-2">
                      <ImageThumbnail src={p.imagen} alt={p.nombre} className="h-10 w-14" />
                    </td>
                    <td className="px-2 py-2">{p.codigo}</td>
                    <td className="px-2 py-2 text-left">{p.nombre}</td>
                    <td className="px-2 py-2">{p.stock}</td>
                    <td className="px-2 py-2">S/ {p.precio.toFixed(2)}</td>
                    <td className="px-2 py-2">
                      <button
                        type="button"
                        disabled={yaAgregado}
                        onClick={() => toggleSeleccion(p)}
                        className={`rounded-lg px-3 py-1.5 text-xs font-medium text-white transition disabled:opacity-40 ${
                          seleccionado ? 'bg-brand-danger hover:bg-brand-danger-hover' : 'bg-brand-success hover:bg-emerald-600'
                        }`}
                      >
                        {yaAgregado ? 'En carrito' : seleccionado ? 'Quitar' : 'Seleccionar'}
                      </button>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-3 rounded-lg bg-sky-50 px-4 py-2 text-center text-sm text-sky-700">
        <i className="bi bi-cart-check me-2" />
        Productos seleccionados: <strong>{seleccionados.length}</strong>
      </div>
    </Modal>
  )
}
