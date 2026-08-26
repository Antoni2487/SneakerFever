import { useEffect, useMemo, useState } from 'react'
import Swal from 'sweetalert2'
import { apiClient, ApiError } from '../lib/apiClient'
import type { MovimientoInventario, Product, TipoMovimientoInventario } from '../types'
import RegistrarMovimientoModal from '../components/inventario/RegistrarMovimientoModal'
import DetalleMovimientoModal from '../components/inventario/DetalleMovimientoModal'

type Tab = 'movimientos' | 'kardex' | 'stock'
type FiltroTipo = 'TODOS' | TipoMovimientoInventario

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

const FILTROS: { key: FiltroTipo; label: string; activeClass: string }[] = [
  { key: 'TODOS', label: 'Todos', activeClass: 'bg-brand-primary text-white border-brand-primary' },
  { key: 'ENTRADA', label: 'Entradas', activeClass: 'bg-brand-success text-white border-brand-success' },
  { key: 'SALIDA', label: 'Salidas', activeClass: 'bg-brand-danger text-white border-brand-danger' },
  { key: 'DEVOLUCION', label: 'Devoluciones', activeClass: 'bg-sky-500 text-white border-sky-500' },
  { key: 'MERMA', label: 'Mermas', activeClass: 'bg-amber-500 text-white border-amber-500' },
]

