import { useEffect, useState } from 'react'
import Swal from 'sweetalert2'
import { apiClient, ApiError } from '../../lib/apiClient'
import type { CreditoVenta, EstadoCredito } from '../../types'
import PagarCuotaModal from './PagarCuotaModal'
import CuotasModal from './CuotasModal'

type Filtro = 'activos' | 'vencidos' | 'proximos'

const ESTADO_BADGE: Record<EstadoCredito, string> = {
  ACTIVO: 'bg-brand-success',
  PAGADO: 'bg-sky-500',
  VENCIDO: 'bg-brand-danger',
  CANCELADO: 'bg-slate-400',
}

function formatPrice(value: number) {
  return `S/ ${value.toFixed(2)}`
}

function progresoColor(porcentaje: number) {
  if (porcentaje >= 75) return 'bg-brand-success'
  if (porcentaje >= 50) return 'bg-amber-500'
  return 'bg-brand-danger'
}

const ENDPOINTS: Record<Filtro, string> = {
  activos: '/creditos/api/activos',
  vencidos: '/creditos/api/vencidos',
  proximos: '/creditos/api/proximos-vencer?dias=7',
}

export default function CreditosTab() {
  const [filtro, setFiltro] = useState<Filtro>('activos')
  const [creditos, setCreditos] = useState<CreditoVenta[]>([])
  const [loading, setLoading] = useState(true)
  const [cuotasId, setCuotasId] = useState<number | null>(null)
  const [pagarId, setPagarId] = useState<number | null>(null)

  // Los tres contadores/montos de las tarjetas se calculan client-side sumando cada
  // lista real (activos/vencidos/proximos), en vez de usar GET /creditos/api/reporte:
  // ese endpoint devuelve ReporteCreditosDTO (total_creditos, creditos_activos, etc.)
  // con nombres que no coinciden con lo que necesitan las tarjetas — sumar las listas
  // reales es más simple y evita depender de ese desajuste.
  const [resumen, setResumen] = useState<Record<Filtro, { count: number; monto: number }>>({
    activos: { count: 0, monto: 0 },
    vencidos: { count: 0, monto: 0 },
    proximos: { count: 0, monto: 0 },
  })

  useEffect(() => {
    loadResumen()
  }, [])

  useEffect(() => {
    loadCreditos(filtro)
  }, [filtro])

  async function loadResumen() {
    try {
      const [activos, vencidos, proximos] = await Promise.all([
        apiClient.get<CreditoVenta[]>(ENDPOINTS.activos),
        apiClient.get<CreditoVenta[]>(ENDPOINTS.vencidos),
        apiClient.get<CreditoVenta[]>(ENDPOINTS.proximos),
      ])
      const sum = (list: CreditoVenta[]) => list.reduce((acc, c) => acc + c.saldo_pendiente, 0)
      setResumen({
        activos: { count: (activos.data as CreditoVenta[])?.length ?? 0, monto: sum((activos.data as CreditoVenta[]) ?? []) },
        vencidos: { count: (vencidos.data as CreditoVenta[])?.length ?? 0, monto: sum((vencidos.data as CreditoVenta[]) ?? []) },
        proximos: { count: (proximos.data as CreditoVenta[])?.length ?? 0, monto: sum((proximos.data as CreditoVenta[]) ?? []) },
      })
    } catch {
      // El resumen es informativo; si falla, las tarjetas quedan en 0 sin bloquear la tabla.
    }
  }

  async function loadCreditos(tipo: Filtro) {
    setLoading(true)
    try {
      const res = await apiClient.get<CreditoVenta[]>(ENDPOINTS[tipo])
      setCreditos((res.data as CreditoVenta[]) ?? [])
    } catch (error) {
      Swal.fire({
        title: error instanceof ApiError ? error.message : 'Error al cargar los créditos',
        icon: 'error',
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
      })
    } finally {
      setLoading(false)
    }
  }

  function refrescarTodo() {
    loadResumen()
    loadCreditos(filtro)
  }

  const cards: { key: Filtro; label: string; icon: string; color: string }[] = [
    { key: 'activos', label: 'Créditos Activos', icon: 'bi-check-circle-fill', color: 'success' },
    { key: 'vencidos', label: 'Créditos Vencidos', icon: 'bi-exclamation-circle-fill', color: 'danger' },
    { key: 'proximos', label: 'Próximos a Vencer', icon: 'bi-clock-fill', color: 'warning' },
  ]

  return (
    <div>
      <div className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
        {cards.map((card) => {
          const active = filtro === card.key
          const data = resumen[card.key]
          const colorClass =
            card.color === 'success' ? 'text-brand-success border-brand-success' : card.color === 'danger' ? 'text-brand-danger border-brand-danger' : 'text-amber-500 border-amber-500'
          return (
            <button
              key={card.key}
              onClick={() => setFiltro(card.key)}
              className={`rounded-xl border-2 bg-white p-5 text-center shadow-sm transition hover:-translate-y-1 hover:shadow-md ${
                active ? colorClass : 'border-transparent'
              }`}
            >
              <i className={`bi ${card.icon} text-3xl ${colorClass.split(' ')[0]}`} />
              <h6 className="mt-2 text-sm text-slate-500">{card.label}</h6>
              <div className={`my-1 text-xl font-bold ${colorClass.split(' ')[0]}`}>{formatPrice(data.monto)}</div>
              <div className="text-xs text-slate-400">{data.count} créditos</div>
            </button>
          )
        })}
      </div>

      <div className="rounded-xl bg-white p-4 shadow-sm md:p-6">
        <h4 className="mb-4 text-lg font-bold">
          <i className="bi bi-list-check me-2" />
          {filtro === 'activos' ? 'Créditos Activos' : filtro === 'vencidos' ? 'Créditos Vencidos' : 'Próximos a Vencer'}
        </h4>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[900px] text-center text-sm">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-2 py-2">ID</th>
                <th className="px-2 py-2">Comprobante</th>
                <th className="px-2 py-2">Cliente</th>
                <th className="px-2 py-2 text-right">Monto Total</th>
                <th className="px-2 py-2 text-right">Pagado</th>
                <th className="px-2 py-2 text-right">Saldo</th>
                <th className="px-2 py-2">Progreso</th>
                <th className="px-2 py-2">Estado</th>
                <th className="px-2 py-2">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={9} className="px-2 py-6 text-slate-500">
                    Cargando...
                  </td>
                </tr>
              ) : creditos.length === 0 ? (
                <tr>
                  <td colSpan={9} className="px-2 py-6 text-slate-500">
                    No se encontraron créditos
                  </td>
                </tr>
              ) : (
                creditos.map((c) => {
                  const porcentaje = Math.round(c.porcentaje_pagado || 0)
                  const puedePagar = c.estado === 'ACTIVO' && c.saldo_pendiente > 0
                  return (
                    <tr key={c.id} className="border-t">
                      <td className="px-2 py-2">{c.id}</td>
                      <td className="px-2 py-2">{c.comprobante_completo}</td>
                      <td className="px-2 py-2 text-left">
                        {c.cliente_nombre}
                        <br />
                        <small className="text-slate-400">{c.cliente_documento}</small>
                      </td>
                      <td className="px-2 py-2 text-right">{formatPrice(c.monto_con_interes)}</td>
                      <td className="px-2 py-2 text-right">{formatPrice(c.monto_pagado)}</td>
                      <td className="px-2 py-2 text-right font-semibold">{formatPrice(c.saldo_pendiente)}</td>
                      <td className="px-2 py-2">
                        <div className="h-5 w-full overflow-hidden rounded-full bg-slate-200">
                          <div className={`flex h-5 items-center justify-center text-[10px] text-white ${progresoColor(porcentaje)}`} style={{ width: `${porcentaje}%` }}>
                            {porcentaje}%
                          </div>
                        </div>
                      </td>
                      <td className="px-2 py-2">
                        <span className={`rounded-full px-2 py-0.5 text-xs font-semibold text-white ${ESTADO_BADGE[c.estado]}`}>{c.estado}</span>
                      </td>
                      <td className="px-2 py-2">
                        <div className="flex items-center justify-center gap-1.5">
                          <button onClick={() => setCuotasId(c.id)} title="Ver cuotas" className="rounded bg-brand-primary px-2 py-1.5 text-white hover:bg-brand-primary-hover">
                            <i className="bi bi-calendar-check" />
                          </button>
                          <button
                            onClick={() => setPagarId(c.id)}
                            disabled={!puedePagar}
                            title={puedePagar ? 'Registrar pago' : 'No hay cuotas pendientes'}
                            className="rounded bg-brand-success px-2 py-1.5 text-white hover:bg-emerald-600 disabled:opacity-40"
                          >
                            <i className="bi bi-cash" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {cuotasId !== null && <CuotasModal creditoId={cuotasId} onClose={() => setCuotasId(null)} />}
      {pagarId !== null && (
        <PagarCuotaModal
          creditoId={pagarId}
          onClose={() => setPagarId(null)}
          onPaid={refrescarTodo}
        />
      )}
    </div>
  )
}
