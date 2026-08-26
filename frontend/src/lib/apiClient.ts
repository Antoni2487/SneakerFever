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

async function request<T = unknown>(path: string, options: RequestInit = {}): Promise<ApiEnvelope<T>> {
  const response = await fetch(path, {
    credentials: 'include',
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  })

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

export const apiClient = {
  get: <T = unknown>(path: string) => request<T>(path),
  post: <T = unknown>(path: string, data?: unknown) =>
    request<T>(path, { method: 'POST', body: data !== undefined ? JSON.stringify(data) : undefined }),
  put: <T = unknown>(path: string, data?: unknown) =>
    request<T>(path, { method: 'PUT', body: data !== undefined ? JSON.stringify(data) : undefined }),
  delete: <T = unknown>(path: string) => request<T>(path, { method: 'DELETE' }),
}
