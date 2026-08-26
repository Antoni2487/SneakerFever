import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const BACKEND_URL = 'http://localhost:8082'

const PROXIED_PATHS = [
  '/marcas',
  '/categorias',
  '/clientes',
  '/productos',
  '/usuarios',
  '/perfiles',
  '/ventas',
  '/creditos',
  '/inventario',
  '/personalizacion',
  '/carrito',
  '/login',
  '/logout',
  '/api',
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
