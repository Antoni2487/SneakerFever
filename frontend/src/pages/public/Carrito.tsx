import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Swal from 'sweetalert2'
import { useCart } from '../../context/CartContext'
import { useAuth } from '../../context/AuthContext'
import { apiClient, ApiError } from '../../lib/apiClient'

function notify(title: string, icon: 'success' | 'error') {
  Swal.fire({ title, icon, toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true })
}

export default function Carrito() {
  const { carrito, loading, actualizarCantidad, eliminar, refrescar } = useCart()
  const { usuario } = useAuth()
  const navigate = useNavigate()

  const [documento, setDocumento] = useState('')
  const [tipoComprobante, setTipoComprobante] = useState<'BOLETA' | 'FACTURA'>('BOLETA')
  const [procesando, setProcesando] = useState(false)

  async function handleCheckout() {
    const doc = documento.trim()
    if (!doc) {
      notify('Ingresa tu DNI o RUC', 'error')
      return
    }
    if (tipoComprobante === 'FACTURA' && doc.length !== 11) {
      notify('Para Factura el documento debe ser un RUC de 11 dígitos', 'error')
      return
    }
    if (tipoComprobante === 'BOLETA' && doc.length !== 8 && doc.length !== 11) {
      notify('Documento inválido: debe ser DNI (8 dígitos) o RUC (11 dígitos)', 'error')
      return
    }

    setProcesando(true)
    try {
      const res = await apiClient.post<{ message: string }>('/carrito/api/checkout', { documento: doc, tipoComprobante })
      notify((res.message as string) ?? 'Compra realizada con éxito', 'success')
      await refrescar()
      navigate('/catalogo')
    } catch (err) {
      notify(err instanceof ApiError ? err.message : 'No se pudo procesar la compra', 'error')
    } finally {
      setProcesando(false)
    }
  }

  if (loading) {
    return (
      <main className="flex min-h-[60vh] items-center justify-center">
        <i className="bi bi-arrow-repeat animate-spin text-3xl text-slate-300" />
      </main>
    )
  }

  if (carrito.items.length === 0) {
    return (
      <main className="mx-auto flex min-h-[60vh] max-w-2xl flex-col items-center justify-center px-4 text-center">
        <i className="bi bi-bag-x text-5xl text-slate-300" />
        <h1 className="mt-4 text-2xl font-black tracking-wide">Tu carrito está vacío</h1>
        <p className="mt-2 text-sm text-slate-500">Explora el catálogo y agrega tus zapatillas favoritas.</p>
        <Link to="/catalogo" className="mt-6 rounded-full bg-black px-6 py-2.5 text-sm font-bold uppercase text-white transition hover:bg-slate-800">
          Ir al catálogo
        </Link>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-6xl px-4 py-10 lg:px-8">
      <h1 className="mb-8 text-2xl font-black tracking-tight">Mi Carrito</h1>

      <div className="grid grid-cols-1 gap-10 lg:grid-cols-[1.6fr_1fr]">
        <div className="space-y-4">
          {carrito.items.map((item) => (
            <div key={item.productoId} className="flex items-center gap-4 rounded-xl border border-slate-200 bg-white p-4">
              <div className="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-slate-50">
                {item.imagen ? (
                  <img src={item.imagen} alt={item.nombre} className="h-full w-full object-contain" />
                ) : (
                  <i className="bi bi-image text-2xl text-slate-300" />
                )}
              </div>

              <div className="min-w-0 flex-1">
                <h3 className="truncate text-sm font-semibold">{item.nombre}</h3>
                <p className="mt-0.5 text-xs text-slate-500">S/ {item.precio.toFixed(2)} c/u</p>

                <div className="mt-2 flex items-center gap-3">
                  <div className="flex items-center rounded-lg border border-slate-300">
                    <button
                      onClick={() => actualizarCantidad(item.productoId, item.cantidad - 1)}
                      disabled={item.cantidad <= 1}
                      className="px-2.5 py-1 text-sm font-bold text-slate-600 disabled:opacity-30"
                    >
                      −
                    </button>
                    <span className="min-w-[2rem] px-1 text-center text-sm">{item.cantidad}</span>
                    <button
                      onClick={() => actualizarCantidad(item.productoId, item.cantidad + 1)}
                      className="px-2.5 py-1 text-sm font-bold text-slate-600"
                    >
                      +
                    </button>
                  </div>
                  <button onClick={() => eliminar(item.productoId)} className="text-xs font-semibold text-brand-danger hover:underline">
                    <i className="bi bi-trash me-1" />
                    Eliminar
                  </button>
                </div>
              </div>

              <div className="shrink-0 text-right text-sm font-bold">S/ {item.subtotal.toFixed(2)}</div>
            </div>
          ))}
        </div>

        <div className="h-fit rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-xs font-black uppercase tracking-widest">Resumen</h2>
          <div className="flex items-center justify-between border-b border-slate-100 pb-4 text-sm">
            <span className="text-slate-500">Total</span>
            <span className="text-xl font-black">S/ {carrito.total.toFixed(2)}</span>
          </div>

          {usuario ? (
            <div className="mt-4 space-y-3">
              <div>
                <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">DNI o RUC</label>
                <input
                  type="text"
                  value={documento}
                  onChange={(e) => setDocumento(e.target.value.replace(/\D/g, ''))}
                  maxLength={11}
                  placeholder="12345678"
                  className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-black focus:outline-none"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Comprobante</label>
                <select
                  value={tipoComprobante}
                  onChange={(e) => setTipoComprobante(e.target.value as 'BOLETA' | 'FACTURA')}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-black focus:outline-none"
                >
                  <option value="BOLETA">Boleta</option>
                  <option value="FACTURA">Factura</option>
                </select>
              </div>
              <button
                onClick={handleCheckout}
                disabled={procesando}
                className="mt-2 flex w-full items-center justify-center gap-2 rounded-full bg-black py-3 text-sm font-bold uppercase tracking-wide text-white transition hover:bg-slate-800 disabled:opacity-60"
              >
                {procesando ? 'Procesando...' : 'Finalizar compra'} <i className="bi bi-arrow-right" />
              </button>
            </div>
          ) : (
            <div className="mt-4">
              <p className="mb-3 text-sm text-slate-500">Inicia sesión para completar tu compra.</p>
              <button
                onClick={() => navigate('/login?redirect=/carrito')}
                className="flex w-full items-center justify-center gap-2 rounded-full bg-black py-3 text-sm font-bold uppercase tracking-wide text-white transition hover:bg-slate-800"
              >
                Iniciar sesión <i className="bi bi-box-arrow-in-right" />
              </button>
            </div>
          )}
        </div>
      </div>
    </main>
  )
}
