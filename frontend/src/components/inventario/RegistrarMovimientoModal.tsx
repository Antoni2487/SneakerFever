import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import Swal from 'sweetalert2'
import Modal from '../ui/Modal'
import { apiClient, ApiError } from '../../lib/apiClient'
import type { MotivoMovimiento, Product, TipoMovimientoInventario } from '../../types'

const MOTIVOS_POR_TIPO: Record<TipoMovimientoInventario, { value: MotivoMovimiento; label: string }[]> = {
  ENTRADA: [
    { value: 'COMPRA', label: 'Compra de mercadería' },
    { value: 'DEVOLUCION_CLIENTE', label: 'Devolución de cliente' },
    { value: 'AJUSTE_POSITIVO', label: 'Ajuste positivo' },
  ],
  SALIDA: [
    { value: 'VENTA', label: 'Venta al cliente' },
    { value: 'AJUSTE_NEGATIVO', label: 'Ajuste negativo' },
  ],
  DEVOLUCION: [{ value: 'DEVOLUCION_CLIENTE', label: 'Devolución de cliente' }],
  MERMA: [{ value: 'MERMA', label: 'Producto vencido/dañado' }],
}

function notify(message: string, icon: 'success' | 'error' | 'warning' | 'info' = 'info') {
  Swal.fire({ title: message, icon, toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true })
}

interface RegistrarMovimientoModalProps {
  productos: Product[]
  prefillProductoId?: number
  onClose: () => void
  onSaved: () => void
}

