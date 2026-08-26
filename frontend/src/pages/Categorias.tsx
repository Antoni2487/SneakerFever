import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import Swal from 'sweetalert2'
import Modal from '../components/ui/Modal'
import { apiClient, ApiError } from '../lib/apiClient'
import type { Category, CategoryFormData } from '../types'

const EMPTY_FORM: CategoryFormData = { nombre: '', descripcion: '' }

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

export default function Categorias() {
  const [categorias, setCategorias] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [form, setForm] = useState<CategoryFormData>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    loadCategorias()
  }, [])

  async function loadCategorias() {
    setLoading(true)
    try {
      const res = await apiClient.get<Category[]>('/categorias/api/datatables')
      setCategorias((res.data as Category[]) ?? [])
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar categorías', 'error')
    } finally {
      setLoading(false)
    }
  }

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return categorias
    return categorias.filter((c) => c.nombre.toLowerCase().includes(term))
  }, [categorias, search])

  function openCreateModal() {
    setEditId(null)
    setForm(EMPTY_FORM)
    setFieldErrors({})
    setModalOpen(true)
  }

  async function openEditModal(id: number) {
    try {
      const res = await apiClient.get<Category>(`/categorias/api/${id}`)
      const categoria = res.data as Category
      setEditId(categoria.id)
      setForm({ nombre: categoria.nombre, descripcion: categoria.descripcion ?? '' })
      setFieldErrors({})
      setModalOpen(true)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar los datos', 'error')
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!form.nombre.trim()) {
      setFieldErrors({ nombre: 'El nombre de la categoría es requerido' })
      return
    }

    setSaving(true)
    setFieldErrors({})
    try {
      if (editId) {
        await apiClient.put(`/categorias/api/actualizar/${editId}`, form)
        notify('Categoría actualizada exitosamente', 'success')
      } else {
        await apiClient.post('/categorias/api/crear', form)
        notify('Categoría creada exitosamente', 'success')
      }
      setModalOpen(false)
      await loadCategorias()
    } catch (error) {
      if (error instanceof ApiError && error.errors) {
        setFieldErrors(error.errors)
      } else {
        notify(error instanceof ApiError ? error.message : 'Error al guardar la categoría', 'error')
      }
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus(categoria: Category) {
    const isActive = categoria.estado === 1
    if (isActive) {
      const result = await Swal.fire({
        title: '¿Estás seguro?',
        text: '¡La categoría se desactivará y no podrá utilizarse!',
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
      await apiClient.put(`/categorias/api/cambiar-estado/${categoria.id}`)
      notify('Estado cambiado exitosamente', 'success')
      await loadCategorias()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cambiar el estado', 'error')
    }
  }

  async function handleDelete(categoria: Category) {
    const result = await Swal.fire({
      title: '¿Eliminar categoría?',
      text: `¿Está seguro de eliminar la categoría "${categoria.nombre}"? Esta acción no se puede deshacer.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
    })
    if (!result.isConfirmed) return

    try {
      await apiClient.delete(`/categorias/api/eliminar/${categoria.id}`)
      notify('Categoría eliminada exitosamente', 'success')
      await loadCategorias()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al eliminar la categoría', 'error')
    }
  }

  return (
    <div>
      <header className="border-b bg-white p-3 shadow-sm md:p-4">
        <div className="flex items-center">
          <h1 className="text-xl font-bold md:text-2xl">Gestión de Categorías</h1>
          <button
            onClick={openCreateModal}
            className="ml-auto flex items-center gap-2 rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover"
          >
            <i className="bi bi-plus-circle" />
            Nueva Categoría
          </button>
        </div>
      </header>

      <div className="p-4 md:p-6">
        <div className="rounded-xl bg-white p-4 shadow-sm md:p-6">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-bold">Lista de Categorías</h2>
            <input
              type="search"
              placeholder="Buscar..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full max-w-xs rounded-lg border border-slate-300 px-3 py-1.5 text-sm focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
            />
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[500px] text-center text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-3 py-2 font-semibold">ID</th>
                  <th className="px-3 py-2 font-semibold">Nombre de la Categoría</th>
                  <th className="px-3 py-2 font-semibold">Estado</th>
                  <th className="px-3 py-2 font-semibold">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={4} className="px-3 py-6 text-slate-500">
                      Cargando...
                    </td>
                  </tr>
                ) : filtered.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-3 py-6 text-slate-500">
                      No se encontraron resultados
                    </td>
                  </tr>
                ) : (
                  filtered.map((categoria) => {
                    const isActive = categoria.estado === 1
                    return (
                      <tr key={categoria.id} className={`border-t ${isActive ? '' : 'bg-slate-100 text-slate-500'}`}>
                        <td className="px-3 py-2">{categoria.id}</td>
                        <td className="px-3 py-2">{categoria.nombre}</td>
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
                              onClick={() => openEditModal(categoria.id)}
                              title="Editar"
                              className="rounded-lg bg-brand-primary px-2.5 py-1.5 text-white transition hover:bg-brand-primary-hover"
                            >
                              <i className="bi bi-pencil-square" />
                            </button>
                            <input
                              type="checkbox"
                              checked={isActive}
                              onChange={() => handleToggleStatus(categoria)}
                              title={isActive ? 'Desactivar' : 'Activar'}
                              className="h-4 w-8 cursor-pointer accent-brand-success"
                            />
                            <button
                              onClick={() => handleDelete(categoria)}
                              title="Eliminar"
                              className="rounded-lg bg-brand-danger px-2.5 py-1.5 text-white transition hover:bg-brand-danger-hover"
                            >
                              <i className="bi bi-trash" />
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
      </div>

      {modalOpen && (
        <Modal
          title={editId ? 'Editar Categoría' : 'Agregar Categoría'}
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
                form="formCategoria"
                disabled={saving}
                className="rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover disabled:opacity-60"
              >
                {saving ? 'Guardando...' : 'Guardar Categoría'}
              </button>
            </>
          }
        >
          <form id="formCategoria" onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="nombre" className="mb-1 block text-sm font-medium">
                Nombre de la Categoría:
              </label>
              <input
                id="nombre"
                value={form.nombre}
                onChange={(e) => setForm((f) => ({ ...f, nombre: e.target.value }))}
                placeholder="Ingrese el nombre de la categoría"
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
                Descripción (Opcional):
              </label>
              <textarea
                id="descripcion"
                rows={3}
                value={form.descripcion}
                onChange={(e) => setForm((f) => ({ ...f, descripcion: e.target.value }))}
                placeholder="Descripción de la categoría"
                className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                  fieldErrors.descripcion
                    ? 'border-brand-danger focus:ring-brand-danger/25'
                    : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                }`}
              />
              {fieldErrors.descripcion && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.descripcion}</p>}
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
