import { useCallback, useEffect, useRef, useState } from 'react'
import { apiClient, ApiError } from './apiClient'

/** Respuesta de los endpoints GET /xxx/api/pagina del backend (page empieza en 0). */
interface PageEnvelope<T> {
  data?: T[]
  page?: number
  totalElements?: number
  totalPages?: number
}

interface Options {
  pageSize?: number
  /** Espera tras la última tecla antes de buscar en el servidor. */
  debounceMs?: number
  onError?: (message: string) => void
}

/**
 * Listado paginado y filtrado en el servidor: la página actual, el texto de
 * búsqueda (con espera para no consultar en cada tecla) y una `reload()` para
 * refrescar la misma página después de crear, editar o eliminar.
 */
export function usePaginatedList<T>(endpoint: string, { pageSize = 10, debounceMs = 300, onError }: Options = {}) {
  const [items, setItems] = useState<T[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [search, setSearchState] = useState('')
  const [query, setQuery] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  // Última función de error sin volver a disparar la carga cuando el padre la recrea.
  const onErrorRef = useRef(onError)
  useEffect(() => {
    onErrorRef.current = onError
  }, [onError])

  useEffect(() => {
    const next = search.trim()
    // Solo si el texto cambió de verdad: si no, al montar (o al tipear y borrar)
    // el temporizador devolvería a la página 1 a quien ya pasó de página.
    if (next === query) return
    const timer = setTimeout(() => {
      setQuery(next)
      setPage(0)
    }, debounceMs)
    return () => clearTimeout(timer)
  }, [search, query, debounceMs])

  useEffect(() => {
    let cancelled = false
    const params = new URLSearchParams({ page: String(page), size: String(pageSize) })
    if (query) params.set('q', query)

    setLoading(true)
    apiClient
      .get<T[]>(`${endpoint}?${params.toString()}`)
      .then((res) => {
        if (cancelled) return
        const body = res as unknown as PageEnvelope<T>
        const total = body.totalPages ?? 0
        // Si la página quedó fuera de rango (p. ej. se eliminó el último
        // elemento de la última página), retrocede a la última existente.
        if (total > 0 && page >= total) {
          setPage(total - 1)
          return
        }
        setItems(body.data ?? [])
        setTotalPages(total)
        setTotalElements(body.totalElements ?? 0)
      })
      .catch((error) => {
        if (!cancelled) onErrorRef.current?.(error instanceof ApiError ? error.message : 'Error al cargar los datos')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [endpoint, page, pageSize, query, reloadKey])

  const reload = useCallback(() => setReloadKey((k) => k + 1), [])

  return {
    items,
    loading,
    page,
    pageSize,
    totalPages,
    totalElements,
    search,
    setSearch: setSearchState,
    setPage,
    reload,
  }
}
