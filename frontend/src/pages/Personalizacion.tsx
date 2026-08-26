import { useEffect, useState } from 'react'
import type { ChangeEvent } from 'react'
import Swal from 'sweetalert2'
import Modal from '../components/ui/Modal'
import { apiClient, ApiError, uploadFile } from '../lib/apiClient'
import type { Brand, PersonalizacionSlot } from '../types'

function notify(message: string, icon: 'success' | 'error' | 'info' = 'info') {
  Swal.fire({ title: message, icon, toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true })
}

function SlidePreview({ slide }: { slide: PersonalizacionSlot }) {
  const tieneImagen = !!slide.imagenUrl
  return (
    <div className="relative flex aspect-[3/2] w-full items-center justify-center overflow-hidden rounded-lg border-2 border-dashed border-slate-300 bg-slate-50">
      <span className="absolute left-2.5 top-2.5 flex h-8 w-8 items-center justify-center rounded-full bg-brand-primary/90 text-sm font-bold text-white">
        {slide.orden}
      </span>
      {tieneImagen ? (
        <img src={slide.imagenUrl ?? undefined} alt={`Slide ${slide.orden}`} className="h-full w-full object-cover" />
      ) : (
        <div className="p-4 text-center text-slate-400">
          <i className="bi bi-image text-4xl" />
          <p className="mt-2 mb-0 text-sm">Slide vacío</p>
          <small className="text-xs">1080x720px</small>
        </div>
      )}
    </div>
  )
}

