import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const BACKEND_URL = 'http://localhost:8082'

// Solo se proxean las sub-rutas /api de cada dominio: la ruta base (p. ej. /categorias)
// la debe seguir sirviendo el SPA de Vite, no la vista Thymeleaf del backend, o un refresh
// de página en una ruta de React chocaría con el backend en vez de mostrar la app.
const PROXIED_PATHS = [
  '/marcas/api',
  '/categorias/api',
  '/clientes/api',
  '/productos/api',
  '/usuarios/api',
  '/perfiles/api',
  '/ventas/api',
  '/creditos/api',
  '/inventario/api',
  '/personalizacion/api',
  '/admin/api',
  '/carrito',
  '/api',
  '/uploads',
]

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: Object.fromEntries(
      PROXIED_PATHS.map((path) => [path, { target: BACKEND_URL, changeOrigin: true }]),
    ),
  },
})
