import { useState } from 'react'
import type { FormEvent } from 'react'
import Swal from 'sweetalert2'
import { apiClient, ApiError } from '../../lib/apiClient'

const EMPTY_FORM = { nombre: '', correo: '', asunto: '', mensaje: '' }

function notify(message: string, icon: 'success' | 'error') {
  Swal.fire({
    title: message,
    icon,
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 4000,
    timerProgressBar: true,
  })
}

export default function Contacto() {
  const [form, setForm] = useState(EMPTY_FORM)
  const [enviando, setEnviando] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setEnviando(true)
    try {
      const res = await apiClient.post<{ message: string }>('/api/public/contacto', form)
      notify((res.message as string) ?? 'Mensaje enviado', 'success')
      setForm(EMPTY_FORM)
    } catch (err) {
      notify(err instanceof ApiError ? err.message : 'No se pudo enviar el mensaje', 'error')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className="mx-auto max-w-6xl px-4 py-12 lg:px-8">
      <div className="grid grid-cols-1 gap-12 lg:grid-cols-[1fr_1.4fr]">
        <div>
          <h1 className="text-2xl font-black tracking-tight">Ponte en contacto</h1>
          <p className="mt-3 text-sm leading-relaxed text-slate-500">
            ¿Tienes dudas sobre tu pedido o nuestros productos? Escríbenos y te responderemos a la brevedad.
          </p>

          <div className="mt-8 space-y-6">
            <div className="flex gap-4">
              <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-black text-white">
                <i className="bi bi-geo-alt" />
              </div>
              <div>
                <h3 className="text-sm font-bold">Visítanos</h3>
                <p className="text-sm text-slate-500">
                  Av. Larco 123, Miraflores
                  <br />
                  Lima, Perú
                </p>
              </div>
            </div>

            <div className="flex gap-4">
              <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-black text-white">
                <i className="bi bi-telephone" />
              </div>
              <div>
                <h3 className="text-sm font-bold">Llámanos</h3>
                <p className="text-sm text-slate-500">
                  +51 999 888 777
                  <br />
                  Lun-Dom 9am a 9pm
                </p>
              </div>
            </div>

            <div className="flex gap-4">
              <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-black text-white">
                <i className="bi bi-envelope" />
              </div>
              <div>
                <h3 className="text-sm font-bold">Escríbenos</h3>
                <p className="text-sm text-slate-500">contacto@sneakersfever.com</p>
              </div>
            </div>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Nombre completo</label>
              <input
                type="text"
                required
                value={form.nombre}
                onChange={(e) => setForm({ ...form, nombre: e.target.value })}
                placeholder="Juan Pérez"
                className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-black focus:outline-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Correo electrónico</label>
              <input
                type="email"
                required
                value={form.correo}
                onChange={(e) => setForm({ ...form, correo: e.target.value })}
                placeholder="juan@email.com"
                className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-black focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Asunto</label>
            <input
              type="text"
              required
              value={form.asunto}
              onChange={(e) => setForm({ ...form, asunto: e.target.value })}
              placeholder="Consulta sobre pedido..."
              className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-black focus:outline-none"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Mensaje</label>
            <textarea
              required
              rows={5}
              value={form.mensaje}
              onChange={(e) => setForm({ ...form, mensaje: e.target.value })}
              placeholder="Escribe tu mensaje aquí..."
              className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm focus:border-black focus:outline-none"
            />
          </div>

          <button
            type="submit"
            disabled={enviando}
            className="flex w-full items-center justify-center gap-2 rounded-full bg-black py-3.5 text-sm font-bold uppercase tracking-wide text-white transition hover:bg-slate-800 disabled:opacity-60"
          >
            {enviando ? 'Enviando...' : 'Enviar mensaje'} <i className="bi bi-send" />
          </button>
        </form>
      </div>
    </main>
  )
}
