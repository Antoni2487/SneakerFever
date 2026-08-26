import { useEffect, useMemo, useState } from 'react'
import Swal from 'sweetalert2'
import { apiClient, ApiError } from '../lib/apiClient'
import type { EstadoVenta, FormaPago, Venta, VentaEstadisticas } from '../types'
import NuevaVentaModal from '../components/ventas/NuevaVentaModal'
import DetalleVentaModal from '../components/ventas/DetalleVentaModal'
import CreditosTab from '../components/ventas/CreditosTab'

const ESTADO_BADGE: Record<EstadoVenta, string> = {
  PENDIENTE: 'bg-amber-500',
  PAGADA: 'bg-brand-success',
  ANULADA: 'bg-brand-danger',
}

const FORMA_PAGO_BADGE: Record<FormaPago, string> = {
  CONTADO: 'bg-brand-success',
  CREDITO: 'bg-amber-500',
}

function notify(message: string, icon: 'success' | 'error' | 'info' = 'info') {
  Swal.fire({ title: message, icon, toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true })
}

function formatPrice(value: number) {
  return `S/ ${value.toFixed(2)}`
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export default function Ventas() {
  const [tab, setTab] = useState<'ventas' | 'creditos'>('ventas')
  const [ventas, setVentas] = useState<Venta[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [stats, setStats] = useState<VentaEstadisticas>({ ventasHoy: 0, ventasSemana: 0, ventasMes: 0, totalVentas: 0 })

  const [nuevaVentaOpen, setNuevaVentaOpen] = useState(false)
  const [detalleId, setDetalleId] = useState<number | null>(null)

  useEffect(() => {
    loadVentas()
    loadStats()
  }, [])

  async function loadVentas() {
    setLoading(true)
    try {
      const res = await apiClient.get<Venta[]>('/ventas/api/listar')
      setVentas((res.data as Venta[]) ?? [])
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar ventas', 'error')
    } finally {
      setLoading(false)
    }
  }

  async function loadStats() {
    try {
      const res = await apiClient.get<VentaEstadisticas>('/ventas/api/estadisticas')
      setStats((res.data as VentaEstadisticas) ?? { ventasHoy: 0, ventasSemana: 0, ventasMes: 0, totalVentas: 0 })
    } catch {
      // Las tarjetas de estadísticas son informativas; si fallan quedan en 0.
    }
  }

  function refrescarTodo() {
    loadVentas()
    loadStats()
  }

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return ventas
    return ventas.filter(
      (v) => v.cliente_nombre.toLowerCase().includes(term) || v.comprobante_completo.toLowerCase().includes(term) || v.cliente_documento.includes(term),
    )
  }, [ventas, search])

  async function handleAnular(venta: Venta) {
    const result = await Swal.fire({
      title: '¿Anular venta?',
      text: 'Esta acción devolverá el stock de los productos. ¿Está seguro?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí, anular',
      cancelButtonText: 'Cancelar',
    })
    if (!result.isConfirmed) return

    try {
      await apiClient.put(`/ventas/api/anular/${venta.id}`)
      notify('Venta anulada exitosamente', 'success')
      refrescarTodo()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al anular la venta', 'error')
    }
  }

  return (
    <div>
      <header className="border-b bg-white p-3 shadow-sm md:p-4">
        <h1 className="text-xl font-bold md:text-2xl">Gestión de Ventas y Créditos</h1>
      </header>

      <div className="p-4 md:p-6">
        <div className="mb-4 flex gap-2 border-b border-slate-200">
          <button
            onClick={() => setTab('ventas')}
            className={`flex items-center gap-2 border-b-[3px] px-6 py-3 text-sm font-semibold transition ${
              tab === 'ventas' ? 'border-brand-primary text-brand-primary' : 'border-transparent text-slate-500 hover:text-brand-primary'
            }`}
          >
            <i className="bi bi-cart-check" /> Ventas
          </button>
          <button
            onClick={() => setTab('creditos')}
            className={`flex items-center gap-2 border-b-[3px] px-6 py-3 text-sm font-semibold transition ${
              tab === 'creditos' ? 'border-brand-primary text-brand-primary' : 'border-transparent text-slate-500 hover:text-brand-primary'
            }`}
          >
            <i className="bi bi-credit-card" /> Créditos
          </button>
        </div>

        {tab === 'ventas' ? (
          <div>
            <div className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div className="rounded-xl border-t-4 border-brand-primary bg-white p-4 text-center shadow-sm">
                <i className="bi bi-calendar-day text-3xl text-brand-primary" />
                <h6 className="mt-2 text-sm text-slate-500">Ventas Hoy</h6>
                <div className="text-xl font-bold text-brand-primary">{formatPrice(stats.ventasHoy)}</div>
              </div>
              <div className="rounded-xl border-t-4 border-brand-success bg-white p-4 text-center shadow-sm">
                <i className="bi bi-calendar-week text-3xl text-brand-success" />
                <h6 className="mt-2 text-sm text-slate-500">Ventas Semana</h6>
                <div className="text-xl font-bold text-brand-success">{formatPrice(stats.ventasSemana)}</div>
              </div>
              <div className="rounded-xl border-t-4 border-sky-500 bg-white p-4 text-center shadow-sm">
                <i className="bi bi-calendar-month text-3xl text-sky-500" />
                <h6 className="mt-2 text-sm text-slate-500">Ventas Mes</h6>
                <div className="text-xl font-bold text-sky-500">{formatPrice(stats.ventasMes)}</div>
              </div>
              <div className="rounded-xl border-t-4 border-amber-500 bg-white p-4 text-center shadow-sm">
                <i className="bi bi-receipt text-3xl text-amber-500" />
                <h6 className="mt-2 text-sm text-slate-500">Total Ventas</h6>
                <div className="text-xl font-bold text-amber-500">{stats.totalVentas}</div>
              </div>
            </div>

            <div className="mb-3 flex items-center justify-between">
              <h4 className="text-lg font-bold">
                <i className="bi bi-list-ul me-2" />
                Lista de Ventas
              </h4>
              <button
                onClick={() => setNuevaVentaOpen(true)}
                className="flex items-center gap-2 rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover"
              >
                <i className="bi bi-plus-circle" /> Nueva Venta
              </button>
            </div>

            <div className="rounded-xl bg-white p-4 shadow-sm md:p-6">
              <div className="mb-4 flex justify-end">
                <input
                  type="search"
                  placeholder="Buscar..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="w-full max-w-xs rounded-lg border border-slate-300 px-3 py-1.5 text-sm focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
                />
              </div>

              <div className="overflow-x-auto">
                <table className="w-full min-w-[900px] text-center text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-2 py-2">ID</th>
                      <th className="px-2 py-2">Comprobante</th>
                      <th className="px-2 py-2">Cliente</th>
                      <th className="px-2 py-2">Forma Pago</th>
                      <th className="px-2 py-2 text-right">Total</th>
                      <th className="px-2 py-2">Estado</th>
                      <th className="px-2 py-2">Fecha</th>
                      <th className="px-2 py-2">Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {loading ? (
                      <tr>
                        <td colSpan={8} className="px-2 py-6 text-slate-500">
                          Cargando...
                        </td>
                      </tr>
                    ) : filtered.length === 0 ? (
                      <tr>
                        <td colSpan={8} className="px-2 py-6 text-slate-500">
                          No se encontraron resultados
                        </td>
                      </tr>
                    ) : (
                      filtered.map((venta) => (
                        <tr key={venta.id} className="border-t">
                          <td className="px-2 py-2">{venta.id}</td>
                          <td className="px-2 py-2">{venta.comprobante_completo}</td>
                          <td className="px-2 py-2 text-left">
                            {venta.cliente_nombre}
                            <br />
                            <small className="text-slate-400">{venta.cliente_documento}</small>
                          </td>
                          <td className="px-2 py-2">
                            <span className={`rounded-full px-2 py-0.5 text-xs font-semibold text-white ${FORMA_PAGO_BADGE[venta.forma_pago]}`}>
                              {venta.forma_pago === 'CONTADO' ? 'Contado' : 'Crédito'}
                            </span>
                          </td>
                          <td className="px-2 py-2 text-right font-semibold">{formatPrice(venta.total)}</td>
                          <td className="px-2 py-2">
                            <span className={`rounded-full px-2 py-0.5 text-xs font-semibold text-white ${ESTADO_BADGE[venta.estado]}`}>{venta.estado}</span>
                          </td>
                          <td className="px-2 py-2">{formatDateTime(venta.fecha_creacion)}</td>
                          <td className="px-2 py-2">
                            <div className="flex items-center justify-center gap-1.5">
                              <button onClick={() => setDetalleId(venta.id)} title="Ver detalles" className="rounded bg-sky-500 px-2 py-1.5 text-white hover:bg-sky-600">
                                <i className="bi bi-eye" />
                              </button>
                              {venta.estado !== 'ANULADA' && (
                                <button onClick={() => handleAnular(venta)} title="Anular venta" className="rounded bg-brand-danger px-2 py-1.5 text-white hover:bg-brand-danger-hover">
                                  <i className="bi bi-x-circle" />
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        ) : (
          <CreditosTab />
        )}
      </div>

      {nuevaVentaOpen && <NuevaVentaModal onClose={() => setNuevaVentaOpen(false)} onCreated={refrescarTodo} />}
      {detalleId !== null && <DetalleVentaModal ventaId={detalleId} onClose={() => setDetalleId(null)} />}
    </div>
  )
}
