import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import Swal from 'sweetalert2'
import Modal from '../components/ui/Modal'
import { apiClient, ApiError } from '../lib/apiClient'
import type { Cliente, ClienteFormData } from '../types'

const EMPTY_FORM: ClienteFormData = { nombre: '', documento: '', telefono: '', correo: '' }
const DOCUMENTO_RE = /^\d{8}$|^\d{11}$/
const TELEFONO_RE = /^\d{9}$/
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

export default function Clientes() {
  const [clientes, setClientes] = useState<Cliente[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [form, setForm] = useState<ClienteFormData>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  const [consultaDoc, setConsultaDoc] = useState('')
  const [consultaLoading, setConsultaLoading] = useState(false)
  const [consultaMensaje, setConsultaMensaje] = useState<{ type: 'success' | 'warning' | 'danger'; text: string } | null>(null)

  useEffect(() => {
    loadClientes()
  }, [])

  async function loadClientes() {
    setLoading(true)
    try {
      const res = await apiClient.get<Cliente[]>('/clientes/api/datatables')
      const data = ((res.data as Cliente[]) ?? []).filter((c) => c.estado !== 2)
      setClientes(data)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar clientes', 'error')
    } finally {
      setLoading(false)
    }
  }

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return clientes
    return clientes.filter(
      (c) => c.nombre.toLowerCase().includes(term) || c.documento.includes(term) || c.telefono.includes(term),
    )
  }, [clientes, search])

  function openCreateModal() {
    setEditId(null)
    setForm(EMPTY_FORM)
    setFieldErrors({})
    setConsultaDoc('')
    setConsultaMensaje(null)
    setModalOpen(true)
  }

  async function openEditModal(id: number) {
    try {
      const res = await apiClient.get<Cliente>(`/clientes/api/${id}`)
      const cliente = res.data as Cliente
      setEditId(cliente.id)
      setForm({ nombre: cliente.nombre, documento: cliente.documento, telefono: cliente.telefono, correo: cliente.correo ?? '' })
      setFieldErrors({})
      setConsultaDoc('')
      setConsultaMensaje(null)
      setModalOpen(true)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar los datos del cliente', 'error')
    }
  }

  async function handleConsultarDocumento() {
    const documento = consultaDoc.trim()
    setConsultaMensaje(null)

    if (!documento) {
      setConsultaMensaje({ type: 'warning', text: 'Por favor ingrese un documento' })
      return
    }
    if (documento.length !== 8 && documento.length !== 11) {
      setConsultaMensaje({ type: 'danger', text: 'El documento debe tener 8 dígitos (DNI) o 11 dígitos (RUC)' })
      return
    }

    setConsultaLoading(true)
    try {
      const res = await apiClient.get(`/clientes/api/consultar-documento/${documento}`)
      const nombreEncontrado = (res.nombre as string) || (res.razonSocial as string) || ''
      if (nombreEncontrado) {
        setForm((f) => ({ ...f, nombre: nombreEncontrado, documento }))
        setConsultaMensaje({ type: 'success', text: 'Datos encontrados y autocompletados. Complete teléfono y correo.' })
      } else {
        setConsultaMensaje({ type: 'warning', text: 'No se encontró información para este documento' })
      }
    } catch (error) {
      setConsultaMensaje({
        type: 'danger',
        text: error instanceof ApiError ? error.message : 'Error al consultar el documento',
      })
    } finally {
      setConsultaLoading(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const errors: Record<string, string> = {}
    if (!form.nombre.trim()) errors.nombre = 'El nombre del cliente es obligatorio'
    if (!form.documento.trim()) errors.documento = 'El documento es obligatorio'
    else if (!DOCUMENTO_RE.test(form.documento.trim())) errors.documento = 'El documento debe ser un DNI (8 dígitos) o RUC (11 dígitos)'
    if (!form.telefono.trim()) errors.telefono = 'El teléfono es obligatorio'
    else if (!TELEFONO_RE.test(form.telefono.trim())) errors.telefono = 'El teléfono debe tener 9 dígitos'
    if (form.correo.trim() && !EMAIL_RE.test(form.correo.trim())) errors.correo = 'El correo debe ser válido'

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    setSaving(true)
    setFieldErrors({})
    try {
      const payload = {
        nombre: form.nombre.trim(),
        documento: form.documento.trim(),
        telefono: form.telefono.trim(),
        correo: form.correo.trim() || null,
      }
      if (editId) {
        await apiClient.put(`/clientes/api/actualizar/${editId}`, payload)
        notify('Cliente actualizado exitosamente', 'success')
      } else {
        await apiClient.post('/clientes/api/crear', payload)
        notify('Cliente creado exitosamente', 'success')
      }
      setModalOpen(false)
      await loadClientes()
    } catch (error) {
      if (error instanceof ApiError && error.errors) {
        setFieldErrors(error.errors)
      } else {
        notify(error instanceof ApiError ? error.message : 'Error al guardar el cliente', 'error')
      }
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus(cliente: Cliente) {
    const isActive = cliente.estado === 1
    if (isActive) {
      const result = await Swal.fire({
        title: '¿Estás seguro?',
        text: '¡El cliente se desactivará y no podrá utilizarse!',
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
      await apiClient.put(`/clientes/api/cambiar-estado/${cliente.id}`)
      notify('Estado cambiado exitosamente', 'success')
      await loadClientes()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cambiar el estado', 'error')
    }
  }

  async function handleDelete(cliente: Cliente) {
    const result = await Swal.fire({
      title: '¿Eliminar cliente?',
      text: `¿Está seguro de eliminar al cliente "${cliente.nombre}"? Esta acción no se puede deshacer.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
    })
    if (!result.isConfirmed) return

    try {
      await apiClient.delete(`/clientes/api/eliminar/${cliente.id}`)
      notify('Cliente eliminado exitosamente', 'success')
      await loadClientes()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al eliminar el cliente', 'error')
    }
  }

  const consultaMensajeClass: Record<'success' | 'warning' | 'danger', string> = {
    success: 'bg-green-50 text-green-700 border-green-300',
    warning: 'bg-amber-50 text-amber-700 border-amber-300',
    danger: 'bg-red-50 text-brand-danger border-red-300',
  }

  return (
    <div>
      <header className="border-b bg-white p-3 shadow-sm md:p-4">
        <div className="flex items-center">
          <h1 className="text-xl font-bold md:text-2xl">Gestión de Clientes</h1>
          <button
            onClick={openCreateModal}
            className="ml-auto flex items-center gap-2 rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover"
          >
            <i className="bi bi-plus-circle" />
            Nuevo Cliente
          </button>
        </div>
      </header>

      <div className="p-4 md:p-6">
        <div className="rounded-xl bg-white p-4 shadow-sm md:p-6">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-bold">Lista de Clientes</h2>
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
                  <th className="px-3 py-2 font-semibold">Documento</th>
                  <th className="px-3 py-2 font-semibold">Teléfono</th>
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
                  filtered.map((cliente) => {
                    const isActive = cliente.estado === 1
                    return (
                      <tr key={cliente.id} className={`border-t ${isActive ? '' : 'bg-slate-100 text-slate-500'}`}>
                        <td className="px-3 py-2">{cliente.id}</td>
                        <td className="px-3 py-2">{cliente.nombre}</td>
                        <td className="px-3 py-2">{cliente.documento}</td>
                        <td className="px-3 py-2">{cliente.telefono}</td>
                        <td className="px-3 py-2">{cliente.correo || '-'}</td>
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
                              onClick={() => openEditModal(cliente.id)}
                              title="Editar"
                              className="rounded-lg bg-brand-primary px-2.5 py-1.5 text-white transition hover:bg-brand-primary-hover"
                            >
                              <i className="bi bi-pencil-square" />
                            </button>
                            <input
                              type="checkbox"
                              checked={isActive}
                              onChange={() => handleToggleStatus(cliente)}
                              title={isActive ? 'Desactivar' : 'Activar'}
                              className="h-4 w-8 cursor-pointer accent-brand-success"
                            />
                            <button
                              onClick={() => handleDelete(cliente)}
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
          title={editId ? 'Editar Cliente' : 'Agregar Cliente'}
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
                form="formCliente"
                disabled={saving}
                className="rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover disabled:opacity-60"
              >
                {saving ? 'Guardando...' : 'Guardar Cliente'}
              </button>
            </>
          }
        >
          <div className="mb-4 rounded-lg bg-slate-50 p-4">
            <h6 className="mb-3 flex items-center gap-2 text-sm font-semibold text-brand-primary">
              <i className="bi bi-search" />
              Consultar Documento (DNI/RUC)
            </h6>
            <label htmlFor="documentoConsulta" className="mb-1 block text-sm font-medium">
              Documento:
            </label>
            <div className="flex gap-2">
              <input
                id="documentoConsulta"
                value={consultaDoc}
                onChange={(e) => setConsultaDoc(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault()
                    handleConsultarDocumento()
                  }
                }}
                placeholder="Ingrese DNI (8 dígitos) o RUC (11 dígitos)"
                maxLength={11}
                className="flex-1 rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
              />
              <button
                type="button"
                onClick={handleConsultarDocumento}
                disabled={consultaLoading}
                className="flex items-center gap-2 rounded-lg bg-sky-500 px-4 py-2 font-medium text-white transition hover:bg-sky-600 disabled:opacity-60"
              >
                <i className="bi bi-search" />
                {consultaLoading ? 'Consultando...' : 'Consultar'}
              </button>
            </div>
            <p className="mt-1 text-xs text-slate-500">
              <i className="bi bi-info-circle me-1" />
              Ingrese el documento y haga clic en "Consultar" para autocompletar el nombre
            </p>
            {consultaMensaje && (
              <div className={`mt-2 rounded-lg border px-3 py-2 text-sm ${consultaMensajeClass[consultaMensaje.type]}`}>
                {consultaMensaje.text}
              </div>
            )}
          </div>

          <form id="formCliente" onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="nombre" className="mb-1 block text-sm font-medium">
                Nombre Completo / Razón Social <span className="text-brand-danger">*</span>
              </label>
              <input
                id="nombre"
                value={form.nombre}
                onChange={(e) => setForm((f) => ({ ...f, nombre: e.target.value }))}
                placeholder="Ingrese el nombre completo o razón social"
                className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                  fieldErrors.nombre
                    ? 'border-brand-danger focus:ring-brand-danger/25'
                    : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                }`}
              />
              {fieldErrors.nombre && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.nombre}</p>}
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="documento" className="mb-1 block text-sm font-medium">
                  Documento (DNI/RUC) <span className="text-brand-danger">*</span>
                </label>
                <input
                  id="documento"
                  value={form.documento}
                  onChange={(e) => setForm((f) => ({ ...f, documento: e.target.value }))}
                  placeholder="8 o 11 dígitos"
                  maxLength={11}
                  className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                    fieldErrors.documento
                      ? 'border-brand-danger focus:ring-brand-danger/25'
                      : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                  }`}
                />
                {fieldErrors.documento && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.documento}</p>}
              </div>

              <div>
                <label htmlFor="telefono" className="mb-1 block text-sm font-medium">
                  Teléfono <span className="text-brand-danger">*</span>
                </label>
                <input
                  id="telefono"
                  value={form.telefono}
                  onChange={(e) => setForm((f) => ({ ...f, telefono: e.target.value }))}
                  placeholder="9 dígitos"
                  maxLength={9}
                  className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                    fieldErrors.telefono
                      ? 'border-brand-danger focus:ring-brand-danger/25'
                      : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                  }`}
                />
                {fieldErrors.telefono && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.telefono}</p>}
              </div>
            </div>

            <div>
              <label htmlFor="correo" className="mb-1 block text-sm font-medium">
                Correo Electrónico (Opcional)
              </label>
              <input
                id="correo"
                type="email"
                value={form.correo}
                onChange={(e) => setForm((f) => ({ ...f, correo: e.target.value }))}
                placeholder="ejemplo@correo.com"
                className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                  fieldErrors.correo
                    ? 'border-brand-danger focus:ring-brand-danger/25'
                    : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                }`}
              />
              {fieldErrors.correo && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.correo}</p>}
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
