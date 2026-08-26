import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './routes/ProtectedRoute'
import AdminLayout from './components/layout/AdminLayout'
import PublicLayout from './components/public/PublicLayout'
import Login from './pages/Login'
import Categorias from './pages/Categorias'
import Marcas from './pages/Marcas'
import Productos from './pages/Productos'
import Usuarios from './pages/Usuarios'
import Clientes from './pages/Clientes'
import Perfiles from './pages/Perfiles'
import Ventas from './pages/Ventas'
import Dashboard from './pages/Dashboard'
import Inventario from './pages/Inventario'
import Personalizacion from './pages/Personalizacion'
import Placeholder from './pages/Placeholder'
import Landing from './pages/public/Landing'
import Catalogo from './pages/public/Catalogo'
import ProductoDetalle from './pages/public/ProductoDetalle'
import PublicPlaceholder from './pages/public/PublicPlaceholder'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />

        {/* Sitio público - sin autenticación */}
        <Route element={<PublicLayout />}>
          <Route path="/" element={<Landing />} />
          <Route path="/catalogo" element={<Catalogo />} />
          <Route path="/producto/:id" element={<ProductoDetalle />} />
          <Route path="/contacto" element={<PublicPlaceholder />} />
          <Route path="/carrito" element={<PublicPlaceholder />} />
        </Route>

        {/* Panel admin - requiere sesión */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AdminLayout />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/categorias" element={<Categorias />} />
            <Route path="/marcas" element={<Marcas />} />
            <Route path="/productos" element={<Productos />} />
            <Route path="/usuarios" element={<Navigate to="/usuarios/listar" replace />} />
            <Route path="/usuarios/listar" element={<Usuarios />} />
            <Route path="/clientes" element={<Clientes />} />
            <Route path="/perfiles" element={<Navigate to="/perfiles/listar" replace />} />
            <Route path="/perfiles/listar" element={<Perfiles />} />
            <Route path="/ventas" element={<Ventas />} />
            <Route path="/inventario" element={<Inventario />} />
            <Route path="/personalizacion" element={<Personalizacion />} />
            <Route path="*" element={<Placeholder />} />
          </Route>
        </Route>
      </Routes>
    </AuthProvider>
  )
}
