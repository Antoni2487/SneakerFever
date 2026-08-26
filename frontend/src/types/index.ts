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

export interface Brand {
  id: number
  nombre: string
  descripcion: string | null
  estado: number
  imagen: string | null
  imagenes: string[]
  fechaCreacion: string
  fechaActualizacion: string
}

export interface BrandFormData {
  nombre: string
  imagen: string
}

export type Genero = 'HOMBRE' | 'MUJER'

export interface Product {
  id: number
  nombre: string
  descripcion: string | null
  imagen: string | null
  imagenes: string[]
  precio: number
  descuento: number | null
  destacado: boolean | null
  stock: number
  stockMinimo: number
  genero: Genero
  category: Category | null
  brand: Brand | null
  estado: number
  fechaCreacion: string
  fechaActualizacion: string
}

export interface ProductFormData {
  nombre: string
  descripcion: string
  imagen: string
  precio: string
  descuento: string
  stock: string
  stockMinimo: string
  genero: Genero | ''
  categoryId: string
  brandId: string
  destacado: boolean
  estado: boolean
}

export interface UsuarioFormData {
  nombre: string
  usuario: string
  clave: string
  correo: string
  perfilId: string
}
