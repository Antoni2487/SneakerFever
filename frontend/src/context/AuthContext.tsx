import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import type { Usuario } from '../types'
import { apiClient } from '../lib/apiClient'

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
    apiClient
      .get<never>('/api/auth/me')
      .then((res) => setUsuario((res.usuario as Usuario) ?? null))
      .catch(() => setUsuario(null))
      .finally(() => setLoading(false))
  }, [])

  async function login(usuarioInput: string, clave: string) {
    const res = await apiClient.post<never>('/api/auth/login', { usuario: usuarioInput, clave })
    setUsuario((res.usuario as Usuario) ?? null)
  }

  async function logout() {
    try {
      await apiClient.post('/api/auth/logout')
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
