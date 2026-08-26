export interface Opcion {
  id: number
  nombre: string
  ruta: string
  icono: string | null
}

export interface Perfil {
  id: number
  nombre: string
  descripcion: string | null
  estado: boolean
  opciones: Opcion[]
}

export interface Usuario {
  id: number
  nombre: string
  usuario: string
  correo: string
  estado: number
  perfil: Perfil | null
}

export interface Category {
  id: number
  nombre: string
  descripcion: string | null
  estado: number
  fechaCreacion: string
  fechaActualizacion: string
}

export interface CategoryFormData {
  nombre: string
  descripcion: string
}
