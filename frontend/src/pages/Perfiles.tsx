import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import Swal from 'sweetalert2'
import Modal from '../components/ui/Modal'
import { apiClient, ApiError } from '../lib/apiClient'
import type { Opcion, Perfil, PerfilDetalle, PerfilFormData } from '../types'

const EMPTY_FORM: PerfilFormData = { nombre: '', descripcion: '' }

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

export default function Perfiles() {
  const [perfiles, setPerfiles] = useState<Perfil[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [form, setForm] = useState<PerfilFormData>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  const [permisosOpen, setPermisosOpen] = useState(false)
  const [permisosPerfil, setPermisosPerfil] = useState<PerfilDetalle | null>(null)
  const [todasOpciones, setTodasOpciones] = useState<Opcion[]>([])
  const [seleccionadas, setSeleccionadas] = useState<Set<number>>(new Set())
  const [savingPermisos, setSavingPermisos] = useState(false)

  useEffect(() => {
    loadPerfiles()
  }, [])

  async function loadPerfiles() {
    setLoading(true)
    try {
      const res = await apiClient.get<Perfil[]>('/perfiles/api/listar')
      setPerfiles((res.data as Perfil[]) ?? [])
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar perfiles', 'error')
    } finally {
      setLoading(false)
    }
  }

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return perfiles
    return perfiles.filter((p) => p.nombre.toLowerCase().includes(term) || (p.descripcion ?? '').toLowerCase().includes(term))
  }, [perfiles, search])

  function openCreateModal() {
    setEditId(null)
    setForm(EMPTY_FORM)
    setFieldErrors({})
    setModalOpen(true)
  }

  async function openEditModal(id: number) {
    try {
      const res = await apiClient.get<PerfilDetalle>(`/perfiles/api/${id}`)
      const detalle = res.data as PerfilDetalle
      setEditId(detalle.id)
      setForm({ nombre: detalle.nombre, descripcion: detalle.descripcion ?? '' })
      setFieldErrors({})
      setModalOpen(true)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar el perfil', 'error')
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!form.nombre.trim()) {
      setFieldErrors({ nombre: 'El nombre es obligatorio' })
      return
    }

    setSaving(true)
    setFieldErrors({})
    try {
      const payload = { id: editId, nombre: form.nombre.trim(), descripcion: form.descripcion.trim() || null }
      const res = await apiClient.post('/perfiles/api/guardar', payload)
      notify((res.message as string) ?? 'Perfil guardado', 'success')
      setModalOpen(false)
      await loadPerfiles()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al guardar el perfil', 'error')
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus(perfil: Perfil) {
    const isActive = perfil.estado
    if (isActive) {
      const result = await Swal.fire({
        title: '¿Estás seguro?',
        text: '¡El perfil se desactivará y no podrá utilizarse!',
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
      const res = await apiClient.post(`/perfiles/api/cambiar-estado/${perfil.id}`)
      notify((res.message as string) ?? 'Estado del perfil actualizado', 'success')
      await loadPerfiles()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cambiar el estado', 'error')
    }
  }

  async function handleDelete(perfil: Perfil) {
    const result = await Swal.fire({
      title: '¿Estás seguro?',
      text: '¡No podrás revertir esta acción! Se eliminará el perfil permanentemente.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, ¡eliminar!',
      cancelButtonText: 'Cancelar',
    })
    if (!result.isConfirmed) return

    try {
      const res = await apiClient.delete(`/perfiles/api/eliminar/${perfil.id}`)
      notify((res.message as string) ?? 'Perfil eliminado correctamente', 'success')
      await loadPerfiles()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al eliminar el perfil', 'error')
    }
  }

  async function openPermisos(perfil: Perfil) {
    try {
      const [perfilRes, opcionesRes] = await Promise.all([
        apiClient.get<PerfilDetalle>(`/perfiles/api/${perfil.id}`),
        apiClient.get<Opcion[]>('/perfiles/api/opciones'),
      ])
      const detalle = perfilRes.data as PerfilDetalle
      setPermisosPerfil(detalle)
      setTodasOpciones((opcionesRes.data as Opcion[]) ?? [])
      setSeleccionadas(new Set(detalle.opciones))
      setPermisosOpen(true)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar datos de permisos', 'error')
    }
  }

  function toggleOpcion(id: number) {
    setSeleccionadas((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  async function handleSavePermisos() {
    if (!permisosPerfil) return

    setSavingPermisos(true)
    try {
      const payload = {
        id: permisosPerfil.id,
        nombre: permisosPerfil.nombre,
        descripcion: permisosPerfil.descripcion,
        estado: permisosPerfil.estado,
        opciones: Array.from(seleccionadas),
      }
      await apiClient.post('/perfiles/api/guardar', payload)
      notify('Permisos actualizados correctamente', 'success')
      setPermisosOpen(false)
      await loadPerfiles()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al guardar permisos', 'error')
    } finally {
      setSavingPermisos(false)
    }
  }

  return (
    <div>
      <header className="border-b bg-white p-3 shadow-sm md:p-4">
        <div className="flex items-center">
          <h1 className="text-xl font-bold md:text-2xl">Gestión de Perfiles</h1>
          <button
            onClick={openCreateModal}
            className="ml-auto flex items-center gap-2 rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover"
          >
            <i className="bi bi-plus-circle" />
            Nuevo Perfil
          </button>
        </div>
      </header>

      <div className="p-4 md:p-6">
        <div className="rounded-xl bg-white p-4 shadow-sm md:p-6">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-bold">Lista de Perfiles</h2>
            <input
              type="search"
              placeholder="Buscar..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full max-w-xs rounded-lg border border-slate-300 px-3 py-1.5 text-sm focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
            />
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[650px] text-center text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-3 py-2 font-semibold">ID</th>
                  <th className="px-3 py-2 font-semibold">Nombre</th>
                  <th className="px-3 py-2 font-semibold">Descripción</th>
                  <th className="px-3 py-2 font-semibold">Estado</th>
                  <th className="px-3 py-2 font-semibold">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={5} className="px-3 py-6 text-slate-500">
                      Cargando...
                    </td>
                  </tr>
                ) : filtered.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-3 py-6 text-slate-500">
                      No se encontraron resultados
                    </td>
                  </tr>
                ) : (
                  filtered.map((perfil) => (
                    <tr key={perfil.id} className={`border-t ${perfil.estado ? '' : 'bg-slate-100 text-slate-500'}`}>
                      <td className="px-3 py-2">{perfil.id}</td>
                      <td className="px-3 py-2">{perfil.nombre}</td>
                      <td className="px-3 py-2">{perfil.descripcion || '-'}</td>
                      <td className="px-3 py-2">
                        <span
                          className={`rounded-full px-2.5 py-1 text-xs font-semibold text-white ${
                            perfil.estado ? 'bg-brand-success' : 'bg-brand-danger'
                          }`}
                        >
                          {perfil.estado ? 'Activo' : 'Inactivo'}
                        </span>
                      </td>
                      <td className="px-3 py-2">
                        <div className="flex items-center justify-center gap-2">
                          <button
                            onClick={() => openPermisos(perfil)}
                            title="Permisos"
                            className="flex items-center gap-1 rounded-lg bg-sky-500 px-2.5 py-1.5 text-white transition hover:bg-sky-600"
                          >
                            <i className="bi bi-shield-lock-fill" />
                            <span className="hidden sm:inline">Permisos</span>
                          </button>
                          <button
                            onClick={() => openEditModal(perfil.id)}
                            title="Editar"
                            className="rounded-lg bg-brand-primary px-2.5 py-1.5 text-white transition hover:bg-brand-primary-hover"
                          >
                            <i className="bi bi-pencil-square" />
                          </button>
                          <input
                            type="checkbox"
                            checked={perfil.estado}
                            onChange={() => handleToggleStatus(perfil)}
                            title={perfil.estado ? 'Desactivar' : 'Activar'}
                            className="h-4 w-8 cursor-pointer accent-brand-success"
                          />
                          <button
                            onClick={() => handleDelete(perfil)}
                            title="Eliminar"
                            className="rounded-lg bg-brand-danger px-2.5 py-1.5 text-white transition hover:bg-brand-danger-hover"
                          >
                            <i className="bi bi-trash" />
                          </button>
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

      {modalOpen && (
        <Modal
          title={editId ? 'Editar Perfil' : 'Agregar Perfil'}
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
                form="formPerfil"
                disabled={saving}
                className="rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover disabled:opacity-60"
              >
                {saving ? 'Guardando...' : 'Guardar Perfil'}
              </button>
            </>
          }
        >
          <form id="formPerfil" onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="nombre" className="mb-1 block text-sm font-medium">
                Nombre del Perfil:
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
              <label htmlFor="descripcion" className="mb-1 block text-sm font-medium">
                Descripción:
              </label>
              <textarea
                id="descripcion"
                rows={3}
                value={form.descripcion}
                onChange={(e) => setForm((f) => ({ ...f, descripcion: e.target.value }))}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
              />
            </div>
          </form>
        </Modal>
      )}

      {permisosOpen && permisosPerfil && (
        <Modal
          title="Asignar Permisos"
          onClose={() => setPermisosOpen(false)}
          footer={
            <>
              <button
                type="button"
                onClick={() => setPermisosOpen(false)}
                className="rounded-lg bg-slate-200 px-4 py-2 font-medium text-slate-700 transition hover:bg-slate-300"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleSavePermisos}
                disabled={savingPermisos}
                className="rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover disabled:opacity-60"
              >
                {savingPermisos ? 'Guardando...' : 'Guardar Permisos'}
              </button>
            </>
          }
        >
          <p className="mb-3 text-sm">
            Seleccione los módulos a los que tendrá acceso el perfil <strong>{permisosPerfil.nombre}</strong>:
          </p>
          <div className="max-h-80 divide-y divide-slate-200 overflow-y-auto rounded-lg border border-slate-200">
            {todasOpciones.map((opcion) => (
              <label key={opcion.id} className="flex cursor-pointer items-center gap-2 px-3 py-2 text-sm hover:bg-slate-50">
                <input
                  type="checkbox"
                  checked={seleccionadas.has(opcion.id)}
                  onChange={() => toggleOpcion(opcion.id)}
                  className="h-4 w-4 accent-brand-primary"
                />
                {opcion.nombre}
              </label>
            ))}
          </div>
        </Modal>
      )}
    </div>
  )
}