function notify(message: string, icon: 'success' | 'error' | 'info' = 'info') {
  Swal.fire({ title: message, icon, toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true })
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('es-PE', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

export default function Inventario() {
  const [tab, setTab] = useState<Tab>('movimientos')
  const [productos, setProductos] = useState<Product[]>([])
  const [stockBajo, setStockBajo] = useState<Product[]>([])

  const [movimientos, setMovimientos] = useState<MovimientoInventario[]>([])
  const [loadingMovimientos, setLoadingMovimientos] = useState(true)
  const [filtroTipo, setFiltroTipo] = useState<FiltroTipo>('TODOS')

  const [ultimosParaStats, setUltimosParaStats] = useState<MovimientoInventario[]>([])

  const [kardexProductoId, setKardexProductoId] = useState('')
  const [kardexData, setKardexData] = useState<MovimientoInventario[] | null>(null)
  const [loadingKardex, setLoadingKardex] = useState(false)

  const [modalOpen, setModalOpen] = useState(false)
  const [prefillProductoId, setPrefillProductoId] = useState<number | undefined>(undefined)
  const [detalleMovimiento, setDetalleMovimiento] = useState<MovimientoInventario | null>(null)

  useEffect(() => {
    loadProductos()
    loadStockBajo()
    loadStatsMovimientos()
  }, [])

  useEffect(() => {
    loadMovimientos(filtroTipo)
  }, [filtroTipo])

  async function loadProductos() {
    try {
      const res = await apiClient.get<Product[]>('/productos/api/listar')
      setProductos((res.data as Product[]) ?? [])
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar productos', 'error')
    }
  }

  async function loadStockBajo() {
    try {
      const res = await apiClient.get<Product[]>('/productos/api/stock-bajo')
      setStockBajo((res.data as Product[]) ?? [])
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar stock bajo', 'error')
    }
  }

  async function loadStatsMovimientos() {
    try {
      const res = await apiClient.get<MovimientoInventario[]>('/inventario/api/ultimos')
      setUltimosParaStats((res.data as MovimientoInventario[]) ?? [])
    } catch {
      // Las estadísticas son informativas; si fallan quedan en 0.
    }
  }

  async function loadMovimientos(tipo: FiltroTipo) {
    setLoadingMovimientos(true)
    try {
      const url = tipo === 'TODOS' ? '/inventario/api/ultimos' : `/inventario/api/tipo/${tipo}`
      const res = await apiClient.get<MovimientoInventario[]>(url)
      setMovimientos((res.data as MovimientoInventario[]) ?? [])
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar movimientos', 'error')
    } finally {
      setLoadingMovimientos(false)
    }
  }

  function refrescarTodo() {
    loadProductos()
    loadStockBajo()
    loadStatsMovimientos()
    loadMovimientos(filtroTipo)
  }

  async function handleBuscarKardex() {
    if (!kardexProductoId) {
      notify('Debe seleccionar un producto', 'info')
      return
    }
    setLoadingKardex(true)
    try {
      const res = await apiClient.get<MovimientoInventario[]>(`/inventario/api/kardex/${kardexProductoId}`)
      setKardexData((res.data as MovimientoInventario[]) ?? [])
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar el kardex', 'error')
    } finally {
      setLoadingKardex(false)
    }
  }

  function handleReponerStock(producto: Product) {
    setPrefillProductoId(producto.id)
    setModalOpen(true)
    notify(`Reponiendo stock de: ${producto.nombre}`, 'info')
  }

  const stats = useMemo(() => {
    const hoy = new Date().toISOString().split('T')[0]
    const movimientosHoy = ultimosParaStats.filter((m) => m.fechaMovimiento.split('T')[0] === hoy)
    const entradas = movimientosHoy.filter((m) => m.tipoMovimiento === 'ENTRADA' || m.tipoMovimiento === 'DEVOLUCION').length
    const salidas = movimientosHoy.filter((m) => m.tipoMovimiento === 'SALIDA' || m.tipoMovimiento === 'MERMA').length
    return { entradas, salidas, total: movimientosHoy.length }
  }, [ultimosParaStats])

  const kardexProducto = productos.find((p) => p.id === Number(kardexProductoId))

  return (
    <div>
      <header className="border-b bg-white p-3 shadow-sm md:p-4">
        <div className="flex items-center">
          <h1 className="flex items-center gap-2 text-xl font-bold md:text-2xl">
            <i className="bi bi-box-seam" /> Gestión de Inventario
          </h1>
          <button
            onClick={() => {
              setPrefillProductoId(undefined)
              setModalOpen(true)
            }}
            className="ml-auto flex items-center gap-2 rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover"
          >
            <i className="bi bi-plus-circle" /> Registrar Movimiento
          </button>
        </div>
      </header>

      <div className="p-4 md:p-6">
        <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div className="rounded-xl border-t-4 border-brand-success bg-white p-4 text-center shadow-sm">
            <i className="bi bi-arrow-up-circle-fill text-3xl text-brand-success" />
            <h6 className="mt-2 text-sm text-slate-500">Entradas Hoy</h6>
            <div className="text-xl font-bold text-brand-success">{stats.entradas}</div>
          </div>
          <div className="rounded-xl border-t-4 border-brand-danger bg-white p-4 text-center shadow-sm">
            <i className="bi bi-arrow-down-circle-fill text-3xl text-brand-danger" />
            <h6 className="mt-2 text-sm text-slate-500">Salidas Hoy</h6>
            <div className="text-xl font-bold text-brand-danger">{stats.salidas}</div>
          </div>
          <div className="rounded-xl border-t-4 border-amber-500 bg-white p-4 text-center shadow-sm">
            <i className="bi bi-exclamation-triangle-fill text-3xl text-amber-500" />
            <h6 className="mt-2 text-sm text-slate-500">Stock Bajo</h6>
            <div className="text-xl font-bold text-amber-500">{stockBajo.length}</div>
          </div>
          <div className="rounded-xl border-t-4 border-brand-primary bg-white p-4 text-center shadow-sm">
            <i className="bi bi-clock-history text-3xl text-brand-primary" />
            <h6 className="mt-2 text-sm text-slate-500">Movimientos Hoy</h6>
            <div className="text-xl font-bold text-brand-primary">{stats.total}</div>
          </div>
        </div>

        <div className="mb-4 flex gap-2 border-b border-slate-200">
          <button
            onClick={() => setTab('movimientos')}
            className={`flex items-center gap-2 border-b-[3px] px-5 py-2.5 text-sm font-semibold transition ${
              tab === 'movimientos' ? 'border-brand-primary text-brand-primary' : 'border-transparent text-slate-500 hover:text-brand-primary'
            }`}
          >
            <i className="bi bi-list-ul" /> Movimientos
          </button>
          <button
            onClick={() => setTab('kardex')}
            className={`flex items-center gap-2 border-b-[3px] px-5 py-2.5 text-sm font-semibold transition ${
              tab === 'kardex' ? 'border-brand-primary text-brand-primary' : 'border-transparent text-slate-500 hover:text-brand-primary'
            }`}
          >
            <i className="bi bi-journal-text" /> Kardex por Producto
          </button>
          <button
            onClick={() => setTab('stock')}
            className={`flex items-center gap-2 border-b-[3px] px-5 py-2.5 text-sm font-semibold transition ${
              tab === 'stock' ? 'border-brand-primary text-brand-primary' : 'border-transparent text-slate-500 hover:text-brand-primary'
            }`}
          >
            <i className="bi bi-exclamation-circle" /> Productos con Stock Bajo
          </button>
        </div>

        {tab === 'movimientos' && (
          <div className="rounded-xl bg-white p-4 shadow-sm md:p-6">
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-lg font-bold">Últimos Movimientos</h2>
              <div className="flex flex-wrap gap-1.5">
                {FILTROS.map((f) => (
                  <button
                    key={f.key}
                    onClick={() => setFiltroTipo(f.key)}
                    className={`rounded-lg border px-3 py-1.5 text-xs font-semibold transition ${
                      filtroTipo === f.key ? f.activeClass : 'border-slate-300 text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    {f.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full min-w-[900px] text-center text-sm">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-2 py-2">ID</th>
                    <th className="px-2 py-2">Fecha</th>
                    <th className="px-2 py-2">Producto</th>
                    <th className="px-2 py-2">Tipo</th>
                    <th className="px-2 py-2">Motivo</th>
                    <th className="px-2 py-2">Cantidad</th>
                    <th className="px-2 py-2">Stock Ant.</th>
                    <th className="px-2 py-2">Stock Nuevo</th>
                    <th className="px-2 py-2">Usuario</th>
                    <th className="px-2 py-2">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingMovimientos ? (
                    <tr>
                      <td colSpan={10} className="px-2 py-6 text-slate-500">
                        Cargando...
                      </td>
                    </tr>
                  ) : movimientos.length === 0 ? (
                    <tr>
                      <td colSpan={10} className="px-2 py-6 text-slate-500">
                        No se encontraron movimientos
                      </td>
                    </tr>
                  ) : (
                    movimientos.map((m) => {
                      const esEntrada = ES_ENTRADA.includes(m.tipoMovimiento)
                      return (
                        <tr key={m.id} className="border-t">
                          <td className="px-2 py-2">{m.id}</td>
                          <td className="px-2 py-2">{formatDateTime(m.fechaMovimiento)}</td>
                          <td className="px-2 py-2 text-left">{m.productoNombre}</td>
                          <td className="px-2 py-2">
                            <span className={`rounded-full px-2 py-0.5 text-xs font-semibold text-white ${TIPO_BADGE[m.tipoMovimiento]}`}>
                              {TIPO_LABEL[m.tipoMovimiento]}
                            </span>
                          </td>
                          <td className="px-2 py-2">{MOTIVO_LABEL[m.motivo] ?? m.motivo}</td>
                          <td className="px-2 py-2">
                            <span className={`rounded-full px-2 py-0.5 text-xs font-semibold text-white ${esEntrada ? 'bg-brand-success' : 'bg-brand-danger'}`}>
                              {esEntrada ? '+' : '-'}
                              {m.cantidad}
                            </span>
                          </td>
                          <td className="px-2 py-2">{m.stockAnterior}</td>
                          <td className="px-2 py-2 font-semibold">{m.stockNuevo}</td>
                          <td className="px-2 py-2">{m.usuarioNombre}</td>
                          <td className="px-2 py-2">
                            <button onClick={() => setDetalleMovimiento(m)} title="Ver detalle" className="rounded bg-sky-500 px-2 py-1.5 text-white hover:bg-sky-600">
                              <i className="bi bi-eye" />
                            </button>
                          </td>
                        </tr>
                      )
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {tab === 'kardex' && (
          <div className="rounded-xl bg-white p-4 shadow-sm md:p-6">
            <h2 className="mb-4 text-lg font-bold">Buscar Kardex por Producto</h2>
            <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-end">
              <div className="flex-1">
                <label htmlFor="kardexProducto" className="mb-1 block text-sm font-medium">
                  Seleccionar Producto:
                </label>
                <select
                  id="kardexProducto"
                  value={kardexProductoId}
                  onChange={(e) => setKardexProductoId(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
                >
                  <option value="">-- Seleccione un producto --</option>
                  {productos.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.nombre}
                    </option>
                  ))}
                </select>
              </div>
              <button
                onClick={handleBuscarKardex}
                disabled={loadingKardex}
                className="flex items-center justify-center gap-2 rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover disabled:opacity-60"
              >
                <i className="bi bi-search" /> {loadingKardex ? 'Buscando...' : 'Ver Kardex'}
              </button>
            </div>

            {kardexData && (
              <>
                {kardexProducto && (
                  <div className="mb-3 rounded-lg bg-sky-50 px-4 py-2 text-sm">
                    <strong>Producto:</strong> {kardexProducto.nombre} | <strong>Stock Actual:</strong> {kardexProducto.stock} unidades |{' '}
                    <strong>Total de movimientos:</strong> {kardexData.length}
                  </div>
                )}
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[800px] text-center text-sm">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-2 py-2">Fecha</th>
                        <th className="px-2 py-2">Tipo</th>
                        <th className="px-2 py-2">Motivo</th>
                        <th className="px-2 py-2">Cantidad</th>
                        <th className="px-2 py-2">Stock Anterior</th>
                        <th className="px-2 py-2">Stock Nuevo</th>
                        <th className="px-2 py-2">Referencia</th>
                        <th className="px-2 py-2">Usuario</th>
                      </tr>
                    </thead>
                    <tbody>
                      {kardexData.length === 0 ? (
                        <tr>
                          <td colSpan={8} className="px-2 py-6 text-slate-500">
                            Sin movimientos para este producto
                          </td>
                        </tr>
                      ) : (
                        kardexData.map((m) => {
                          const esEntrada = ES_ENTRADA.includes(m.tipoMovimiento)
                          let referencia = '-'
                          if (m.referenciaTipo === 'VENTA' && m.referenciaId) referencia = `Venta #${m.referenciaId}`
                          else if (m.referenciaTipo !== 'NINGUNO') referencia = m.referenciaTipo
                          return (
                            <tr key={m.id} className="border-t">
                              <td className="px-2 py-1.5">{formatDateTime(m.fechaMovimiento)}</td>
                              <td className="px-2 py-1.5">
                                <span className={`rounded-full px-2 py-0.5 text-xs font-semibold text-white ${TIPO_BADGE[m.tipoMovimiento]}`}>
                                  {TIPO_LABEL[m.tipoMovimiento]}
                                </span>
                              </td>
                              <td className="px-2 py-1.5">{MOTIVO_LABEL[m.motivo] ?? m.motivo}</td>
                              <td className="px-2 py-1.5">
                                <span className={esEntrada ? 'text-brand-success' : 'text-brand-danger'}>
                                  {esEntrada ? '+' : '-'}
                                  {m.cantidad}
                                </span>
                              </td>
                              <td className="px-2 py-1.5">{m.stockAnterior}</td>
                              <td className="px-2 py-1.5 font-semibold">{m.stockNuevo}</td>
                              <td className="px-2 py-1.5">{referencia}</td>
                              <td className="px-2 py-1.5">{m.usuarioNombre}</td>
                            </tr>
                          )
                        })
                      )}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </div>
        )}

        {tab === 'stock' && (
          <div className="rounded-xl bg-white p-4 shadow-sm md:p-6">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-bold">
              <i className="bi bi-exclamation-triangle-fill text-amber-500" /> Productos con Stock Bajo
            </h2>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[600px] text-center text-sm">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-2 py-2">ID</th>
                    <th className="px-2 py-2">Producto</th>
                    <th className="px-2 py-2">Stock Actual</th>
                    <th className="px-2 py-2">Stock Mínimo</th>
                    <th className="px-2 py-2">Estado</th>
                    <th className="px-2 py-2">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {stockBajo.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="px-2 py-6 text-slate-500">
                        Sin productos con stock bajo
                      </td>
                    </tr>
                  ) : (
                    stockBajo.map((p) => (
                      <tr key={p.id} className="border-t">
                        <td className="px-2 py-2">{p.id}</td>
                        <td className="px-2 py-2 text-left">{p.nombre}</td>
                        <td className="px-2 py-2">{p.stock}</td>
                        <td className="px-2 py-2">{p.stockMinimo}</td>
                        <td className="px-2 py-2">
                          <span className={`rounded-full px-2 py-0.5 text-xs font-semibold text-white ${p.stock === 0 ? 'bg-brand-danger' : 'bg-amber-500'}`}>
                            {p.stock === 0 ? 'Sin stock' : 'Stock bajo'}
                          </span>
                        </td>
                        <td className="px-2 py-2">
                          <button
                            onClick={() => handleReponerStock(p)}
                            title="Reponer stock"
                            className="flex items-center gap-1 rounded-lg bg-brand-success px-3 py-1.5 text-xs font-medium text-white transition hover:bg-emerald-600"
                          >
                            <i className="bi bi-plus-circle" /> Reponer
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {modalOpen && (
        <RegistrarMovimientoModal
          productos={productos}
          prefillProductoId={prefillProductoId}
          onClose={() => setModalOpen(false)}
          onSaved={refrescarTodo}
        />
      )}
      {detalleMovimiento && <DetalleMovimientoModal movimiento={detalleMovimiento} onClose={() => setDetalleMovimiento(null)} />}
    </div>
  )
}
