import { useEffect, useMemo, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import Swal from 'sweetalert2'
import Modal from '../components/ui/Modal'
import ImageThumbnail from '../components/ui/ImageThumbnail'
import { apiClient, ApiError, uploadFile } from '../lib/apiClient'
import type { Brand, Category, Genero, Product, ProductFormData } from '../types'

const EMPTY_FORM: ProductFormData = {
  nombre: '',
  descripcion: '',
  imagen: '',
  precio: '',
  descuento: '',
  stock: '0',
  stockMinimo: '0',
  genero: '',
  categoryId: '',
  brandId: '',
  destacado: false,
  estado: true,
}

const GENERO_LABELS: Record<Genero, string> = { HOMBRE: 'Masculino', MUJER: 'Femenino' }
const GENERO_BADGE_CLASS: Record<Genero, string> = {
  HOMBRE: 'bg-blue-600',
  MUJER: 'bg-brand-danger',
}

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

function formatPrice(value: number) {
  return `S/. ${value.toFixed(2)}`
}

function precioFinal(precio: number, descuento: number | null) {
  if (!descuento) return precio
  return precio * (1 - descuento / 100)
}

export default function Productos() {
  const [productos, setProductos] = useState<Product[]>([])
  const [categorias, setCategorias] = useState<Category[]>([])
  const [marcas, setMarcas] = useState<Brand[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [detalle, setDetalle] = useState<Product | null>(null)
  const [editId, setEditId] = useState<number | null>(null)
  const [form, setForm] = useState<ProductFormData>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    loadProductos()
    loadOpciones()
  }, [])

  async function loadProductos() {
    setLoading(true)
    try {
      const res = await apiClient.get<Product[]>('/productos/api/datatables')
      const data = ((res.data as Product[]) ?? []).filter((p) => p.estado !== 2)
      setProductos(data)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar productos', 'error')
    } finally {
      setLoading(false)
    }
  }

  async function loadOpciones() {
    try {
      const [cats, brands] = await Promise.all([
        apiClient.get<Category[]>('/categorias/api/activas'),
        apiClient.get<Brand[]>('/marcas/api/activas'),
      ])
      setCategorias((cats.data as Category[]) ?? [])
      setMarcas((brands.data as Brand[]) ?? [])
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar categorías y marcas', 'error')
    }
  }

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return productos
    return productos.filter((p) => p.nombre.toLowerCase().includes(term))
  }, [productos, search])

  function openCreateModal() {
    setEditId(null)
    setForm(EMPTY_FORM)
    setFieldErrors({})
    setModalOpen(true)
  }

  async function openEditModal(id: number) {
    try {
      const res = await apiClient.get<Product>(`/productos/api/${id}`)
      const producto = res.data as Product
      setEditId(producto.id)
      setForm({
        nombre: producto.nombre,
        descripcion: producto.descripcion ?? '',
        imagen: producto.imagen ?? '',
        precio: String(producto.precio ?? ''),
        descuento: producto.descuento ? String(producto.descuento) : '',
        stock: String(producto.stock ?? 0),
        stockMinimo: String(producto.stockMinimo ?? 0),
        genero: producto.genero,
        categoryId: producto.category ? String(producto.category.id) : '',
        brandId: producto.brand ? String(producto.brand.id) : '',
        destacado: Boolean(producto.destacado),
        estado: producto.estado === 1,
      })
      setFieldErrors({})
      setModalOpen(true)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar los datos del producto', 'error')
    }
  }

  async function openDetalle(id: number) {
    try {
      const res = await apiClient.get<Product>(`/productos/api/${id}`)
      setDetalle(res.data as Product)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar los detalles', 'error')
    }
  }

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    setUploading(true)
    try {
      const res = await uploadFile('/api/upload/productos/imagen', file)
      setForm((f) => ({ ...f, imagen: (res.url as string) ?? f.imagen }))
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al subir la imagen', 'error')
    } finally {
      setUploading(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const errors: Record<string, string> = {}
    if (!form.nombre.trim()) errors.nombre = 'El nombre del producto es requerido'
    if (!form.genero) errors.genero = 'El género es requerido'
    if (!form.categoryId) errors.categoryId = 'La categoría es requerida'
    if (!form.brandId) errors.brandId = 'La marca es requerida'
    const precio = parseFloat(form.precio)
    if (!form.precio || isNaN(precio) || precio <= 0) errors.precio = 'El precio debe ser mayor a 0'

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    setSaving(true)
    setFieldErrors({})
    try {
      const descuento = form.descuento ? parseFloat(form.descuento) : null
      const imagen = form.imagen.trim() || null
      const payload = {
        nombre: form.nombre.trim(),
        descripcion: form.descripcion.trim() || null,
        genero: form.genero,
        precio,
        descuento,
        stock: parseInt(form.stock, 10) || 0,
        stockMinimo: parseInt(form.stockMinimo, 10) || 0,
        imagen,
        imagenes: imagen ? [imagen] : null,
        destacado: form.destacado,
        estado: form.estado ? 1 : 0,
        categoryId: parseInt(form.categoryId, 10),
        brandId: parseInt(form.brandId, 10),
      }

      if (editId) {
        await apiClient.put(`/productos/api/actualizar/${editId}`, payload)
        notify('Producto actualizado exitosamente', 'success')
      } else {
        await apiClient.post('/productos/api/crear', payload)
        notify('Producto creado exitosamente', 'success')
      }
      setModalOpen(false)
      await loadProductos()
    } catch (error) {
      if (error instanceof ApiError && error.errors) {
        setFieldErrors(error.errors)
      } else {
        notify(error instanceof ApiError ? error.message : 'Error al guardar el producto', 'error')
      }
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus(producto: Product) {
    const isActive = producto.estado === 1
    if (isActive) {
      const result = await Swal.fire({
        title: '¿Desactivar producto?',
        text: `El producto "${producto.nombre}" dejará de estar visible en la tienda.`,
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
      await apiClient.put(`/productos/api/cambiar-estado/${producto.id}`)
      notify('Estado cambiado exitosamente', 'success')
      await loadProductos()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cambiar el estado', 'error')
    }
  }

  async function handleToggleDestacado(producto: Product) {
    try {
      await apiClient.put(`/productos/api/destacado/${producto.id}`)
      notify('Estado destacado cambiado exitosamente', 'success')
      await loadProductos()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cambiar el destacado', 'error')
    }
  }

  async function handleDelete(producto: Product) {
    const result = await Swal.fire({
      title: '¿Eliminar producto?',
      text: `¿Está seguro de eliminar el producto "${producto.nombre}"? Esta acción no se puede deshacer.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
    })
    if (!result.isConfirmed) return

    try {
      await apiClient.delete(`/productos/api/eliminar/${producto.id}`)
      notify('Producto eliminado exitosamente', 'success')
      await loadProductos()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al eliminar el producto', 'error')
    }
  }

  return (
    <div>
      <header className="border-b bg-white p-3 shadow-sm md:p-4">
        <div className="flex items-center">
          <h1 className="text-xl font-bold md:text-2xl">Gestión de Productos</h1>
          <button
            onClick={openCreateModal}
            className="ml-auto flex items-center gap-2 rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover"
          >
            <i className="bi bi-plus-circle" />
            Nuevo Producto
          </button>
        </div>
      </header>

      <div className="p-4 md:p-6">
        <div className="rounded-xl bg-white p-4 shadow-sm md:p-6">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-bold">Lista de Productos</h2>
            <input
              type="search"
              placeholder="Buscar..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full max-w-xs rounded-lg border border-slate-300 px-3 py-1.5 text-sm focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
            />
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[1000px] text-center text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-3 py-2 font-semibold">ID</th>
                  <th className="px-3 py-2 font-semibold">Imagen</th>
                  <th className="px-3 py-2 font-semibold">Nombre</th>
                  <th className="px-3 py-2 font-semibold">Precio</th>
                  <th className="px-3 py-2 font-semibold">Stock</th>
                  <th className="px-3 py-2 font-semibold">Categoría</th>
                  <th className="px-3 py-2 font-semibold">Marca</th>
                  <th className="px-3 py-2 font-semibold">Género</th>
                  <th className="px-3 py-2 font-semibold">Estado</th>
                  <th className="px-3 py-2 font-semibold">Destacado</th>
                  <th className="px-3 py-2 font-semibold">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={11} className="px-3 py-6 text-slate-500">
                      Cargando...
                    </td>
                  </tr>
                ) : filtered.length === 0 ? (
                  <tr>
                    <td colSpan={11} className="px-3 py-6 text-slate-500">
                      No se encontraron resultados
                    </td>
                  </tr>
                ) : (
                  filtered.map((producto) => {
                    const isActive = producto.estado === 1
                    const isDestacado = Boolean(producto.destacado)
                    const final = precioFinal(producto.precio, producto.descuento)
                    return (
                      <tr key={producto.id} className={`border-t ${isActive ? '' : 'bg-slate-100 text-slate-500'}`}>
                        <td className="px-3 py-2">{producto.id}</td>
                        <td className="px-3 py-2">
                          <ImageThumbnail src={producto.imagen} alt="Imagen de producto" />
                        </td>
                        <td className="px-3 py-2">{producto.nombre}</td>
                        <td className="px-3 py-2">
                          {producto.descuento ? (
                            <div className="flex flex-col items-center">
                              <span className="text-xs text-slate-400 line-through">{formatPrice(producto.precio)}</span>
                              <span className="font-semibold text-brand-danger">{formatPrice(final)}</span>
                            </div>
                          ) : (
                            <span>{formatPrice(producto.precio)}</span>
                          )}
                        </td>
                        <td className="px-3 py-2">{producto.stock}</td>
                        <td className="px-3 py-2">{producto.category?.nombre ?? '-'}</td>
                        <td className="px-3 py-2">{producto.brand?.nombre ?? '-'}</td>
                        <td className="px-3 py-2">
                          <span className={`rounded-full px-2.5 py-1 text-xs font-semibold text-white ${GENERO_BADGE_CLASS[producto.genero]}`}>
                            {producto.genero === 'HOMBRE' ? 'Hombre' : 'Mujer'}
                          </span>
                        </td>
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
                          <button
                            onClick={() => handleToggleDestacado(producto)}
                            title={isDestacado ? 'Quitar destacado' : 'Marcar destacado'}
                            className={`text-lg ${isDestacado ? 'text-amber-400' : 'text-slate-300'} transition hover:scale-110`}
                          >
                            <i className={`bi ${isDestacado ? 'bi-star-fill' : 'bi-star'}`} />
                          </button>
                        </td>
                        <td className="px-3 py-2">
                          <div className="flex items-center justify-center gap-2">
                            <button
                              onClick={() => openDetalle(producto.id)}
                              title="Ver detalles"
                              className="rounded-lg bg-sky-500 px-2.5 py-1.5 text-white transition hover:bg-sky-600"
                            >
                              <i className="bi bi-eye" />
                            </button>
                            <button
                              onClick={() => openEditModal(producto.id)}
                              title="Editar"
                              className="rounded-lg bg-brand-primary px-2.5 py-1.5 text-white transition hover:bg-brand-primary-hover"
                            >
                              <i className="bi bi-pencil-square" />
                            </button>
                            <input
                              type="checkbox"
                              checked={isActive}
                              onChange={() => handleToggleStatus(producto)}
                              title={isActive ? 'Desactivar' : 'Activar'}
                              className="h-4 w-8 cursor-pointer accent-brand-success"
                            />
                            <button
                              onClick={() => handleDelete(producto)}
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

      {detalle && (
        <Modal title="Detalles del Producto" onClose={() => setDetalle(null)}>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <div className="md:col-span-1">
              <ImageThumbnail src={detalle.imagen} alt={detalle.nombre} className="h-48 w-full" />
              {detalle.destacado && (
                <p className="mt-2 text-center">
                  <span className="rounded-full bg-gradient-to-r from-indigo-500 to-purple-600 px-3 py-1 text-xs font-semibold text-white">
                    <i className="bi bi-star-fill me-1" />
                    Destacado
                  </span>
                </p>
              )}
            </div>
            <div className="space-y-2 text-sm md:col-span-2">
              <p>
                <span className="font-bold text-slate-500">Estado: </span>
                <span
                  className={`rounded-full px-2.5 py-1 text-xs font-semibold text-white ${
                    detalle.estado === 1 ? 'bg-brand-success' : 'bg-brand-danger'
                  }`}
                >
                  {detalle.estado === 1 ? 'Activo' : 'Inactivo'}
                </span>
              </p>
              <p>
                <span className="font-bold text-slate-500">Nombre: </span>
                {detalle.nombre}
              </p>
              <p>
                <span className="font-bold text-slate-500">Descripción: </span>
                {detalle.descripcion || 'Sin descripción'}
              </p>
              <p>
                <span className="font-bold text-slate-500">Género: </span>
                {GENERO_LABELS[detalle.genero]}
              </p>
              <p>
                <span className="font-bold text-slate-500">Categoría: </span>
                {detalle.category?.nombre ?? '-'}
              </p>
              <p>
                <span className="font-bold text-slate-500">Marca: </span>
                {detalle.brand?.nombre ?? '-'}
              </p>
              <p>
                <span className="font-bold text-slate-500">Precio original: </span>
                {formatPrice(detalle.precio)}
              </p>
              <p>
                <span className="font-bold text-slate-500">Descuento: </span>
                {detalle.descuento ? `${detalle.descuento}%` : 'Sin descuento'}
              </p>
              <p>
                <span className="font-bold text-slate-500">Precio final: </span>
                <span className="text-lg font-bold text-brand-success">
                  {formatPrice(precioFinal(detalle.precio, detalle.descuento))}
                </span>
              </p>
              <p>
                <span className="font-bold text-slate-500">Stock disponible: </span>
                {detalle.stock}
              </p>
            </div>
          </div>
        </Modal>
      )}

      {modalOpen && (
        <Modal
          title={editId ? 'Editar Producto' : 'Agregar Producto'}
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
                form="formProducto"
                disabled={saving || uploading}
                className="rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover disabled:opacity-60"
              >
                {saving ? 'Guardando...' : 'Guardar Producto'}
              </button>
            </>
          }
        >
          <form id="formProducto" onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="nombre" className="mb-1 block text-sm font-medium">
                  Nombre del Producto <span className="text-brand-danger">*</span>
                </label>
                <input
                  id="nombre"
                  value={form.nombre}
                  onChange={(e) => setForm((f) => ({ ...f, nombre: e.target.value }))}
                  placeholder="Ej: Nike Air Max 2024"
                  className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                    fieldErrors.nombre
                      ? 'border-brand-danger focus:ring-brand-danger/25'
                      : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                  }`}
                />
                {fieldErrors.nombre && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.nombre}</p>}
              </div>

              <div>
                <label htmlFor="genero" className="mb-1 block text-sm font-medium">
                  Género <span className="text-brand-danger">*</span>
                </label>
                <select
                  id="genero"
                  value={form.genero}
                  onChange={(e) => setForm((f) => ({ ...f, genero: e.target.value as Genero }))}
                  className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                    fieldErrors.genero
                      ? 'border-brand-danger focus:ring-brand-danger/25'
                      : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                  }`}
                >
                  <option value="">Seleccione un género</option>
                  <option value="HOMBRE">Masculino</option>
                  <option value="MUJER">Femenino</option>
                </select>
                {fieldErrors.genero && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.genero}</p>}
              </div>
            </div>

            <div>
              <label htmlFor="descripcion" className="mb-1 block text-sm font-medium">
                Descripción
              </label>
              <textarea
                id="descripcion"
                rows={3}
                value={form.descripcion}
                onChange={(e) => setForm((f) => ({ ...f, descripcion: e.target.value }))}
                placeholder="Descripción detallada del producto"
                className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
              />
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="categoryId" className="mb-1 block text-sm font-medium">
                  Categoría <span className="text-brand-danger">*</span>
                </label>
                <select
                  id="categoryId"
                  value={form.categoryId}
                  onChange={(e) => setForm((f) => ({ ...f, categoryId: e.target.value }))}
                  className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                    fieldErrors.categoryId
                      ? 'border-brand-danger focus:ring-brand-danger/25'
                      : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                  }`}
                >
                  <option value="">Seleccione una categoría</option>
                  {categorias.map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {cat.nombre}
                    </option>
                  ))}
                </select>
                {fieldErrors.categoryId && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.categoryId}</p>}
              </div>

              <div>
                <label htmlFor="brandId" className="mb-1 block text-sm font-medium">
                  Marca <span className="text-brand-danger">*</span>
                </label>
                <select
                  id="brandId"
                  value={form.brandId}
                  onChange={(e) => setForm((f) => ({ ...f, brandId: e.target.value }))}
                  className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                    fieldErrors.brandId
                      ? 'border-brand-danger focus:ring-brand-danger/25'
                      : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                  }`}
                >
                  <option value="">Seleccione una marca</option>
                  {marcas.map((marca) => (
                    <option key={marca.id} value={marca.id}>
                      {marca.nombre}
                    </option>
                  ))}
                </select>
                {fieldErrors.brandId && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.brandId}</p>}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <div>
                <label htmlFor="precio" className="mb-1 block text-sm font-medium">
                  Precio (S/.) <span className="text-brand-danger">*</span>
                </label>
                <input
                  id="precio"
                  type="number"
                  step="0.01"
                  min="0"
                  value={form.precio}
                  onChange={(e) => setForm((f) => ({ ...f, precio: e.target.value }))}
                  placeholder="0.00"
                  className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                    fieldErrors.precio
                      ? 'border-brand-danger focus:ring-brand-danger/25'
                      : 'border-slate-300 focus:border-brand-primary focus:ring-brand-primary/25'
                  }`}
                />
                {fieldErrors.precio && <p className="mt-1 text-sm text-brand-danger">{fieldErrors.precio}</p>}
              </div>
              <div>
                <label htmlFor="descuento" className="mb-1 block text-sm font-medium">
                  Descuento (%)
                </label>
                <input
                  id="descuento"
                  type="number"
                  step="0.01"
                  min="0"
                  max="100"
                  value={form.descuento}
                  onChange={(e) => setForm((f) => ({ ...f, descuento: e.target.value }))}
                  placeholder="0"
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
                />
              </div>
              <div>
                <label htmlFor="stock" className="mb-1 block text-sm font-medium">
                  Stock
                </label>
                <input
                  id="stock"
                  type="number"
                  min="0"
                  value={form.stock}
                  onChange={(e) => setForm((f) => ({ ...f, stock: e.target.value }))}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
                />
              </div>
              <div>
                <label htmlFor="stockMinimo" className="mb-1 block text-sm font-medium">
                  Stock Mínimo
                </label>
                <input
                  id="stockMinimo"
                  type="number"
                  min="0"
                  value={form.stockMinimo}
                  onChange={(e) => setForm((f) => ({ ...f, stockMinimo: e.target.value }))}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
                />
              </div>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium">Imagen del Producto</label>
              <div className="flex flex-col gap-2 sm:flex-row">
                <input
                  type="url"
                  value={form.imagen}
                  onChange={(e) => setForm((f) => ({ ...f, imagen: e.target.value }))}
                  placeholder="https://ejemplo.com/imagen.jpg"
                  className="flex-1 rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
                />
                <label className="flex cursor-pointer items-center justify-center gap-2 rounded-lg bg-brand-primary px-4 py-2 font-medium text-white transition hover:bg-brand-primary-hover">
                  <i className="bi bi-cloud-upload" />
                  {uploading ? 'Subiendo...' : 'Subir archivo'}
                  <input type="file" accept="image/*" onChange={handleFileChange} disabled={uploading} className="hidden" />
                </label>
              </div>
              {form.imagen && (
                <div className="mt-3">
                  <ImageThumbnail src={form.imagen} alt="Imagen de producto" />
                </div>
              )}
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <label className="flex items-center gap-2 text-sm font-medium">
                <input
                  type="checkbox"
                  checked={form.destacado}
                  onChange={(e) => setForm((f) => ({ ...f, destacado: e.target.checked }))}
                  className="h-4 w-4 accent-brand-primary"
                />
                <i className="bi bi-star-fill text-amber-400" />
                Producto Destacado
              </label>
              <label className="flex items-center gap-2 text-sm font-medium">
                <input
                  type="checkbox"
                  checked={form.estado}
                  onChange={(e) => setForm((f) => ({ ...f, estado: e.target.checked }))}
                  className="h-4 w-4 accent-brand-primary"
                />
                <i className="bi bi-check-circle-fill text-brand-success" />
                Producto Activo
              </label>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
