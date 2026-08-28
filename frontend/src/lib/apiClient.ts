export interface ApiEnvelope<T = unknown> {
  success: boolean
  message?: string
  errors?: Record<string, string>
  data?: T
  [key: string]: unknown
}

export class ApiError extends Error {
  status: number
  errors?: Record<string, string>

  constructor(message: string, status: number, errors?: Record<string, string>) {
    super(message)
    this.status = status
    this.errors = errors
  }
}

// Access token en memoria (variable de módulo), NUNCA en localStorage/sessionStorage:
// así queda inalcanzable para un XSS. Se pierde a propósito en cada recarga de
// página — AuthContext lo vuelve a obtener al arrancar llamando a refreshAccessToken(),
// que sí sobrevive a la recarga a través de la cookie httpOnly del refresh token.
let accessToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
}

// Se notifica cuando una petición queda definitivamente sin autenticar (401 que
// ni el refresh pudo resolver) para que AuthContext limpie el usuario y
// ProtectedRoute redirija a /login. apiClient no conoce React/Context, así que
// expone este único punto de enganche en vez de importar el contexto.
type AuthFailureHandler = () => void
let authFailureHandler: AuthFailureHandler | null = null

export function onAuthFailure(handler: AuthFailureHandler | null): void {
  authFailureHandler = handler
}

async function parseEnvelope<T>(response: Response): Promise<ApiEnvelope<T>> {
  let body: ApiEnvelope<T>
  try {
    body = await response.json()
  } catch {
    throw new ApiError('Error de conexión con el servidor', response.status)
  }

  if (!response.ok || body.success === false) {
    throw new ApiError(body.message ?? 'Error de conexión con el servidor', response.status, body.errors)
  }

  return body
}

// Evita disparar varios POST /api/auth/refresh en paralelo cuando varias
// peticiones reciben 401 casi al mismo tiempo — todas esperan la misma promesa.
let refreshPromise: Promise<string | null> | null = null

export function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' })
      .then(async (response) => {
        if (!response.ok) {
          setAccessToken(null)
          return null
        }
        const body = (await response.json()) as ApiEnvelope<never>
        const token = typeof body.accessToken === 'string' ? body.accessToken : null
        setAccessToken(token)
        return token
      })
      .catch(() => {
        setAccessToken(null)
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

function authHeaders(hasBody: boolean, extra?: HeadersInit): HeadersInit {
  const token = getAccessToken()
  return {
    ...(hasBody ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extra,
  }
}

async function request<T = unknown>(path: string, options: RequestInit = {}, allowRetry = true): Promise<ApiEnvelope<T>> {
  const response = await fetch(path, {
    ...options,
    headers: authHeaders(Boolean(options.body), options.headers),
  })

  if (response.status === 401) {
    if (allowRetry) {
      const newToken = await refreshAccessToken()
      if (newToken) {
        return request<T>(path, options, false)
      }
    }
    authFailureHandler?.()
  }

  return parseEnvelope<T>(response)
}

export const apiClient = {
  get: <T = unknown>(path: string) => request<T>(path),
  post: <T = unknown>(path: string, data?: unknown) =>
    request<T>(path, { method: 'POST', body: data !== undefined ? JSON.stringify(data) : undefined }),
  put: <T = unknown>(path: string, data?: unknown) =>
    request<T>(path, { method: 'PUT', body: data !== undefined ? JSON.stringify(data) : undefined }),
  delete: <T = unknown>(path: string) => request<T>(path, { method: 'DELETE' }),
}

async function uploadRequest(path: string, formData: FormData, allowRetry = true): Promise<ApiEnvelope> {
  const response = await fetch(path, {
    method: 'POST',
    headers: authHeaders(false),
    body: formData,
  })

  if (response.status === 401) {
    if (allowRetry) {
      const newToken = await refreshAccessToken()
      if (newToken) {
        return uploadRequest(path, formData, false)
      }
    }
    authFailureHandler?.()
  }

  return parseEnvelope(response)
}

export async function uploadFile(path: string, file: File, fieldName = 'file'): Promise<ApiEnvelope> {
  const formData = new FormData()
  formData.append(fieldName, file)
  return uploadRequest(path, formData)
}

// --- Autenticación: los únicos puntos que hablan con la cookie httpOnly del refresh token ---

export interface LoginResult {
  accessToken: string
  usuario: unknown
}

export async function loginRequest(usuario: string, clave: string): Promise<LoginResult> {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ usuario, clave }),
  })
  const body = await parseEnvelope<never>(response)
  const token = typeof body.accessToken === 'string' ? body.accessToken : null
  if (!token) {
    throw new ApiError('El servidor no devolvió un access token', response.status)
  }
  setAccessToken(token)
  return { accessToken: token, usuario: body.usuario }
}

export async function logoutRequest(): Promise<void> {
  try {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })
  } finally {
    setAccessToken(null)
  }
}