export default function Personalizacion() {
  const [logo, setLogo] = useState<PersonalizacionSlot | null>(null)
  const [slides, setSlides] = useState<PersonalizacionSlot[]>([])
  const [marcas, setMarcas] = useState<Brand[]>([])
  const [loading, setLoading] = useState(true)

  const [uploadingLogo, setUploadingLogo] = useState(false)
  const [uploadingSlide, setUploadingSlide] = useState<number | null>(null)

  const [modalOrden, setModalOrden] = useState<number | null>(null)
  const [selectedMarcaId, setSelectedMarcaId] = useState('')
  const [savingMarca, setSavingMarca] = useState(false)

  useEffect(() => {
    loadAll()
  }, [])

  async function loadAll() {
    setLoading(true)
    await Promise.all([loadLogo(), loadSlides(), loadMarcas()])
    setLoading(false)
  }

  async function loadLogo() {
    try {
      const res = await apiClient.get<PersonalizacionSlot>('/personalizacion/api/logo')
      setLogo((res.data as PersonalizacionSlot) ?? null)
    } catch {
      setLogo(null)
    }
  }

  async function loadSlides() {
    try {
      const res = await apiClient.get<PersonalizacionSlot[]>('/personalizacion/api/slides/con-marca')
      setSlides(((res.data as PersonalizacionSlot[]) ?? []).sort((a, b) => (a.orden ?? 0) - (b.orden ?? 0)))
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al cargar slides', 'error')
    }
  }

  async function loadMarcas() {
    try {
      const res = await apiClient.get<Brand[]>('/personalizacion/api/marcas-disponibles')
      setMarcas((res.data as Brand[]) ?? [])
    } catch {
      // El selector de marcas no es crítico para ver la pantalla.
    }
  }

  async function handleLogoFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    setUploadingLogo(true)
    try {
      const uploadRes = await uploadFile('/api/upload/personalizacion/imagen', file)
      const url = uploadRes.url as string
      await apiClient.put(`/personalizacion/api/logo/imagen?imagenUrl=${encodeURIComponent(url)}`)
      notify('Logo actualizado correctamente', 'success')
      await loadLogo()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al subir el logo', 'error')
    } finally {
      setUploadingLogo(false)
    }
  }

  async function handleEliminarLogo() {
    const result = await Swal.fire({
      title: '¿Eliminar logo?',
      text: 'Esta acción eliminará el logo actual del sitio',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
    })
    if (!result.isConfirmed) return

    try {
      await apiClient.delete('/personalizacion/api/logo/imagen')
      notify('Logo eliminado exitosamente', 'success')
      await loadLogo()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al eliminar el logo', 'error')
    }
  }

  async function handleSlideFileChange(event: ChangeEvent<HTMLInputElement>, orden: number) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    setUploadingSlide(orden)
    try {
      const uploadRes = await uploadFile('/api/upload/personalizacion/imagen', file)
      const url = uploadRes.url as string
      await apiClient.put(`/personalizacion/api/slide/${orden}/imagen?imagenUrl=${encodeURIComponent(url)}`)
      notify('El slide se ha actualizado correctamente', 'success')
      await loadSlides()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al subir la imagen', 'error')
    } finally {
      setUploadingSlide(null)
    }
  }

  function abrirModalAsignarMarca(slide: PersonalizacionSlot) {
    setModalOrden(slide.orden)
    setSelectedMarcaId(slide.marca ? String(slide.marca.id) : '')
  }

  async function handleGuardarMarca() {
    if (modalOrden == null) return
    setSavingMarca(true)
    try {
      const query = selectedMarcaId ? `?marcaId=${selectedMarcaId}` : ''
      await apiClient.put(`/personalizacion/api/slide/${modalOrden}/marca${query}`)
      notify('¡Marca actualizada!', 'success')
      setModalOrden(null)
      await loadSlides()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al asignar la marca', 'error')
    } finally {
      setSavingMarca(false)
    }
  }

  async function handleLimpiarSlide(orden: number) {
    const result = await Swal.fire({
      title: '¿Limpiar slide?',
      text: 'Se eliminará la imagen y la marca asignada',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, limpiar',
      cancelButtonText: 'Cancelar',
    })
    if (!result.isConfirmed) return

    try {
      await apiClient.delete(`/personalizacion/api/slide/${orden}`)
      notify('Slide limpiado exitosamente', 'success')
      await loadSlides()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Error al limpiar el slide', 'error')
    }
  }

  if (loading) {
    return <div className="flex min-h-[60vh] items-center justify-center text-slate-500">Cargando...</div>
  }

  return (
    <div>
      <header className="border-b bg-white p-3 shadow-sm md:p-4">
        <h1 className="flex items-center gap-2 text-xl font-bold md:text-2xl">
          <i className="bi bi-palette text-brand-primary" /> Centro de Personalización
        </h1>
        <div className="mt-3 rounded-lg border border-sky-200 bg-sky-50 px-4 py-2 text-sm text-sky-800">
          <i className="bi bi-info-circle-fill me-2" />
          <strong>Información:</strong> Aquí puedes configurar el logo y los slides del carrusel principal de tu sitio web.
          <br />
          <small>Logo: 500x500px sin fondo | Slides: 1080x720px | Máximo 5 slides</small>
        </div>
      </header>

      <div className="space-y-5 p-4 md:p-6">
        <div className="overflow-hidden rounded-xl bg-white shadow-sm">
          <div className="bg-brand-primary px-5 py-3 text-white">
            <h2 className="flex items-center gap-2 text-sm font-bold">
              <i className="bi bi-star-fill" /> Logo del Sitio
            </h2>
          </div>
          <div className="grid grid-cols-1 gap-6 p-5 md:grid-cols-3">
            <div className="mx-auto w-full max-w-[220px]">
              <div className="flex aspect-square w-full items-center justify-center overflow-hidden rounded-lg border-2 border-dashed border-slate-300 bg-slate-50">
                {logo?.imagenUrl ? (
                  <img src={logo.imagenUrl} alt="Logo" className="h-full w-full object-contain" />
                ) : (
                  <div className="p-4 text-center text-slate-400">
                    <i className="bi bi-image text-4xl" />
                    <p className="mt-2 mb-0 text-sm">Sin logo</p>
                    <small className="text-xs">500x500px sin fondo</small>
                  </div>
                )}
              </div>
            </div>
            <div className="md:col-span-2">
              <h3 className="mb-2 font-bold">Gestión del Logo</h3>
              <p className="mb-4 text-sm text-slate-500">
                El logo se mostrará en la cabecera de tu sitio web. Asegúrate de usar una imagen con fondo transparente en formato PNG.
              </p>

              <label className="mb-3 flex w-full max-w-xs cursor-pointer items-center justify-center gap-2 rounded-lg bg-brand-primary px-4 py-2.5 font-medium text-white transition hover:bg-brand-primary-hover">
                <i className="bi bi-cloud-upload" />
                {uploadingLogo ? 'Subiendo...' : 'Subir Nuevo Logo'}
                <input type="file" accept="image/png,image/jpeg,image/jpg,image/webp" onChange={handleLogoFileChange} disabled={uploadingLogo} className="hidden" />
              </label>

              <button
                onClick={handleEliminarLogo}
                disabled={!logo?.imagenUrl}
                className="flex items-center gap-2 rounded-lg bg-brand-danger px-4 py-2.5 font-medium text-white transition hover:bg-brand-danger-hover disabled:opacity-40"
              >
                <i className="bi bi-trash" /> Eliminar Logo
              </button>

              <div className="mt-4 rounded-lg border border-amber-300 bg-amber-50 px-4 py-2 text-sm text-amber-800">
                <i className="bi bi-exclamation-triangle me-2" />
                <strong>Recomendación:</strong> Usa formato PNG con fondo transparente para mejor visualización.
              </div>
            </div>
          </div>
        </div>

        <div className="overflow-hidden rounded-xl bg-white shadow-sm">
          <div className="bg-brand-success px-5 py-3 text-white">
            <h2 className="flex items-center gap-2 text-sm font-bold">
              <i className="bi bi-images" /> Slides del Carrusel Principal
            </h2>
          </div>
          <div className="p-5">
            <p className="mb-4 text-sm text-slate-500">
              Configura hasta 5 slides para el carrusel principal. Cada slide puede estar asociado a una marca y redirigirá al catálogo de productos al hacer clic.
            </p>

            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {slides.map((slide) => {
                const orden = slide.orden as number
                const tieneImagen = !!slide.imagenUrl
                return (
                  <div key={slide.id} className="rounded-xl border border-slate-200 p-4 shadow-sm transition hover:-translate-y-1 hover:shadow-md">
                    <div className="mb-3">
                      <SlidePreview slide={slide} />
                    </div>

                    <div className={`mb-3 flex items-center gap-2 rounded-lg px-3 py-2 text-sm ${slide.marca ? 'bg-blue-50' : 'bg-amber-50'}`}>
                      <i className="bi bi-tag" />
                      <span>{slide.marca ? slide.marca.nombre : 'Sin marca asignada'}</span>
                    </div>

                    <div className="grid gap-2">
                      <label className="flex cursor-pointer items-center justify-center gap-2 rounded-lg bg-brand-primary px-3 py-2 text-sm font-medium text-white transition hover:bg-brand-primary-hover">
                        <i className="bi bi-cloud-upload" />
                        {uploadingSlide === orden ? 'Subiendo...' : 'Subir Imagen'}
                        <input
                          type="file"
                          accept="image/*"
                          onChange={(e) => handleSlideFileChange(e, orden)}
                          disabled={uploadingSlide === orden}
                          className="hidden"
                        />
                      </label>
                      <button
                        onClick={() => abrirModalAsignarMarca(slide)}
                        className="flex items-center justify-center gap-2 rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
                      >
                        <i className="bi bi-tag" /> Asignar Marca
                      </button>
                      <button
                        onClick={() => handleLimpiarSlide(orden)}
                        disabled={!tieneImagen}
                        className="flex items-center justify-center gap-2 rounded-lg border border-brand-danger px-3 py-2 text-sm font-medium text-brand-danger transition hover:bg-brand-danger hover:text-white disabled:opacity-40"
                      >
                        <i className="bi bi-trash" /> Limpiar
                      </button>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        </div>
      </div>

      {modalOrden !== null && (
        <Modal
          title="Asignar Marca al Slide"
          onClose={() => setModalOrden(null)}
          footer={
            <>
              <button
                type="button"
                onClick={() => setModalOrden(null)}
                className="rounded-lg bg-slate-200 px-4 py-2 font-medium text-slate-700 transition hover:bg-slate-300"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleGuardarMarca}
                disabled={savingMarca}
                className="rounded-lg bg-brand-success px-4 py-2 font-medium text-white transition hover:bg-emerald-600 disabled:opacity-60"
              >
                {savingMarca ? 'Guardando...' : 'Guardar'}
              </button>
            </>
          }
        >
          <label htmlFor="selectMarca" className="mb-1 block text-sm font-medium">
            Selecciona una marca:
          </label>
          <select
            id="selectMarca"
            value={selectedMarcaId}
            onChange={(e) => setSelectedMarcaId(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-brand-primary focus:outline-none focus:ring-2 focus:ring-brand-primary/25"
          >
            <option value="">Sin marca</option>
            {marcas.map((m) => (
              <option key={m.id} value={m.id}>
                {m.nombre}
              </option>
            ))}
          </select>
          <p className="mt-1 text-xs text-slate-400">Al hacer clic en el slide, se redirigirá al catálogo de productos de la marca seleccionada.</p>
        </Modal>
      )}
    </div>
  )
}
