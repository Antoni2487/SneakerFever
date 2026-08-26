import { useEffect, useMemo, useState } from 'react'
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { apiClient, ApiError } from '../lib/apiClient'
import type { DashboardResumen, Product, ProductoMasVendido, ReporteVentas, Venta, VentaPorDia } from '../types'

const COLOR_PRIMARY = '#007bff'
const COLOR_SUCCESS = '#28a745'
const COLOR_DANGER = '#dc3545'
const COLOR_WARNING = '#f59e0b'
const COLOR_INFO = '#0ea5e9'

const PERIODOS = [7, 30, 90] as const
type Periodo = (typeof PERIODOS)[number]

function formatPrice(value: number) {
  return `S/ ${value.toFixed(2)}`
}

function formatShortDate(value: string) {
  const [, month, day] = value.split('-')
  return `${day}/${month}`
}

function isoDateTime(date: Date) {
  return date.toISOString().slice(0, 19)
}

interface KpiCardProps {
  icon: string
  label: string
  value: string
  color: string
  sub?: string
}

function KpiCard({ icon, label, value, color, sub }: KpiCardProps) {
  return (
    <div className="rounded-xl border-t-4 bg-white p-4 shadow-sm" style={{ borderTopColor: color }}>
      <div className="flex items-center gap-3">
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-lg text-white" style={{ backgroundColor: color }}>
          <i className={`bi ${icon}`} />
        </div>
        <div className="min-w-0">
          <div className="truncate text-xs font-medium text-slate-500">{label}</div>
          <div className="truncate text-lg font-bold" style={{ color }}>
            {value}
          </div>
          {sub && <div className="truncate text-xs text-slate-400">{sub}</div>}
        </div>
      </div>
    </div>
  )
}

function Panel({ title, icon, children }: { title: string; icon: string; children: React.ReactNode }) {
  return (
    <div className="rounded-xl bg-white p-4 shadow-sm md:p-5">
      <h3 className="mb-4 flex items-center gap-2 text-sm font-bold text-slate-700">
        <i className={`bi ${icon} text-brand-primary`} />
        {title}
      </h3>
      {children}
    </div>
  )
}

