import { useEffect, useState } from 'react'
import Swal from 'sweetalert2'
import Modal from '../ui/Modal'
import { apiClient, ApiError } from '../../lib/apiClient'
import type { CreditoVenta, EstadoVenta, Venta } from '../../types'

const ESTADO_BADGE: Record<EstadoVenta, string> = {
  PENDIENTE: 'bg-amber-500',
  PAGADA: 'bg-brand-success',
  ANULADA: 'bg-brand-danger',
}

function formatPrice(value: number) {
  return `S/ ${value.toFixed(2)}`
}

export default function DetalleVentaModal({ ventaId, onClose }: { ventaId: number; onClose: () => void }) {
  const [venta, setVenta] = useState<Venta | null>(null)
  const [credito, setCredito] = useState<CreditoVenta | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelado = false

    async function load() {
      try {
        const res = await apiClient.get<Venta>(`/ventas/api/${ventaId}`)
        const data = res.data as Venta
        if (cancelado) return
        setVenta(data)

        if (data.tiene_credito) {
          try {
            const creditoRes = await apiClient.get<CreditoVenta>(`/creditos/api/venta/${ventaId}`)
            if (!cancelado) setCredito(creditoRes.data as CreditoVenta)
          } catch {
            // El crédito puede no estar disponible todavía; no bloquea el detalle de la venta.
          }
        }
      } catch (error) {
        if (!cancelado) {
          Swal.fire({
            title: error instanceof ApiError ? error.message : 'No se pudo cargar la venta',
            icon: 'error',
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000,
          })
        }
      } finally {
        if (!cancelado) setLoading(false)
      }
    }

    load()
    return () => {
      cancelado = true
    }
  }, [ventaId])

  return (
    <Modal title={`Venta #${ventaId}`} onClose={onClose}>
      {loading ? (
        <p className="py-8 text-center text-slate-500">Cargando...</p>
      ) : !venta ? (
        <p className="py-8 text-center text-brand-danger">No se pudo cargar la venta</p>
      ) : (
        <div className="space-y-4">
          <div className="rounded-lg border border-brand-primary/30 bg-blue-50">
            <div className="rounded-t-lg bg-brand-primary px-4 py-2 text-sm font-semibold text-white">
              <i className="bi bi-file-earmark-text me-2" /> Información del Comprobante
            </div>
            <div className="grid grid-cols-2 gap-3 p-4 text-sm sm:grid-cols-4">
              <div>
                <span className="block text-xs text-slate-500">Comprobante</span>
                <strong>{venta.comprobante_completo}</strong>
              </div>
              <div>
                <span className="block text-xs text-slate-500">Serie - Número</span>
                <strong>
                  {venta.serie} - {venta.numero}
                </strong>
              </div>
              <div className="col-span-2">
                <span className="block text-xs text-slate-500">Cliente</span>
                <strong>{venta.cliente_nombre}</strong>
              </div>
              <div>
                <span className="block text-xs text-slate-500">Forma de Pago</span>
                <span className="rounded-full bg-sky-500 px-2.5 py-1 text-xs font-semibold text-white">
                  {venta.forma_pago === 'CREDITO' ? <i className="bi bi-calendar-check me-1" /> : <i className="bi bi-cash-coin me-1" />}
                  {venta.forma_pago}
                </span>
              </div>
              <div>
                <span className="block text-xs text-slate-500">Estado</span>
                <span className={`rounded-full px-2.5 py-1 text-xs font-semibold text-white ${ESTADO_BADGE[venta.estado]}`}>{venta.estado}</span>
              </div>
            </div>
          </div>

          {credito && (
            <div className="rounded-lg border border-amber-300">
              <div className="rounded-t-lg bg-amber-400 px-4 py-2 text-sm font-semibold text-amber-950">
                <i className="bi bi-credit-card me-2" /> Información de Crédito
              </div>
              <div className="grid grid-cols-3 gap-3 p-4 text-sm">
                <div>
                  <span className="block text-xs text-slate-500">Monto Inicial</span>
                  <strong className="text-brand-primary">{formatPrice(credito.monto_inicial)}</strong>
                </div>
                <div>
                  <span className="block text-xs text-slate-500">Cuotas</span>
                  <strong>{credito.numero_cuotas} cuotas</strong>
                </div>
                <div>
                  <span className="block text-xs text-slate-500">Interés</span>
                  <strong className="text-amber-600">{credito.interes_porcentaje.toFixed(2)}%</strong>
                </div>
                <div className="col-span-3 rounded-lg bg-red-50 px-4 py-2">
                  <div className="flex items-center justify-between">
                    <span className="text-brand-danger">
                      <i className="bi bi-exclamation-circle me-1" /> <strong>Deuda Pendiente:</strong>
                    </span>
                    <span className="text-lg font-bold">{formatPrice(credito.saldo_pendiente)}</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          <div className="overflow-x-auto rounded-lg border border-slate-200">
            <div className="bg-slate-50 px-4 py-2 text-sm font-semibold">
              <i className="bi bi-box-seam me-2" /> Detalle de Productos
            </div>
            <table className="w-full min-w-[500px] text-center text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-2 py-2">#</th>
                  <th className="px-2 py-2">Producto</th>
                  <th className="px-2 py-2">Cant.</th>
                  <th className="px-2 py-2 text-right">Precio</th>
                  <th className="px-2 py-2">Desc.</th>
                  <th className="px-2 py-2 text-right">Subtotal</th>
                </tr>
              </thead>
              <tbody>
                {venta.detalles.map((d, i) => (
                  <tr key={d.id} className="border-t">
                    <td className="px-2 py-1.5">
                      <span className="rounded bg-slate-500 px-1.5 py-0.5 text-xs text-white">{i + 1}</span>
                    </td>
                    <td className="px-2 py-1.5 text-left font-medium">{d.producto_nombre}</td>
                    <td className="px-2 py-1.5">
                      <span className="rounded bg-sky-100 px-1.5 py-0.5 text-xs">{d.cantidad}</span>
                    </td>
                    <td className="px-2 py-1.5 text-right text-slate-500">{formatPrice(d.precio_unitario)}</td>
                    <td className="px-2 py-1.5">{d.descuento_porcentaje > 0 ? `${d.descuento_porcentaje.toFixed(2)}%` : '-'}</td>
                    <td className="px-2 py-1.5 text-right font-semibold text-brand-primary">{formatPrice(d.subtotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="rounded-lg bg-slate-50 p-4">
            <div className="flex justify-between text-sm">
              <span>Subtotal:</span>
              <strong>{formatPrice(venta.subtotal)}</strong>
            </div>
            {venta.descuento_general > 0 && (
              <div className="flex justify-between text-sm text-amber-600">
                <span>Descuento General:</span>
                <strong>{venta.descuento_general.toFixed(2)}%</strong>
              </div>
            )}
            <hr className="my-2" />
            <div className="flex justify-between text-lg font-bold">
              <span>TOTAL:</span>
              <span className="text-brand-success">{formatPrice(venta.total)}</span>
            </div>
          </div>
        </div>
      )}
    </Modal>
  )
}
