import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import type { Usuario } from '../types'
import { apiClient, loginRequest, logoutRequest, onAuthFailure, refreshAccessToken, setAccessToken } from '../lib/apiClient'

interface AuthContextValue {
  usuario: Usuario | null
  loading: boolean
  login: (usuario: string, clave: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    // Si el refresh token (cookie httpOnly) sigue vigente, lo cambiamos por un
    // access token nuevo antes de considerar a alguien autenticado — el access
    // token en memoria no sobrevive a la recarga de página, la cookie sí.
    async function bootstrap() {
      const token = await refreshAccessToken()
      if (!token) {
        if (!cancelled) setUsuario(null)
        if (!cancelled) setLoading(false)
        return
      }
      try {
        const res = await apiClient.get<never>('/api/auth/me')
        if (!cancelled) setUsuario((res.usuario as Usuario) ?? null)
      } catch {
        if (!cancelled) setUsuario(null)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    bootstrap()
    onAuthFailure(() => {
      setAccessToken(null)
      if (!cancelled) setUsuario(null)
    })

    return () => {
      cancelled = true
      onAuthFailure(null)
    }
  }, [])

  async function login(usuarioInput: string, clave: string) {
    const res = await loginRequest(usuarioInput, clave)
    setUsuario((res.usuario as Usuario) ?? null)
  }

  async function logout() {
    try {
      await logoutRequest()
    } finally {
      setUsuario(null)
    }
  }

  return <AuthContext.Provider value={{ usuario, loading, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
