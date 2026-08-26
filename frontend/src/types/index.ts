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

export interface Cliente {
  id: number
  nombre: string
  documento: string
  telefono: string
  correo: string | null
  estado: number
  fechaCreacion: string
  fechaActualizacion: string
}

export interface ClienteFormData {
  nombre: string
  documento: string
  telefono: string
  correo: string
}

// GET /perfiles/api/{id} devuelve las opciones como un set de IDs, no como
// objetos completos (a diferencia de PerfilResponse, que usa OpcionResponse[]).
export interface PerfilDetalle {
  id: number
  nombre: string
  descripcion: string | null
  estado: boolean
  opciones: number[]
}

export interface PerfilFormData {
  nombre: string
  descripcion: string
}

export type EstadoVenta = 'PENDIENTE' | 'PAGADA' | 'ANULADA'
export type FormaPago = 'CONTADO' | 'CREDITO'
export type TipoComprobante = 'BOLETA' | 'FACTURA' | 'NOTA_VENTA'
export type IntervaloCredito = 'SEMANAL' | 'QUINCENAL' | 'MENSUAL'
export type EstadoCredito = 'ACTIVO' | 'PAGADO' | 'VENCIDO' | 'CANCELADO'
export type EstadoCuota = 'PENDIENTE' | 'PAGADA' | 'PARCIAL' | 'VENCIDA'
export type MetodoPago = 'EFECTIVO' | 'TRANSFERENCIA' | 'YAPE' | 'PLIN' | 'TARJETA'

export interface ProductoDisponible {
  id: number
  nombre: string
  precio: number
  stock: number
  imagen: string | null
  descuento: number | null
  codigo: string
}

// Item en el carrito de la venta (antes de enviarse al backend)
export interface CarritoItem {
  productoId: number
  productoNombre: string
  codigo: string
  cantidad: number
  precioUnitario: number
  descuentoPorcentaje: number
  subtotal: number
  stockDisponible: number
}

export interface ClienteVentaSeleccionado {
  id: number | null
  nombre: string
  documento: string
  telefono: string | null
  correo: string | null
  esNuevo: boolean
}

// VentaResponse/CreditoVentaResponse/CuotaPagoResponse serializan con @JsonProperty en
// snake_case explícito (no el camelCase por defecto de Jackson que usan el resto de los
// DTOs) — estos tipos reflejan el JSON real, verificado contra el backend.
export interface DetalleVentaResponse {
  id: number
  producto_id: number
  producto_nombre: string
  cantidad: number
  precio_unitario: number
  descuento_porcentaje: number
  subtotal: number
}

export interface Venta {
  id: number
  cliente_id: number
  cliente_nombre: string
  cliente_documento: string
  tipo_comprobante: TipoComprobante
  serie: string
  numero: string
  comprobante_completo: string
  forma_pago: FormaPago
  subtotal: number
  descuento_general: number
  igv: number
  total: number
  estado: EstadoVenta
  observaciones: string | null
  usuario_creacion: string
  fecha_creacion: string
  fecha_actualizacion: string
  detalles: DetalleVentaResponse[]
  tiene_credito: boolean
}

// Este endpoint sí devuelve un Map plano (sin @JsonProperty), por eso queda en camelCase.
export interface VentaEstadisticas {
  ventasHoy: number
  ventasSemana: number
  ventasMes: number
  totalVentas: number
}

export interface CuotaPago {
  id: number
  credito_id: number
  numero_cuota: number
  monto_cuota: number
  fecha_vencimiento: string
  monto_pagado: number
  saldo_pendiente: number
  fecha_pago: string | null
  estado: EstadoCuota
  dias_para_vencer: number | null
  dias_vencida: number | null
  porcentaje_pagado: number
}

export interface CreditoVenta {
  id: number
  venta_id: number
  comprobante_completo: string
  cliente_id: number
  cliente_nombre: string
  cliente_documento: string
  monto_total: number
  interes_porcentaje: number
  monto_con_interes: number
  numero_cuotas: number
  monto_inicial: number
  intervalo_cuotas: IntervaloCredito
  fecha_inicio: string
  fecha_fin: string
  monto_pagado: number
  saldo_pendiente: number
  tipo_comprobante: string
  serie: string
  numero: string
  estado: EstadoCredito
  fecha_creacion: string
  fecha_actualizacion: string
  cuotas: CuotaPago[]
  porcentaje_pagado: number
  dias_restantes: number | null
}