export default function Dashboard() {
  const [loading, setLoading] = useState(true)
  const [periodo, setPeriodo] = useState<Periodo>(7)

  const [resumen, setResumen] = useState<DashboardResumen | null>(null)
  const [tendencia, setTendencia] = useState<VentaPorDia[]>([])
  const [topProductos, setTopProductos] = useState<ProductoMasVendido[]>([])
  const [reporte, setReporte] = useState<ReporteVentas | null>(null)
  const [stockBajo, setStockBajo] = useState<Product[]>([])
  const [ventasRecientes, setVentasRecientes] = useState<Venta[]>([])

  useEffect(() => {
    loadAll(periodo)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    loadPeriodo(periodo)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [periodo])

  async function loadPeriodo(dias: Periodo) {
    const fechaFin = new Date()
    const fechaInicio = new Date()
    fechaInicio.setDate(fechaFin.getDate() - (dias - 1))
    fechaInicio.setHours(0, 0, 0, 0)

    try {
      const [tendenciaRes, reporteRes] = await Promise.all([
        apiClient.get<VentaPorDia[]>(`/admin/api/ventas-tendencia?dias=${dias}`),
        apiClient.get<ReporteVentas>(`/ventas/api/reporte?fechaInicio=${isoDateTime(fechaInicio)}&fechaFin=${isoDateTime(fechaFin)}`),
      ])
      setTendencia((tendenciaRes.data as VentaPorDia[]) ?? [])
      setReporte((reporteRes.data as ReporteVentas) ?? null)
    } catch {
      // Si falla, los gráficos del período simplemente quedan vacíos.
    }
  }

  async function loadAll(dias: Periodo) {
    setLoading(true)
    try {
      const [resumenRes, topRes, stockRes, ventasRes] = await Promise.all([
        apiClient.get<DashboardResumen>('/admin/api/resumen'),
        apiClient.get<ProductoMasVendido[]>('/admin/api/productos-mas-vendidos?limite=5'),
        apiClient.get<Product[]>('/productos/api/stock-bajo?limite=10'),
        apiClient.get<Venta[]>('/ventas/api/listar'),
      ])
      setResumen((resumenRes.data as DashboardResumen) ?? null)
      setTopProductos((topRes.data as ProductoMasVendido[]) ?? [])
      setStockBajo((stockRes.data as Product[]) ?? [])
      const ventas = (ventasRes.data as Venta[]) ?? []
      setVentasRecientes(
        [...ventas].sort((a, b) => new Date(b.fecha_creacion).getTime() - new Date(a.fecha_creacion).getTime()).slice(0, 6),
      )
      await loadPeriodo(dias)
    } catch (error) {
      // Errores puntuales por widget no deben tumbar todo el dashboard.
      console.error(error instanceof ApiError ? error.message : error)
    } finally {
      setLoading(false)
    }
  }

  const tendenciaChartData = useMemo(
    () => tendencia.map((t) => ({ fecha: formatShortDate(t.fecha), total: t.total, ventas: t.cantidad_ventas })),
    [tendencia],
  )

  const pagoChartData = useMemo(() => {
    if (!reporte) return []
    return [
      { name: 'Contado', value: reporte.ventas_contado, color: COLOR_SUCCESS },
      { name: 'Crédito', value: reporte.ventas_credito, color: COLOR_WARNING },
    ].filter((d) => d.value > 0)
  }, [reporte])

  const topProductosChartData = useMemo(
    () => [...topProductos].reverse().map((p) => ({ nombre: p.producto_nombre, cantidad: p.cantidad_vendida })),
    [topProductos],
  )

  if (loading) {
    return <div className="flex min-h-[60vh] items-center justify-center text-slate-500">Cargando dashboard...</div>
  }

  return (
    <div>
      <header className="border-b bg-white p-3 shadow-sm md:p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-xl font-bold md:text-2xl">Dashboard</h1>
          <div className="flex gap-1 rounded-lg bg-slate-100 p-1">
            {PERIODOS.map((p) => (
              <button
                key={p}
                onClick={() => setPeriodo(p)}
                className={`rounded-md px-3 py-1.5 text-xs font-semibold transition ${
                  periodo === p ? 'bg-white text-brand-primary shadow-sm' : 'text-slate-500 hover:text-slate-700'
                }`}
              >
                {p} días
              </button>
            ))}
          </div>
        </div>
      </header>

      <div className="space-y-5 p-4 md:p-6">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          <KpiCard icon="bi-calendar-day" label="Ventas Hoy" value={formatPrice(resumen?.ventas.ventasHoy ?? 0)} color={COLOR_PRIMARY} />
          <KpiCard icon="bi-calendar-week" label="Ventas Semana" value={formatPrice(resumen?.ventas.ventasSemana ?? 0)} color={COLOR_SUCCESS} />
          <KpiCard icon="bi-calendar-month" label="Ventas Mes" value={formatPrice(resumen?.ventas.ventasMes ?? 0)} color={COLOR_INFO} />
          <KpiCard icon="bi-receipt" label="Ventas Totales" value={String(resumen?.ventas.totalVentas ?? 0)} color={COLOR_WARNING} />
          <KpiCard
            icon="bi-credit-card"
            label="Créditos por Cobrar"
            value={formatPrice(resumen?.creditos.saldo_pendiente_total ?? 0)}
            color={COLOR_DANGER}
            sub={`${resumen?.creditos.creditos_activos ?? 0} activos`}
          />
          <KpiCard
            icon="bi-person-badge-fill"
            label="Clientes Activos"
            value={String(resumen?.clientes.activos ?? 0)}
            color={COLOR_PRIMARY}
          />
          <KpiCard
            icon="bi-box-seam-fill"
            label="Productos Activos"
            value={String(resumen?.productos.activos ?? 0)}
            color={COLOR_SUCCESS}
            sub={resumen?.stockBajo ? `${resumen.stockBajo} con stock bajo` : undefined}
          />
          <KpiCard icon="bi-people-fill" label="Usuarios" value={String(resumen?.totalUsuarios ?? 0)} color={COLOR_INFO} />
        </div>

        <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <Panel title={`Tendencia de Ventas (${periodo} días)`} icon="bi-graph-up-arrow">
              <ResponsiveContainer width="100%" height={280}>
                <AreaChart data={tendenciaChartData} margin={{ top: 5, right: 10, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="ventasGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor={COLOR_PRIMARY} stopOpacity={0.35} />
                      <stop offset="95%" stopColor={COLOR_PRIMARY} stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                  <XAxis dataKey="fecha" tick={{ fontSize: 12, fill: '#64748b' }} axisLine={{ stroke: '#e2e8f0' }} tickLine={false} />
                  <YAxis tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} width={70} tickFormatter={(v) => `S/${v}`} />
                  <Tooltip
                    formatter={(value, name) => [name === 'total' ? formatPrice(Number(value)) : Number(value), name === 'total' ? 'Total' : 'Ventas']}
                    contentStyle={{ borderRadius: 8, border: '1px solid #e2e8f0', fontSize: 13 }}
                  />
                  <Area type="monotone" dataKey="total" stroke={COLOR_PRIMARY} strokeWidth={2} fill="url(#ventasGradient)" />
                </AreaChart>
              </ResponsiveContainer>
            </Panel>
          </div>

          <Panel title="Contado vs Crédito" icon="bi-pie-chart-fill">
            {pagoChartData.length === 0 ? (
              <div className="flex h-[280px] items-center justify-center text-sm text-slate-400">Sin ventas en el período</div>
            ) : (
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie data={pagoChartData} dataKey="value" nameKey="name" innerRadius={60} outerRadius={95} paddingAngle={3}>
                    {pagoChartData.map((entry) => (
                      <Cell key={entry.name} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ borderRadius: 8, border: '1px solid #e2e8f0', fontSize: 13 }} />
                  <Legend iconType="circle" wrapperStyle={{ fontSize: 13 }} />
                </PieChart>
              </ResponsiveContainer>
            )}
          </Panel>
        </div>

        <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
          <div className="lg:col-span-1">
            <Panel title="Top 5 Productos Vendidos" icon="bi-trophy-fill">
              {topProductosChartData.length === 0 ? (
                <div className="flex h-[260px] items-center justify-center text-sm text-slate-400">Sin ventas todavía</div>
              ) : (
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={topProductosChartData} layout="vertical" margin={{ top: 5, right: 20, left: 10, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#e2e8f0" />
                    <XAxis type="number" tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} allowDecimals={false} />
                    <YAxis
                      type="category"
                      dataKey="nombre"
                      tick={{ fontSize: 12, fill: '#334155' }}
                      axisLine={false}
                      tickLine={false}
                      width={110}
                    />
                    <Tooltip formatter={(value) => [Number(value), 'Unidades vendidas']} contentStyle={{ borderRadius: 8, border: '1px solid #e2e8f0', fontSize: 13 }} />
                    <Bar dataKey="cantidad" fill={COLOR_SUCCESS} radius={[0, 6, 6, 0]} barSize={18} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Panel>
          </div>

          <div className="lg:col-span-1">
            <Panel title="Alertas de Stock Bajo" icon="bi-exclamation-triangle-fill">
              {stockBajo.length === 0 ? (
                <div className="flex h-[260px] flex-col items-center justify-center text-center text-sm text-slate-400">
                  <i className="bi bi-check-circle text-3xl text-brand-success" />
                  <p className="mt-2">Sin alertas de stock</p>
                </div>
              ) : (
                <div className="max-h-[260px] space-y-2 overflow-y-auto">
                  {stockBajo.map((p) => (
                    <div key={p.id} className="flex items-center justify-between rounded-lg bg-red-50 px-3 py-2 text-sm">
                      <span className="truncate pr-2">{p.nombre}</span>
                      <span className="shrink-0 rounded-full bg-brand-danger px-2 py-0.5 text-xs font-semibold text-white">{p.stock} und.</span>
                    </div>
                  ))}
                </div>
              )}
            </Panel>
          </div>

          <div className="lg:col-span-1">
            <Panel title="Ventas Recientes" icon="bi-clock-history">
              {ventasRecientes.length === 0 ? (
                <div className="flex h-[260px] items-center justify-center text-sm text-slate-400">Sin ventas recientes</div>
              ) : (
                <div className="max-h-[260px] space-y-2 overflow-y-auto">
                  {ventasRecientes.map((v) => (
                    <div key={v.id} className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-sm">
                      <div className="min-w-0">
                        <div className="truncate font-medium">{v.cliente_nombre}</div>
                        <div className="text-xs text-slate-400">{v.comprobante_completo}</div>
                      </div>
                      <span className="shrink-0 font-semibold text-brand-primary">{formatPrice(v.total)}</span>
                    </div>
                  ))}
                </div>
              )}
            </Panel>
          </div>
        </div>
      </div>
    </div>
  )
}
