import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './routes/ProtectedRoute'
import AdminLayout from './components/layout/AdminLayout'
import Login from './pages/Login'
import Categorias from './pages/Categorias'
import Marcas from './pages/Marcas'
import Productos from './pages/Productos'
import Usuarios from './pages/Usuarios'
import Clientes from './pages/Clientes'
import Placeholder from './pages/Placeholder'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AdminLayout />}>
            <Route path="/" element={<Navigate to="/categorias" replace />} />
            <Route path="/categorias" element={<Categorias />} />
            <Route path="/marcas" element={<Marcas />} />
            <Route path="/productos" element={<Productos />} />
            <Route path="/usuarios" element={<Navigate to="/usuarios/listar" replace />} />
            <Route path="/usuarios/listar" element={<Usuarios />} />
            <Route path="/clientes" element={<Clientes />} />
            <Route path="*" element={<Placeholder />} />
          </Route>
        </Route>
      </Routes>
    </AuthProvider>
  )
}