export default function RegistrarMovimientoModal({ productos, prefillProductoId, onClose, onSaved }: RegistrarMovimientoModalProps) {
  const [productoId, setProductoId] = useState(prefillProductoId ? String(prefillProductoId) : '')
  const [tipoMovimiento, setTipoMovimiento] = useState<TipoMovimientoInventario | ''>(prefillProductoId ? 'ENTRADA' : '')
  const [cantidad, setCantidad] = useState('')
  const [motivo, setMotivo] = useState<MotivoMovimiento | ''>('')
  const [observaciones, setObservaciones] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  const productoSeleccionado = productos.find((p) => p.id === Number(productoId))
  const stockActual = productoSeleccionado?.stock ?? 0
  const stockMinimo = productoSeleccionado?.stockMinimo ?? 0

  useEffect(() => {
    setMotivo('')
  }, [tipoMovimiento])

  const motivosDisponibles = tipoMovimiento ? MOTIVOS_POR_TIPO[tipoMovimiento] : []

  const nuevoStock = useMemo(() => {
    const cant = parseInt(cantidad, 10)
    if (!tipoMovimiento || !cant || cant <= 0) return null
    return tipoMovimiento === 'ENTRADA' || tipoMovimiento === 'DEVOLUCION' ? stockActual + cant : stockActual - cant
  }, [tipoMovimiento, cantidad, stockActual])

  const previewClass =
    nuevoStock == null
      ? ''
      : nuevoStock < 0
        ? 'border-brand-danger bg-red-50 text-brand-danger'
        : nuevoStock === 0
          ? 'border-amber-300 bg-amber-50 text-amber-700'
          : 'border-brand-success bg-emerald-50 text-brand-success'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const errors: Record<string, string> = {}
    if (!productoId) errors.productoId = 'Debe seleccionar un producto'
    if (!tipoMovimiento) errors.tipoMovimiento = 'Debe seleccionar un tipo'
    const cant = parseInt(cantidad, 10)
    if (!cant || cant < 1) errors.cantidad = 'La cantidad debe ser mayor a 0'
    if (!motivo) errors.motivo = 'Debe seleccionar un motivo'

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    setSaving(true)
    setFieldErrors({})
    try {
      await apiClient.post('/inventario/api/registrar', {
        productoId: Number(productoId),
        tipoMovimiento,
        cantidad: cant,
        motivo,
        observaciones: observaciones.trim() || null,
        referenciaId: null,
        referenciaTipo: 'NINGUNO',
      })
      notify('Movimiento registrado exitosamente', 'success')
      onSaved()
      onClose()
    } catch (error) {
      if (error instanceof ApiError && error.errors) {
        setFieldErrors(error.errors)
      } else {
        notify(error instanceof ApiError ? error.message : 'Error al registrar movimiento', 'error')
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title="Registrar Movimiento de Inventario"
      onClose={onClose}
      footer={
        <>
          <button type="button" onClick={onClose} className="rounded-lg bg-slate-200 px-4 py-2 font-medium text-slate-700 transition hover:bg-slate-300">
            Cancelar
          </button>
          <button
            type="submit"
            form="formMovimiento"
            disabled={saving}
            className="rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover disabled:opacity-60"
          >
            {saving ? 'Guardando...' : 'Registrar Movimiento'}
          </button>
        </>
      }
    >
      <form id="formMovimiento" onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="productoId" className="mb-1 block text-sm font-medium">
            Producto <span className="text-brand-danger">*</span>
          </label>
          <select
            id="productoId"
            value={productoId}
            onChange={(e) => setProductoId(e.target.value)}
            className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
              fieldErrors.productoId ? 'border-brand-danger focus:ring-brand-danger/25' : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
            }`}
          >
            <option value="">Seleccione un producto</option>
            {productos.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nombre}
              </option>
            ))}
          </select>
          {fieldErrors.productoId && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.productoId}</p>}
        </div>

        {productoSeleccionado && (
          <div className="rounded-lg bg-sky-50 px-4 py-2 text-sm">
            <strong>Stock actual:</strong> {stockActual} unidades
            {stockActual === 0 ? (
              <span className="ml-2 rounded-full bg-brand-danger px-2 py-0.5 text-xs font-semibold text-white">Sin stock</span>
            ) : stockActual <= stockMinimo ? (
              <span className="ml-2 rounded-full bg-amber-500 px-2 py-0.5 text-xs font-semibold text-white">Stock bajo</span>
            ) : null}
          </div>
        )}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="tipoMovimiento" className="mb-1 block text-sm font-medium">
              Tipo de Movimiento <span className="text-brand-danger">*</span>
            </label>
            <select
              id="tipoMovimiento"
              value={tipoMovimiento}
              onChange={(e) => setTipoMovimiento(e.target.value as TipoMovimientoInventario)}
              className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                fieldErrors.tipoMovimiento ? 'border-brand-danger focus:ring-brand-danger/25' : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
              }`}
            >
              <option value="">Seleccione tipo</option>
              <option value="ENTRADA">Entrada</option>
              <option value="SALIDA">Salida</option>
              <option value="DEVOLUCION">Devolución</option>
              <option value="MERMA">Merma</option>
            </select>
            {fieldErrors.tipoMovimiento && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.tipoMovimiento}</p>}
          </div>

          <div>
            <label htmlFor="cantidad" className="mb-1 block text-sm font-medium">
              Cantidad <span className="text-brand-danger">*</span>
            </label>
            <input
              id="cantidad"
              type="number"
              min={1}
              value={cantidad}
              onChange={(e) => setCantidad(e.target.value)}
              className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                fieldErrors.cantidad ? 'border-brand-danger focus:ring-brand-danger/25' : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
              }`}
            />
            {fieldErrors.cantidad && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.cantidad}</p>}
          </div>
        </div>

        <div>
          <label htmlFor="motivo" className="mb-1 block text-sm font-medium">
            Motivo <span className="text-brand-danger">*</span>
          </label>
          <select
            id="motivo"
            value={motivo}
            onChange={(e) => setMotivo(e.target.value as MotivoMovimiento)}
            disabled={!tipoMovimiento}
            className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 disabled:opacity-60 ${
              fieldErrors.motivo ? 'border-brand-danger focus:ring-brand-danger/25' : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
            }`}
          >
            <option value="">Seleccione motivo</option>
            {motivosDisponibles.map((m) => (
              <option key={m.value} value={m.value}>
                {m.label}
              </option>
            ))}
          </select>
          {fieldErrors.motivo && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.motivo}</p>}
        </div>

        <div>
          <label htmlFor="observaciones" className="mb-1 block text-sm font-medium">
            Observaciones
          </label>
          <textarea
            id="observaciones"
            rows={3}
            maxLength={500}
            value={observaciones}
            onChange={(e) => setObservaciones(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
          />
          <p className="mt-1 text-xs text-slate-400">Opcional. Máximo 500 caracteres.</p>
        </div>

        {nuevoStock != null && (
          <div className={`rounded-lg border px-4 py-2 text-sm font-medium ${previewClass}`}>
            {nuevoStock < 0 ? '⚠️ Stock insuficiente. Nuevo stock: ' : nuevoStock === 0 ? '⚠️ Stock quedará en cero. Nuevo stock: ' : '✓ Nuevo stock: '}
            {nuevoStock}
          </div>
        )}
      </form>
    </Modal>
  )
}
