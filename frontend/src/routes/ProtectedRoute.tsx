import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function ProtectedRoute() {
  const { usuario, loading } = useAuth()

  if (loading) {
    return <div className="flex min-h-screen items-center justify-center text-slate-500">Cargando...</div>
  }

  if (!usuario) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
