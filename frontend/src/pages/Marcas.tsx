import { useEffect, useMemo, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import Swal from 'sweetalert2'
import Modal from '../components/ui/Modal'
import ImageThumbnail from '../components/ui/ImageThumbnail'
import { apiClient, ApiError, uploadFile } from '../lib/apiClient'
import type { Brand, BrandFormData } from '../types'

const EMPTY_FORM: BrandFormData = { nombre: '', imagen: '' }

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

export default function Marcas() {
  const [marcas, setMarcas] = useState<Brand[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [form, setForm] = useState<BrandFormData>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    loadMarcas()
  }, [])

  async function loadMarcas() {
    setLoading(true)
    try {
      const res = await apiClient.get<Brand[]>('/marcas/api/datatables')
      const data = ((res.data as Brand[]) ?? []).filter((m) => m.estado !== 2)
      setMarcas(data)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar marcas', 'error')
    } finally {
      setLoading(false)
    }
  }

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return marcas
    return marcas.filter((m) => m.nombre.toLowerCase().includes(term))
  }, [marcas, search])

  function openCreateModal() {
    setEditId(null)
    setForm(EMPTY_FORM)
    setFieldErrors({})
    setModalOpen(true)
  }

  async function openEditModal(id: number) {
    try {
      const res = await apiClient.get<Brand>(`/marcas/api/${id}`)
      const marca = res.data as Brand
      setEditId(marca.id)
      setForm({ nombre: marca.nombre, imagen: marca.imagen ?? '' })
      setFieldErrors({})
      setModalOpen(true)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar los datos de la marca', 'error')
    }
  }

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    setUploading(true)
    try {
      const res = await uploadFile('/api/upload/marcas/imagen', file)
      setForm((f) => ({ ...f, imagen: (res.url as string) ?? f.imagen }))
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al subir la imagen', 'error')
    } finally {
      setUploading(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!form.nombre.trim()) {
      setFieldErrors({ nombre: 'El nombre de la marca es requerido' })
      return
    }

    setSaving(true)
    setFieldErrors({})
    try {
      const payload = { nombre: form.nombre.trim(), imagen: form.imagen.trim() || null }
      if (editId) {
        await apiClient.put(`/marcas/api/actualizar/${editId}`, payload)
        notify('Marca actualizada exitosamente', 'success')
      } else {
        await apiClient.post('/marcas/api/crear', payload)
        notify('Marca creada exitosamente', 'success')
      }
      setModalOpen(false)
      await loadMarcas()
    } catch (error) {
      if (error instanceof ApiError && error.errors) {
        setFieldErrors(error.errors)
      } else {
        notify(error instanceof ApiError ? error.message : 'Error al guardar la marca', 'error')
      }
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus(marca: Brand) {
    const isActive = marca.estado === 1
    if (isActive) {
      const result = await Swal.fire({
        title: '¿Desactivar marca?',
        text: `La marca "${marca.nombre}" dejará de estar visible.`,
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
      await apiClient.put(`/marcas/api/cambiar-estado/${marca.id}`)
      notify('Estado cambiado exitosamente', 'success')
      await loadMarcas()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cambiar el estado', 'error')
    }
  }

  async function handleDelete(marca: Brand) {
    const result = await Swal.fire({
      title: '¿Eliminar marca?',
      text: `¿Está seguro de eliminar la marca "${marca.nombre}"? Esta acción no se puede deshacer.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
    })
    if (!result.isConfirmed) return

    try {
      await apiClient.delete(`/marcas/api/eliminar/${marca.id}`)
      notify('Marca eliminada exitosamente', 'success')
      await loadMarcas()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al eliminar la marca', 'error')
    }
  }

  return (
    <div>
      <header className="border-b bg-white p-3 shadow-sm md:p-4">
        <div className="flex items-center">
          <h1 className="text-xl font-bold md:text-2xl">Gestión de Marcas</h1>
          <button
            onClick={openCreateModal}
            className="ml-auto flex items-center gap-2 rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover"
          >
            <i className="bi bi-plus-circle" />
            Nueva Marca
          </button>
        </div>
        <div className="mt-3 rounded-lg border border-amber-300 bg-amber-50 px-4 py-2 text-sm text-amber-800">
          <i className="bi bi-exclamation-triangle-fill me-2" /> Las imágenes que subas o modifiques aquí se
          mostrarán automáticamente en la página principal, dentro de la sección "Marcas" del sitio web.
        </div>
      </header>

      <div className="p-4 md:p-6">
        <div className="rounded-xl bg-white p-4 shadow-sm md:p-6">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-bold">Lista de Marcas</h2>
            <input
              type="search"
              placeholder="Buscar..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full max-w-xs rounded-lg border border-slate-300 px-3 py-1.5 text-sm focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
            />
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[600px] text-center text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-3 py-2 font-semibold">ID</th>
                  <th className="px-3 py-2 font-semibold">Imagen</th>
                  <th className="px-3 py-2 font-semibold">Nombre</th>
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
                  filtered.map((marca) => {
                    const isActive = marca.estado === 1
                    return (
                      <tr key={marca.id} className={`border-t ${isActive ? '' : 'bg-slate-100 text-slate-500'}`}>
                        <td className="px-3 py-2">{marca.id}</td>
                        <td className="px-3 py-2">
                          <ImageThumbnail src={marca.imagen} alt="Imagen de marca" />
                        </td>
                        <td className="px-3 py-2">{marca.nombre}</td>
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
                              onClick={() => openEditModal(marca.id)}
                              title="Editar"
                              className="rounded-lg bg-brand-primary px-2.5 py-1.5 text-white transition hover:bg-brand-primary-hover"
                            >
                              <i className="bi bi-pencil-square" />
                            </button>
                            <input
                              type="checkbox"
                              checked={isActive}
                              onChange={() => handleToggleStatus(marca)}
                              title={isActive ? 'Desactivar' : 'Activar'}
                              className="h-4 w-8 cursor-pointer accent-brand-success"
                            />
                            <button
                              onClick={() => handleDelete(marca)}
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
          title={editId ? 'Editar Marca' : 'Agregar Nueva Marca'}
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
                form="formMarca"
                disabled={saving || uploading}
                className="rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover disabled:opacity-60"
              >
                {saving ? 'Guardando...' : 'Guardar Marca'}
              </button>
            </>
          }
        >
          <form id="formMarca" onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="nombre" className="mb-1 block text-sm font-medium">
                Nombre de la Marca <span className="text-brand-danger">*</span>
              </label>
              <input
                id="nombre"
                value={form.nombre}
                onChange={(e) => setForm((f) => ({ ...f, nombre: e.target.value }))}
                placeholder="Ej: Nike, Adidas, Puma"
                maxLength={100}
                className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                  fieldErrors.nombre
                    ? 'border-brand-danger focus:ring-brand-danger/25'
                    : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                }`}
              />
              {fieldErrors.nombre && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.nombre}</p>}
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium">Imagen de la Marca</label>
              <div className="flex flex-col gap-2 sm:flex-row">
                <input
                  type="url"
                  value={form.imagen}
                  onChange={(e) => setForm((f) => ({ ...f, imagen: e.target.value }))}
                  placeholder="https://ejemplo.com/imagen-marca.jpg"
                  className={`flex-1 rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                    fieldErrors.imagen
                      ? 'border-brand-danger focus:ring-brand-danger/25'
                      : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                  }`}
                />
                <label className="flex cursor-pointer items-center justify-center gap-2 rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover">
                  <i className="bi bi-cloud-upload" />
                  {uploading ? 'Subiendo...' : 'Subir archivo'}
                  <input type="file" accept="image/*" onChange={handleFileChange} disabled={uploading} className="hidden" />
                </label>
              </div>
              {fieldErrors.imagen && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.imagen}</p>}
              {form.imagen && (
                <div className="mt-3">
                  <ImageThumbnail src={form.imagen} alt="Imagen de marca" />
                </div>
              )}
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
