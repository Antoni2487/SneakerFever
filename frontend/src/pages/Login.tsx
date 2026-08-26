import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import Swal from 'sweetalert2'
import { useAuth } from '../context/AuthContext'
import { ApiError } from '../lib/apiClient'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [usuario, setUsuario] = useState('')
  const [clave, setClave] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!usuario || !clave) {
      Swal.fire({
        icon: 'warning',
        title: 'Campos incompletos',
        text: 'Por favor, ingresa tu usuario y contraseña.',
        confirmButtonColor: '#007bff',
      })
      return
    }

    setSubmitting(true)
    try {
      await login(usuario, clave)
      navigate('/categorias', { replace: true })
    } catch (error) {
      const message = error instanceof ApiError ? error.message : 'Error de Acceso'
      Swal.fire({ icon: 'error', title: 'Error de Acceso', text: message, confirmButtonColor: '#dc3545' })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#f8f9fa] font-sans">
      <div className="w-full max-w-md rounded-xl bg-white p-8 shadow-[0_10px_15px_-3px_rgba(0,0,0,0.1),0_4px_6px_-4px_rgba(0,0,0,0.1)]">
        <div className="mb-4 text-center">
          <h1 className="mb-3 text-2xl font-bold">Acceso App</h1>
          <p className="text-slate-500">Por favor, inicia sesión para continuar</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="mb-3">
            <label htmlFor="usuario" className="mb-1 block text-sm font-medium">
              Usuario
            </label>
            <input
              id="usuario"
              type="text"
              autoFocus
              value={usuario}
              onChange={(e) => setUsuario(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-4 py-2.5 text-base focus:border-brand-primary focus:outline-none focus:ring-4 focus:ring-brand-primary/25"
            />
          </div>

          <div className="mb-4">
            <label htmlFor="clave" className="mb-1 block text-sm font-medium">
              Contraseña
            </label>
            <input
              id="clave"
              type="password"
              value={clave}
              onChange={(e) => setClave(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-4 py-2.5 text-base focus:border-brand-primary focus:outline-none focus:ring-4 focus:ring-brand-primary/25"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-brand-primary py-2.5 text-base font-medium text-white transition hover:bg-brand-primary-hover disabled:opacity-60"
          >
            {submitting ? 'Ingresando...' : 'Iniciar Sesión'}
          </button>
        </form>
      </div>
    </div>
  )
}
