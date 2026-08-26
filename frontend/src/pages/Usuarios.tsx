import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import Swal from 'sweetalert2'
import Modal from '../components/ui/Modal'
import { apiClient, ApiError } from '../lib/apiClient'
import { useAuth } from '../context/AuthContext'
import type { Perfil, Usuario, UsuarioFormData } from '../types'

const EMPTY_FORM: UsuarioFormData = { nombre: '', usuario: '', clave: '', correo: '', perfilId: '' }
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function notify(message: string, icon: 'success' | 'error' | 'info' = 'info') {
  Swal.fire({
    title: message,
    icon,
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 3000,
    timerProgressBar: true,
  })
}

export default function Usuarios() {
  const { usuario: yo } = useAuth()
  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [perfiles, setPerfiles] = useState<Perfil[]>([])
  const [totalAdmins, setTotalAdmins] = useState(0)
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [form, setForm] = useState<UsuarioFormData>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    loadUsuarios()
    loadPerfiles()
  }, [])

  async function loadUsuarios() {
    setLoading(true)
    try {
      const res = await apiClient.get<Usuario[]>('/usuarios/api/listar')
      setUsuarios((res.data as Usuario[]) ?? [])
      setTotalAdmins((res.totalAdmins as number) ?? 0)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar usuarios', 'error')
    } finally {
      setLoading(false)
    }
  }

  async function loadPerfiles() {
    try {
      const res = await apiClient.get<Perfil[]>('/usuarios/api/perfiles')
      setPerfiles((res.data as Perfil[]) ?? [])
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar perfiles', 'error')
    }
  }

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return usuarios
    return usuarios.filter(
      (u) => u.nombre.toLowerCase().includes(term) || u.usuario.toLowerCase().includes(term) || u.correo.toLowerCase().includes(term),
    )
  }, [usuarios, search])

  function isAdmin(u: Usuario) {
    return u.perfil?.nombre === 'Administrador'
  }

  function openCreateModal() {
    setEditId(null)
    setForm(EMPTY_FORM)
    setFieldErrors({})
    setModalOpen(true)
  }

  function openEditModal(u: Usuario) {
    setEditId(u.id)
    setForm({ nombre: u.nombre, usuario: u.usuario, clave: '', correo: u.correo, perfilId: u.perfil ? String(u.perfil.id) : '' })
    setFieldErrors({})
    setModalOpen(true)
    if (yo && u.id === yo.id) {
      notify('Estás editando tu propio perfil. No podrás reducir tu propio nivel de acceso.', 'info')
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const errors: Record<string, string> = {}
    if (!form.nombre.trim()) errors.nombre = 'El nombre es obligatorio'
    else if (form.nombre.trim().length < 2) errors.nombre = 'El nombre debe tener al menos 2 caracteres'
    if (!form.usuario.trim()) errors.usuario = 'El usuario es obligatorio'
    else if (form.usuario.trim().length < 3) errors.usuario = 'El usuario debe tener al menos 3 caracteres'
    if (!form.perfilId) errors.perfilId = 'Debe seleccionar un perfil'
    if (!form.correo.trim()) errors.correo = 'El correo es obligatorio'
    else if (!EMAIL_RE.test(form.correo.trim())) errors.correo = 'El formato del correo no es válido'
    if (!editId && !form.clave) errors.clave = 'La contraseña es obligatoria'
    else if (form.clave && form.clave.length < 6) errors.clave = 'La contraseña debe tener al menos 6 caracteres'

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    setSaving(true)
    setFieldErrors({})
    try {
      const payload: Record<string, unknown> = {
        id: editId,
        nombre: form.nombre.trim(),
        usuario: form.usuario.trim(),
        correo: form.correo.trim(),
        perfilId: parseInt(form.perfilId, 10),
      }
      if (!editId || form.clave) {
        payload.clave = form.clave
      }

      const res = await apiClient.post('/usuarios/api/guardar', payload)
      notify((res.message as string) ?? 'Usuario guardado correctamente', 'success')
      setModalOpen(false)
      await loadUsuarios()
    } catch (error) {
      if (error instanceof ApiError && error.errors) {
        setFieldErrors(error.errors)
      } else {
        notify(error instanceof ApiError ? error.message : 'Error al guardar el usuario', 'error')
      }
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus(u: Usuario) {
    const isActive = u.estado === 1
    if (isActive) {
      const result = await Swal.fire({
        title: '¿Estás seguro?',
        text: '¡El usuario se desactivará y no podrá iniciar sesión!',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, desactivar',
        cancelButtonText: 'Cancelar',
      })
      if (!result.isConfirmed) return
    }

    try {
      const res = await apiClient.post(`/usuarios/api/cambiar-estado/${u.id}`)
      notify((res.message as string) ?? 'Estado actualizado', 'success')
      await loadUsuarios()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cambiar el estado', 'error')
    }
  }

  async function handleDelete(u: Usuario) {
    const result = await Swal.fire({
      title: '¿Estás seguro?',
      text: '¡No podrás revertir esta acción!',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, ¡eliminar!',
      cancelButtonText: 'Cancelar',
    })
    if (!result.isConfirmed) return

    try {
      const res = await apiClient.delete(`/usuarios/api/eliminar/${u.id}`)
      notify((res.message as string) ?? 'Usuario eliminado correctamente', 'success')
      await loadUsuarios()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al eliminar el usuario', 'error')
    }
  }

  return (
    <div>
      <header className="border-b bg-white p-3 shadow-sm md:p-4">
        <div className="flex items-center">
          <h1 className="text-xl font-bold md:text-2xl">Gestión de Usuarios</h1>
          <button
            onClick={openCreateModal}
            className="ml-auto flex items-center gap-2 rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover"
          >
            <i className="bi bi-plus-circle" />
            Nuevo Usuario
          </button>
        </div>
      </header>

      <div className="p-4 md:p-6">
        <div className="rounded-xl bg-white p-4 shadow-sm md:p-6">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-bold">Lista de Usuarios</h2>
            <input
              type="search"
              placeholder="Buscar..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full max-w-xs rounded-lg border border-slate-300 px-3 py-1.5 text-sm focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
            />
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[700px] text-center text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-3 py-2 font-semibold">ID</th>
                  <th className="px-3 py-2 font-semibold">Nombre</th>
                  <th className="px-3 py-2 font-semibold">Usuario</th>
                  <th className="px-3 py-2 font-semibold">Perfil</th>
                  <th className="px-3 py-2 font-semibold">Correo</th>
                  <th className="px-3 py-2 font-semibold">Estado</th>
                  <th className="px-3 py-2 font-semibold">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={7} className="px-3 py-6 text-slate-500">
                      Cargando...
                    </td>
                  </tr>
                ) : filtered.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-3 py-6 text-slate-500">
                      No se encontraron resultados
                    </td>
                  </tr>
                ) : (
                  filtered.map((u) => {
                    const isActive = u.estado === 1
                    const isCurrentUser = yo?.id === u.id
                    const admin = isAdmin(u)
                    const isLastActiveAdmin = admin && isActive && totalAdmins === 1
                    return (
                      <tr
                        key={u.id}
                        className={`border-t ${
                          isCurrentUser ? 'bg-blue-50 font-semibold' : isActive ? '' : 'bg-slate-100 text-slate-500'
                        }`}
                      >
                        <td className="px-3 py-2">{u.id}</td>
                        <td className="px-3 py-2">{u.nombre}</td>
                        <td className="px-3 py-2">{u.usuario}</td>
                        <td className="px-3 py-2">{u.perfil?.nombre ?? 'Sin perfil'}</td>
                        <td className="px-3 py-2">{u.correo}</td>
                        <td className="px-3 py-2">
                          <span
                            className={`rounded-full px-2.5 py-1 text-xs font-semibold text-white ${
                              isActive ? 'bg-brand-success' : 'bg-brand-danger'
                            }`}
                          >
                            {isActive ? 'Activo' : 'Inactivo'}
                          </span>
                        </td>
                        <td className="px-3 py-2">
                          <div className="flex items-center justify-center gap-2">
                            <button
                              onClick={() => openEditModal(u)}
                              title="Editar"
                              className="rounded-lg bg-brand-primary px-2.5 py-1.5 text-white transition hover:bg-brand-primary-hover"
                            >
                              <i className="bi bi-pencil-square" />
                            </button>
                            {isCurrentUser ? (
                              <span className="rounded-full bg-sky-100 px-2.5 py-1 text-xs font-semibold text-sky-700">
                                <i className="bi bi-person-check-fill me-1" />
                                Eres tú
                              </span>
                            ) : isLastActiveAdmin ? (
                              <span
                                className="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-semibold text-amber-700"
                                title="No se puede desactivar/eliminar al único administrador"
                              >
                                <i className="bi bi-shield-lock-fill me-1" />
                                Único Admin
                              </span>
                            ) : (
                              <>
                                <input
                                  type="checkbox"
                                  checked={isActive}
                                  onChange={() => handleToggleStatus(u)}
                                  title={isActive ? 'Desactivar' : 'Activar'}
                                  className="h-4 w-8 cursor-pointer accent-brand-success"
                                />
                                <button
                                  onClick={() => handleDelete(u)}
                                  title="Eliminar"
                                  className="rounded-lg bg-brand-danger px-2.5 py-1.5 text-white transition hover:bg-brand-danger-hover"
                                >
                                  <i className="bi bi-trash" />
                                </button>
                              </>
                            )}
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
      </div>

      {modalOpen && (
        <Modal
          title={editId ? 'Editar Usuario' : 'Agregar Usuario'}
          onClose={() => setModalOpen(false)}
          footer={
            <>
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="rounded-lg bg-slate-200 px-4 py-2 font-medium text-slate-700 transition hover:bg-slate-300"
              >
                Cancelar
              </button>
              <button
                type="submit"
                form="formUsuario"
                disabled={saving}
                className="rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover disabled:opacity-60"
              >
                {saving ? 'Guardando...' : 'Guardar'}
              </button>
            </>
          }
        >
          <form id="formUsuario" onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="nombre" className="mb-1 block text-sm font-medium">
                  Nombre
                </label>
                <input
                  id="nombre"
                  value={form.nombre}
                  onChange={(e) => setForm((f) => ({ ...f, nombre: e.target.value }))}
                  className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                    fieldErrors.nombre
                      ? 'border-brand-danger focus:ring-brand-danger/25'
                      : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                  }`}
                />
                {fieldErrors.nombre && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.nombre}</p>}
              </div>

              <div>
                <label htmlFor="usuario" className="mb-1 block text-sm font-medium">
                  Usuario
                </label>
                <input
                  id="usuario"
                  value={form.usuario}
                  onChange={(e) => setForm((f) => ({ ...f, usuario: e.target.value }))}
                  className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                    fieldErrors.usuario
                      ? 'border-brand-danger focus:ring-brand-danger/25'
                      : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                  }`}
                />
                {fieldErrors.usuario && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.usuario}</p>}
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="correo" className="mb-1 block text-sm font-medium">
                  Correo
                </label>
                <input
                  id="correo"
                  type="email"
                  value={form.correo}
                  onChange={(e) => setForm((f) => ({ ...f, correo: e.target.value }))}
                  className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                    fieldErrors.correo
                      ? 'border-brand-danger focus:ring-brand-danger/25'
                      : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                  }`}
                />
                {fieldErrors.correo && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.correo}</p>}
              </div>

              <div>
                <label htmlFor="perfilId" className="mb-1 block text-sm font-medium">
                  Perfil
                </label>
                <select
                  id="perfilId"
                  value={form.perfilId}
                  onChange={(e) => setForm((f) => ({ ...f, perfilId: e.target.value }))}
                  className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                    fieldErrors.perfilId
                      ? 'border-brand-danger focus:ring-brand-danger/25'
                      : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                  }`}
                >
                  <option value="">Seleccione un perfil...</option>
                  {perfiles.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.nombre}
                    </option>
                  ))}
                </select>
                {fieldErrors.perfilId && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.perfilId}</p>}
              </div>
            </div>

            <div>
              <label htmlFor="clave" className="mb-1 block text-sm font-medium">
                Contraseña
              </label>
              <input
                id="clave"
                type="password"
                value={form.clave}
                onChange={(e) => setForm((f) => ({ ...f, clave: e.target.value }))}
                placeholder={editId ? 'Dejar en blanco para no cambiar' : ''}
                className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                  fieldErrors.clave
                    ? 'border-brand-danger focus:ring-brand-danger/25'
                    : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                }`}
              />
              {fieldErrors.clave ? (
                <p className="mt-1 text-sm text-brand-danger">{fieldErrors.clave}</p>
              ) : (
                <p className="mt-1 text-sm text-slate-500">Mínimo 6 caracteres</p>
              )}
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
