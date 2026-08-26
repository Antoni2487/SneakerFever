import { useEffect, useState } from 'react'
import Swal from 'sweetalert2'
import Modal from '../ui/Modal'
import { apiClient, ApiError } from '../../lib/apiClient'
import type { CuotaPago, EstadoCuota } from '../../types'

const ESTADO_BADGE: Record<EstadoCuota, string> = {
  PENDIENTE: 'bg-amber-500',
  PAGADA: 'bg-brand-success',
  PARCIAL: 'bg-sky-500',
  VENCIDA: 'bg-brand-danger',
}

function formatPrice(value: number) {
  return `S/ ${value.toFixed(2)}`
}

function formatDate(date: string | null) {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('es-PE')
}

export default function CuotasModal({ creditoId, onClose }: { creditoId: number; onClose: () => void }) {
  const [cuotas, setCuotas] = useState<CuotaPago[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    apiClient
      .get<CuotaPago[]>(`/creditos/api/${creditoId}/cuotas`)
      .then((res) => setCuotas((res.data as CuotaPago[]) ?? []))
      .catch((error) =>
        Swal.fire({
          title: error instanceof ApiError ? error.message : 'No se pudieron cargar las cuotas',
          icon: 'error',
          toast: true,
          position: 'top-end',
          showConfirmButton: false,
          timer: 3000,
        }),
      )
      .finally(() => setLoading(false))
  }, [creditoId])

  return (
    <Modal title="Cronograma de Cuotas" onClose={onClose}>
      {loading ? (
        <p className="py-8 text-center text-slate-500">Cargando...</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[600px] text-center text-sm">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-2 py-2">Cuota</th>
                <th className="px-2 py-2">Vencimiento</th>
                <th className="px-2 py-2 text-right">Monto</th>
                <th className="px-2 py-2 text-right">Pagado</th>
                <th className="px-2 py-2 text-right">Saldo</th>
                <th className="px-2 py-2">Estado</th>
                <th className="px-2 py-2">Fecha Pago</th>
              </tr>
            </thead>
            <tbody>
              {cuotas.map((c) => (
                <tr key={c.id} className={`border-t ${c.estado === 'VENCIDA' ? 'bg-red-50' : ''}`}>
                  <td className="px-2 py-1.5">{c.numero_cuota}</td>
                  <td className="px-2 py-1.5">
                    {formatDate(c.fecha_vencimiento)}
                    {c.estado === 'PENDIENTE' && c.dias_para_vencer != null && (
                      <span className="ml-1 text-xs text-slate-400">({c.dias_para_vencer} días)</span>
                    )}
                    {c.estado === 'VENCIDA' && c.dias_vencida != null && (
                      <span className="ml-1 text-xs text-brand-danger">(+{c.dias_vencida} días)</span>
                    )}
                  </td>
                  <td className="px-2 py-1.5 text-right">{formatPrice(c.monto_cuota)}</td>
                  <td className="px-2 py-1.5 text-right">{formatPrice(c.monto_pagado)}</td>
                  <td className="px-2 py-1.5 text-right font-semibold">{formatPrice(c.saldo_pendiente)}</td>
                  <td className="px-2 py-1.5">
                    <span className={`rounded-full px-2 py-0.5 text-xs font-semibold text-white ${ESTADO_BADGE[c.estado]}`}>{c.estado}</span>
                  </td>
                  <td className="px-2 py-1.5">{formatDate(c.fecha_pago)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Modal>
  )
}
