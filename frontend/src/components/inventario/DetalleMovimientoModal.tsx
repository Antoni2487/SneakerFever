import Modal from '../ui/Modal'
import type { MovimientoInventario, TipoMovimientoInventario } from '../../types'

const TIPO_BADGE: Record<TipoMovimientoInventario, string> = {
  ENTRADA: 'bg-brand-success',
  SALIDA: 'bg-brand-danger',
  DEVOLUCION: 'bg-sky-500',
  MERMA: 'bg-amber-500',
}

const TIPO_LABEL: Record<TipoMovimientoInventario, string> = {
  ENTRADA: 'Entrada',
  SALIDA: 'Salida',
  DEVOLUCION: 'Devolución',
  MERMA: 'Merma',
}

const MOTIVO_LABEL: Record<string, string> = {
  COMPRA: 'Compra',
  VENTA: 'Venta',
  AJUSTE_FISICO: 'Ajuste físico',
  AJUSTE_POSITIVO: 'Ajuste positivo',
  AJUSTE_NEGATIVO: 'Ajuste negativo',
  MERMA: 'Merma',
  DEVOLUCION_CLIENTE: 'Devolución cliente',
}

const ES_ENTRADA: TipoMovimientoInventario[] = ['ENTRADA', 'DEVOLUCION']

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('es-PE', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <span className="block text-xs font-bold text-slate-500">{label}</span>
      <div className="text-sm">{children}</div>
    </div>
  )
}

export default function DetalleMovimientoModal({ movimiento, onClose }: { movimiento: MovimientoInventario; onClose: () => void }) {
  const esEntrada = ES_ENTRADA.includes(movimiento.tipoMovimiento)
  let referencia = '-'
  if (movimiento.referenciaTipo === 'VENTA' && movimiento.referenciaId) {
    referencia = `Venta #${movimiento.referenciaId}`
  } else if (movimiento.referenciaTipo !== 'NINGUNO') {
    referencia = movimiento.referenciaTipo
  }

  return (
    <Modal title="Detalle del Movimiento" onClose={onClose}>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Field label="ID Movimiento:">{movimiento.id}</Field>
        <Field label="Fecha:">{formatDateTime(movimiento.fechaMovimiento)}</Field>
        <div className="sm:col-span-2">
          <Field label="Producto:">{movimiento.productoNombre}</Field>
        </div>
        <Field label="Tipo:">
          <span className={`rounded-full px-2.5 py-1 text-xs font-semibold text-white ${TIPO_BADGE[movimiento.tipoMovimiento]}`}>
            {TIPO_LABEL[movimiento.tipoMovimiento]}
          </span>
        </Field>
        <Field label="Motivo:">{MOTIVO_LABEL[movimiento.motivo] ?? movimiento.motivo}</Field>
        <Field label="Cantidad:">
          <span className={`rounded-full px-2.5 py-1 text-xs font-semibold text-white ${esEntrada ? 'bg-brand-success' : 'bg-brand-danger'}`}>
            {esEntrada ? '+' : '-'}
            {movimiento.cantidad}
          </span>
        </Field>
        <Field label="Stock Anterior:">{movimiento.stockAnterior}</Field>
        <Field label="Stock Nuevo:">{movimiento.stockNuevo}</Field>
        <Field label="Usuario:">{movimiento.usuarioNombre}</Field>
        <Field label="Referencia:">{referencia}</Field>
        <div className="sm:col-span-2">
          <Field label="Observaciones:">{movimiento.observaciones || 'Sin observaciones'}</Field>
        </div>
      </div>
    </Modal>
  )
}
