import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import Swal from 'sweetalert2'
import Modal from '../ui/Modal'
import { apiClient, ApiError } from '../../lib/apiClient'
import type { CreditoVenta, MetodoPago } from '../../types'

function notify(message: string, icon: 'success' | 'error' | 'warning' = 'error') {
  Swal.fire({ title: message, icon, toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 })
}

function formatPrice(value: number) {
  return `S/ ${value.toFixed(2)}`
}

function formatDate(date: string) {
  return new Date(date).toLocaleDateString('es-PE')
}

export default function PagarCuotaModal({ creditoId, onClose, onPaid }: { creditoId: number; onClose: () => void; onPaid: () => void }) {
  const [credito, setCredito] = useState<CreditoVenta | null>(null)
  const [loading, setLoading] = useState(true)
  const [montoPagado, setMontoPagado] = useState('')
  const [metodoPago, setMetodoPago] = useState<MetodoPago>('EFECTIVO')
  const [numeroOperacion, setNumeroOperacion] = useState('')
  const [observaciones, setObservaciones] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    apiClient
      .get<CreditoVenta>(`/creditos/api/${creditoId}`)
      .then((res) => setCredito(res.data as CreditoVenta))
      .catch((error) => notify(error instanceof ApiError ? error.message : 'Error al cargar el crédito'))
      .finally(() => setLoading(false))
  }, [creditoId])

  const cuotaPendiente = credito?.cuotas
    .filter((c) => c.estado === 'PENDIENTE' || c.estado === 'VENCIDA')
    .sort((a, b) => a.numero_cuota - b.numero_cuota)[0]

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!credito || !cuotaPendiente) return

    const monto = parseFloat(montoPagado)
    if (!monto || monto <= 0) {
      notify('El monto debe ser mayor a 0')
      return
    }
    if (monto > credito.saldo_pendiente) {
      notify(`El monto no puede exceder el saldo pendiente (${formatPrice(credito.saldo_pendiente)})`)
      return
    }

    setSaving(true)
    try {
      await apiClient.post('/creditos/api/pagos/registrar', {
        credito_id: credito.id,
        cuota_id: cuotaPendiente.id,
        monto_pagado: monto,
        metodo_pago: metodoPago,
        numero_operacion: numeroOperacion.trim() || null,
        observaciones: observaciones.trim() || null,
      })
      notify('Pago registrado correctamente', 'success')
      onPaid()
      onClose()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al registrar el pago')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title="Registrar Pago"
      onClose={onClose}
      footer={
        <>
          <button type="button" onClick={onClose} className="rounded-lg bg-slate-200 px-4 py-2 font-medium text-slate-700 transition hover:bg-slate-300">
            Cancelar
          </button>
          <button
            type="submit"
            form="formPago"
            disabled={saving || !cuotaPendiente}
            className="rounded-lg bg-brand-success px-4 py-2 font-medium text-white transition hover:bg-emerald-600 disabled:opacity-60"
          >
            {saving ? 'Registrando...' : 'Registrar Pago'}
          </button>
        </>
      }
    >
      {loading ? (
        <p className="py-8 text-center text-slate-500">Cargando...</p>
      ) : !credito ? (
        <p className="py-8 text-center text-brand-danger">No se pudo cargar el crédito</p>
      ) : !cuotaPendiente ? (
        <p className="py-8 text-center text-amber-600">No hay cuotas pendientes de pago</p>
      ) : (
        <>
          <div className="mb-4 rounded-lg border border-sky-200 bg-sky-50 p-4 text-sm">
            <div className="grid grid-cols-2 gap-2">
              <div>
                <span className="block text-xs text-slate-500">Cliente</span>
                <strong>{credito.cliente_nombre}</strong>
              </div>
              <div>
                <span className="block text-xs text-slate-500">Saldo Total Pendiente</span>
                <strong className="text-lg text-brand-danger">{formatPrice(credito.saldo_pendiente)}</strong>
              </div>
            </div>
            <hr className="my-2" />
            <div className="rounded-lg bg-amber-50 px-3 py-2">
              <i className="bi bi-calendar-check me-1" />
              <strong>Cuota a Pagar: #{cuotaPendiente.numero_cuota}</strong>
              <br />
              <small>Vencimiento: {formatDate(cuotaPendiente.fecha_vencimiento)}</small>
              <br />
              <strong>Monto cuota: {formatPrice(cuotaPendiente.saldo_pendiente)}</strong>
            </div>
          </div>

          <form id="formPago" onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="monto_pagado" className="mb-1 block text-sm font-medium">
                Monto a Pagar <span className="text-brand-danger">*</span>
              </label>
              <input
                id="monto_pagado"
                type="number"
                step="0.01"
                min="0.01"
                max={credito.saldo_pendiente}
                value={montoPagado}
                onChange={(e) => setMontoPagado(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
              />
            </div>

            <div>
              <label htmlFor="metodo_pago" className="mb-1 block text-sm font-medium">
                Forma de Pago:
              </label>
              <select
                id="metodo_pago"
                value={metodoPago}
                onChange={(e) => setMetodoPago(e.target.value as MetodoPago)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
              >
                <option value="EFECTIVO">Efectivo</option>
                <option value="YAPE">Yape</option>
                <option value="PLIN">Plin</option>
                <option value="TARJETA">Tarjeta</option>
                <option value="TRANSFERENCIA">Transferencia</option>
              </select>
            </div>

            <div>
              <label htmlFor="numero_operacion" className="mb-1 block text-sm font-medium">
                Número de Operación:
              </label>
              <input
                id="numero_operacion"
                value={numeroOperacion}
                onChange={(e) => setNumeroOperacion(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
              />
            </div>

            <div>
              <label htmlFor="observaciones_pago" className="mb-1 block text-sm font-medium">
                Observaciones:
              </label>
              <textarea
                id="observaciones_pago"
                rows={2}
                value={observaciones}
                onChange={(e) => setObservaciones(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
              />
            </div>
          </form>
        </>
      )}
    </Modal>
  )
}
