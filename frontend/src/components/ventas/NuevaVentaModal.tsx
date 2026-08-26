import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import Swal from 'sweetalert2'
import Modal from '../ui/Modal'
import ProductPickerModal from './ProductPickerModal'
import { apiClient, ApiError } from '../../lib/apiClient'
import type {
  CarritoItem,
  ClienteVentaSeleccionado,
  FormaPago,
  IntervaloCredito,
  ProductoDisponible,
  TipoComprobante,
} from '../../types'

const INTERVALO_DIAS: Record<IntervaloCredito, number> = { SEMANAL: 7, QUINCENAL: 15, MENSUAL: 30 }

function notify(message: string, icon: 'success' | 'error' | 'warning' | 'info' = 'info') {
  Swal.fire({ title: message, icon, toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true })
}

function formatPrice(value: number) {
  return `S/ ${value.toFixed(2)}`
}

export default function NuevaVentaModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [documentoConsulta, setDocumentoConsulta] = useState('')
  const [cliente, setCliente] = useState<ClienteVentaSeleccionado | null>(null)
  const [buscandoCliente, setBuscandoCliente] = useState(false)
  const [facturaDisabled, setFacturaDisabled] = useState(false)

  const [tipoComprobante, setTipoComprobante] = useState<TipoComprobante>('BOLETA')
  const [serie, setSerie] = useState('')
  const [numeroPreview, setNumeroPreview] = useState('')

  const [formaPago, setFormaPago] = useState<FormaPago>('CONTADO')
  const [montoInicial, setMontoInicial] = useState('0')
  const [numeroCuotas, setNumeroCuotas] = useState('1')
  const [intervaloCuotas, setIntervaloCuotas] = useState<IntervaloCredito>('MENSUAL')
  const [interesPorcentaje, setInteresPorcentaje] = useState('0')

  const [carrito, setCarrito] = useState<CarritoItem[]>([])
  const [pickerOpen, setPickerOpen] = useState(false)
  const [descuentoGeneral, setDescuentoGeneral] = useState('0')
  const [observaciones, setObservaciones] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!tipoComprobante) {
      setSerie('')
      setNumeroPreview('')
      return
    }
    apiClient
      .get<{ serie: string; numero_actual: number }>(`/ventas/api/series/activas?tipo_comprobante=${tipoComprobante}`)
      .then((res) => {
        if (res.success && res.data) {
          const data = res.data as { serie: string; numero_actual: number }
          setSerie(data.serie)
          setNumeroPreview(String(data.numero_actual + 1))
        } else {
          setSerie('')
          setNumeroPreview('')
          Swal.fire('Sin series activas', (res.message as string) ?? 'No se encontró serie activa', 'warning')
        }
      })
      .catch(() => {
        setSerie('')
        setNumeroPreview('')
        notify('No se pudo obtener la serie activa', 'error')
      })
  }, [tipoComprobante])

  async function buscarCliente() {
    const documento = documentoConsulta.trim()
    if (!documento) {
      notify('Ingrese un documento', 'warning')
      return
    }
    if (!/^\d{8}$|^\d{11}$/.test(documento)) {
      notify('El documento debe ser DNI (8 dígitos) o RUC (11 dígitos)', 'warning')
      return
    }

    setBuscandoCliente(true)
    try {
      const res = await apiClient.get<{ id: number; nombre: string; documento: string; telefono: string; correo: string | null }>(
        `/clientes/api/documento/${documento}`,
      )
      const data = res.data as { id: number; nombre: string; documento: string; telefono: string; correo: string | null }
      seleccionarCliente({ id: data.id, nombre: data.nombre, documento: data.documento, telefono: data.telefono, correo: data.correo, esNuevo: false })
    } catch {
      notify('Consultando en RENIEC/SUNAT...', 'info')
      try {
        const res = await apiClient.get(`/clientes/api/consultar-documento/${documento}`)
        const nombre = res.nombre as string | undefined
        if (res.success && nombre) {
          seleccionarCliente({ id: null, nombre, documento, telefono: '000000000', correo: null, esNuevo: true })
        } else {
          notify((res.message as string) ?? 'No se encontraron datos del documento', 'error')
        }
      } catch (error) {
        notify(error instanceof ApiError ? error.message : 'Error al consultar la API externa', 'error')
      }
    } finally {
      setBuscandoCliente(false)
    }
  }

  function seleccionarCliente(c: ClienteVentaSeleccionado) {
    setCliente(c)
    const documento = c.documento.trim()
    if (documento.length === 8) {
      setFacturaDisabled(true)
      setTipoComprobante('BOLETA')
      notify('Clientes con DNI solo pueden recibir BOLETA', 'info')
    } else if (documento.length === 11) {
      setFacturaDisabled(false)
      setTipoComprobante('FACTURA')
    }
  }

  function agregarProductos(seleccionados: ProductoDisponible[]) {
    setCarrito((prev) => [
      ...prev,
      ...seleccionados.map((p) => {
        const descuento = p.descuento ?? 0
        const precioConDescuento = p.precio * (1 - descuento / 100)
        return {
          productoId: p.id,
          productoNombre: p.nombre,
          codigo: p.codigo,
          cantidad: 1,
          precioUnitario: p.precio,
          descuentoPorcentaje: descuento,
          subtotal: precioConDescuento,
          stockDisponible: p.stock,
        } satisfies CarritoItem
      }),
    ])
    setPickerOpen(false)
    notify('Productos agregados correctamente', 'success')
  }

  function actualizarCantidad(index: number, cantidad: number) {
    setCarrito((prev) => {
      const item = prev[index]
      if (cantidad > item.stockDisponible) {
        notify(`Stock insuficiente. Máximo: ${item.stockDisponible}`, 'error')
        return prev
      }
      const nueva = Math.max(1, cantidad)
      const precioConDescuento = item.precioUnitario * (1 - item.descuentoPorcentaje / 100)
      const next = [...prev]
      next[index] = { ...item, cantidad: nueva, subtotal: precioConDescuento * nueva }
      return next
    })
  }

  function eliminarItem(index: number) {
    setCarrito((prev) => prev.filter((_, i) => i !== index))
  }

  const subtotal = useMemo(() => carrito.reduce((sum, item) => sum + item.subtotal, 0), [carrito])
  const descuentoGeneralNum = parseFloat(descuentoGeneral) || 0
  const montoDescuentoGeneral = subtotal * (descuentoGeneralNum / 100)
  const totalSinInteres = subtotal - montoDescuentoGeneral

  const montoInicialNum = parseFloat(montoInicial) || 0
  const interesNum = parseFloat(interesPorcentaje) || 0
  const numeroCuotasNum = parseInt(numeroCuotas, 10) || 1
  const saldoAFinanciar = totalSinInteres - montoInicialNum
  const montoInteres = formaPago === 'CREDITO' ? saldoAFinanciar * (interesNum / 100) : 0
  const saldoConInteres = saldoAFinanciar + montoInteres
  const totalConInteres = montoInicialNum + saldoConInteres

  const cronograma = useMemo(() => {
    if (formaPago !== 'CREDITO' || carrito.length === 0 || saldoAFinanciar <= 0) return []
    const montoConInteresLocal = saldoAFinanciar * (1 + interesNum / 100)
    const montoPorCuota = montoConInteresLocal / numeroCuotasNum
    const dias = INTERVALO_DIAS[intervaloCuotas]
    const hoy = new Date()
    return Array.from({ length: numeroCuotasNum }, (_, i) => {
      const fecha = new Date(hoy)
      fecha.setDate(hoy.getDate() + dias * (i + 1))
      return { numero: i + 1, fecha: fecha.toLocaleDateString('es-PE'), monto: montoPorCuota }
    })
  }, [formaPago, carrito.length, saldoAFinanciar, interesNum, numeroCuotasNum, intervaloCuotas])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!cliente) {
      notify('Debe seleccionar un cliente', 'error')
      return
    }
    if (carrito.length === 0) {
      notify('Debe agregar al menos un producto', 'error')
      return
    }
    if (formaPago === 'CREDITO' && numeroCuotasNum < 1) {
      notify('El número de cuotas debe ser mayor a 0', 'error')
      return
    }

    const payload: Record<string, unknown> = {
      cliente_id: cliente.id,
      documento: cliente.documento,
      tipo_comprobante: tipoComprobante,
      serie,
      forma_pago: formaPago,
      descuento_general: descuentoGeneralNum,
      observaciones: observaciones.trim() || null,
      detalles: carrito.map((item) => ({
        producto_id: item.productoId,
        cantidad: item.cantidad,
        precio_unitario: item.precioUnitario,
        descuento_porcentaje: item.descuentoPorcentaje,
      })),
    }

    if (formaPago === 'CREDITO') {
      payload.credito = {
        numero_cuotas: numeroCuotasNum,
        intervalo_cuotas: intervaloCuotas,
        interes_porcentaje: interesNum,
        monto_inicial: montoInicialNum,
      }
    }

    setSaving(true)
    try {
      await apiClient.post('/ventas/api/crear', payload)
      notify('Venta creada correctamente', 'success')
      onCreated()
      onClose()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al crear la venta', 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <Modal
        title="Nueva Venta"
        onClose={onClose}
        footer={
          <>
            <button type="button" onClick={onClose} className="rounded-lg bg-slate-200 px-4 py-2 font-medium text-slate-700 transition hover:bg-slate-300">
              Cancelar
            </button>
            <button
              type="submit"
              form="formVenta"
              disabled={saving}
              className="rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover disabled:opacity-60"
            >
              {saving ? 'Guardando...' : 'Guardar Venta'}
            </button>
          </>
        }
      >
        <form id="formVenta" onSubmit={handleSubmit} className="space-y-5">
          <div>
            <h6 className="mb-2 flex items-center gap-2 text-sm font-semibold text-brand-primary">
              <i className="bi bi-person-circle" /> Datos del Cliente
            </h6>
            <label htmlFor="documento_cliente" className="mb-1 block text-sm font-medium">
              Documento del Cliente:
            </label>
            <div className="flex gap-2">
              <input
                id="documento_cliente"
                value={documentoConsulta}
                onChange={(e) => setDocumentoConsulta(e.target.value)}
                placeholder="DNI o RUC"
                className="flex-1 rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
              />
              <button
                type="button"
                onClick={buscarCliente}
                disabled={buscandoCliente}
                className="flex items-center gap-2 rounded-lg border border-brand-primary px-4 py-2 font-medium text-brand-primary transition hover:bg-brand-primary hover:text-white disabled:opacity-60"
              >
                <i className="bi bi-search" /> {buscandoCliente ? 'Buscando...' : 'Buscar'}
              </button>
            </div>
            {cliente && (
              <div className="mt-2 rounded-lg border border-emerald-300 bg-emerald-50 px-4 py-2 text-sm">
                <strong>{cliente.nombre}</strong>{' '}
                <span className={`rounded-full px-2 py-0.5 text-xs font-semibold text-white ${cliente.esNuevo ? 'bg-brand-success' : 'bg-sky-500'}`}>
                  {cliente.esNuevo ? 'NUEVO' : 'EXISTENTE'}
                </span>
                <br />
                <small className="text-slate-600">
                  Doc: {cliente.documento} | Tel: {cliente.telefono || 'N/A'}
                  {cliente.esNuevo && ' | Se creará automáticamente al guardar la venta'}
                </small>
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div>
              <label htmlFor="tipo_comprobante" className="mb-1 block text-sm font-medium">
                Tipo Comprobante:
              </label>
              <select
                id="tipo_comprobante"
                value={tipoComprobante}
                onChange={(e) => setTipoComprobante(e.target.value as TipoComprobante)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
              >
                <option value="BOLETA">Boleta</option>
                <option value="FACTURA" disabled={facturaDisabled}>
                  Factura
                </option>
              </select>
            </div>
            <div>
              <label htmlFor="serie" className="mb-1 block text-sm font-medium">
                Serie
              </label>
              <input id="serie" value={serie} readOnly className="w-full rounded-lg border border-slate-200 bg-slate-100 px-3 py-2" />
            </div>
            <div>
              <label htmlFor="numero_venta" className="mb-1 block text-sm font-medium">
                Número
              </label>
              <input id="numero_venta" value={numeroPreview} readOnly className="w-full rounded-lg border border-slate-200 bg-slate-100 px-3 py-2" />
            </div>
            <div>
              <label htmlFor="forma_pago" className="mb-1 block text-sm font-medium">
                Forma de Pago:
              </label>
              <select
                id="forma_pago"
                value={formaPago}
                onChange={(e) => setFormaPago(e.target.value as FormaPago)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
              >
                <option value="CONTADO">Contado</option>
                <option value="CREDITO">Crédito</option>
              </select>
            </div>
          </div>

          {formaPago === 'CREDITO' && (
            <div className="rounded-lg border border-amber-300 bg-amber-50 p-4">
              <h6 className="mb-3 flex items-center gap-2 text-sm font-semibold text-amber-800">
                <i className="bi bi-credit-card" /> Configuración del Crédito
              </h6>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                <div>
                  <label htmlFor="monto_inicial" className="mb-1 block text-xs font-medium">
                    Monto Inicial (S/):
                  </label>
                  <input
                    id="monto_inicial"
                    type="number"
                    step="0.01"
                    min="0"
                    value={montoInicial}
                    onChange={(e) => setMontoInicial(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 px-2 py-1.5"
                  />
                </div>
                <div>
                  <label htmlFor="numero_cuotas" className="mb-1 block text-xs font-medium">
                    Número de Cuotas:
                  </label>
                  <input
                    id="numero_cuotas"
                    type="number"
                    min="1"
                    value={numeroCuotas}
                    onChange={(e) => setNumeroCuotas(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 px-2 py-1.5"
                  />
                </div>
                <div>
                  <label htmlFor="intervalo_cuotas" className="mb-1 block text-xs font-medium">
                    Intervalo:
                  </label>
                  <select
                    id="intervalo_cuotas"
                    value={intervaloCuotas}
                    onChange={(e) => setIntervaloCuotas(e.target.value as IntervaloCredito)}
                    className="w-full rounded-lg border border-slate-300 px-2 py-1.5"
                  >
                    <option value="SEMANAL">Semanal</option>
                    <option value="QUINCENAL">Quincenal</option>
                    <option value="MENSUAL">Mensual</option>
                  </select>
                </div>
                <div>
                  <label htmlFor="interes_porcentaje" className="mb-1 block text-xs font-medium">
                    Interés (%):
                  </label>
                  <input
                    id="interes_porcentaje"
                    type="number"
                    step="0.01"
                    min="0"
                    value={interesPorcentaje}
                    onChange={(e) => setInteresPorcentaje(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 px-2 py-1.5"
                  />
                </div>
              </div>

              <div className="mt-3 rounded-lg border border-amber-300 bg-white">
                <div className="border-b border-amber-300 bg-amber-100 px-3 py-2 text-sm font-semibold">
                  <i className="bi bi-calendar-check me-2" /> Cronograma de Pagos
                </div>
                <div className="max-h-48 overflow-y-auto p-2">
                  {cronograma.length === 0 ? (
                    <p className="py-3 text-center text-sm text-slate-500">Agregue productos para calcular el cronograma</p>
                  ) : (
                    <table className="w-full text-center text-sm">
                      <thead>
                        <tr className="text-slate-500">
                          <th className="px-2 py-1">Cuota</th>
                          <th className="px-2 py-1">Fecha Vencimiento</th>
                          <th className="px-2 py-1 text-right">Monto</th>
                        </tr>
                      </thead>
                      <tbody>
                        {cronograma.map((c) => (
                          <tr key={c.numero} className="border-t">
                            <td className="px-2 py-1 font-semibold">Cuota {c.numero}</td>
                            <td className="px-2 py-1">{c.fecha}</td>
                            <td className="px-2 py-1 text-right">{formatPrice(c.monto)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              </div>
            </div>
          )}

          <div>
            <div className="mb-2 flex items-center justify-between">
              <h6 className="flex items-center gap-2 text-sm font-semibold text-brand-primary">
                <i className="bi bi-box-seam" /> Productos de la Venta
              </h6>
              <button
                type="button"
                onClick={() => setPickerOpen(true)}
                className="flex items-center gap-2 rounded-lg bg-brand-success px-3 py-1.5 text-sm font-medium text-white transition hover:bg-emerald-600"
              >
                <i className="bi bi-plus-circle" /> Buscar y Agregar Productos
              </button>
            </div>

            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="w-full min-w-[500px] text-center text-sm">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-2 py-2">#</th>
                    <th className="px-2 py-2">Producto</th>
                    <th className="px-2 py-2">Cantidad</th>
                    <th className="px-2 py-2 text-right">Precio</th>
                    <th className="px-2 py-2">Desc.</th>
                    <th className="px-2 py-2 text-right">Subtotal</th>
                    <th className="px-2 py-2">Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {carrito.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="px-2 py-4 text-slate-500">
                        No hay productos agregados
                      </td>
                    </tr>
                  ) : (
                    carrito.map((item, index) => (
                      <tr key={item.productoId} className="border-t">
                        <td className="px-2 py-1.5">{index + 1}</td>
                        <td className="px-2 py-1.5 text-left">{item.productoNombre}</td>
                        <td className="px-2 py-1.5">
                          <input
                            type="number"
                            min={1}
                            max={item.stockDisponible}
                            value={item.cantidad}
                            onChange={(e) => actualizarCantidad(index, parseInt(e.target.value, 10) || 1)}
                            className="w-20 rounded border border-slate-300 px-1.5 py-1 text-center"
                          />
                        </td>
                        <td className="px-2 py-1.5 text-right">{formatPrice(item.precioUnitario)}</td>
                        <td className="px-2 py-1.5">{item.descuentoPorcentaje > 0 ? `${item.descuentoPorcentaje.toFixed(2)}%` : '-'}</td>
                        <td className="px-2 py-1.5 text-right font-semibold">{formatPrice(item.subtotal)}</td>
                        <td className="px-2 py-1.5">
                          <button type="button" onClick={() => eliminarItem(index)} className="rounded bg-brand-danger px-2 py-1 text-white hover:bg-brand-danger-hover">
                            <i className="bi bi-trash" />
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div className="sm:col-span-2">
              <label htmlFor="descuento_general" className="mb-1 block text-sm font-medium">
                Descuento General (%):
              </label>
              <input
                id="descuento_general"
                type="number"
                step="0.01"
                min="0"
                max="100"
                value={descuentoGeneral}
                onChange={(e) => setDescuentoGeneral(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
              />
            </div>
            <div className="rounded-lg bg-slate-50 p-3">
              <div className="flex justify-between text-sm">
                <span>Subtotal:</span>
                <strong>{formatPrice(subtotal)}</strong>
              </div>
              <div className="flex justify-between text-sm">
                <span>Descuento:</span>
                <strong>{formatPrice(montoDescuentoGeneral)}</strong>
              </div>
              <hr className="my-2" />
              {formaPago === 'CREDITO' ? (
                <>
                  {montoInicialNum > 0 && (
                    <div className="flex justify-between text-sm text-brand-primary">
                      <span>Monto inicial:</span>
                      <strong>{formatPrice(montoInicialNum)}</strong>
                    </div>
                  )}
                  {interesNum > 0 && (
                    <div className="flex justify-between text-sm text-amber-600">
                      <span>Interés ({interesNum}%):</span>
                      <strong>+ {formatPrice(montoInteres)}</strong>
                    </div>
                  )}
                  <div className="mt-1 flex justify-between border-t pt-1 text-base font-bold">
                    <span>TOTAL A PAGAR:</span>
                    <span className="text-brand-success">{formatPrice(totalConInteres)}</span>
                  </div>
                </>
              ) : (
                <div className="flex justify-between text-lg font-bold">
                  <span>TOTAL:</span>
                  <span className="text-brand-success">{formatPrice(totalSinInteres)}</span>
                </div>
              )}
            </div>
          </div>

          <div>
            <label htmlFor="observaciones" className="mb-1 block text-sm font-medium">
              Observaciones:
            </label>
            <textarea
              id="observaciones"
              rows={2}
              value={observaciones}
              onChange={(e) => setObservaciones(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
            />
          </div>
        </form>
      </Modal>

      {pickerOpen && (
        <ProductPickerModal yaEnCarrito={carrito.map((c) => c.productoId)} onClose={() => setPickerOpen(false)} onConfirm={agregarProductos} />
      )}
    </>
  )
}
