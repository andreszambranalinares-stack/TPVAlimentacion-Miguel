// Tipos espejo de los DTOs del backend

export type UnidadMedida = 'UNIDAD' | 'KG' | 'LITRO'
export type MetodoPago = 'EFECTIVO' | 'TARJETA'
export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'AJUSTE' | 'MERMA'

export interface Categoria {
  id: number
  nombre: string
}

export interface Producto {
  id: number
  nombre: string
  codigoBarras: string | null
  categoriaId: number | null
  categoriaNombre: string | null
  precioVenta: number
  precioCoste: number
  ivaPorcentaje: number
  stockActual: number
  stockMinimo: number
  unidadMedida: UnidadMedida
  activo: boolean
  bajoMinimo: boolean
}

export interface ProductoEntrada {
  nombre: string
  codigoBarras: string | null
  categoriaId: number | null
  precioVenta: number
  precioCoste: number | null
  ivaPorcentaje: number
  stockMinimo: number | null
  stockInicial: number | null
  unidadMedida: UnidadMedida
  activo: boolean
}

export interface Proveedor {
  id: number
  nombre: string
  telefono: string | null
  contacto: string | null
}

export interface LineaVenta {
  productoId: number
  productoNombre: string
  cantidad: number
  precioUnitario: number
  subtotal: number
}

export interface Venta {
  id: number
  fechaHora: string
  total: number
  totalIva: number
  metodoPago: MetodoPago
  lineas: LineaVenta[]
}

export interface VentaEntrada {
  metodoPago: MetodoPago
  lineas: { productoId: number; cantidad: number }[]
}

export interface MovimientoStock {
  id: number
  productoId: number
  productoNombre: string
  tipo: TipoMovimiento
  cantidad: number
  motivo: string | null
  fechaHora: string
}

export interface InformeVentas {
  desde: string
  hasta: string
  numeroVentas: number
  totalVentas: number
  totalIva: number
  totalEfectivo: number
  totalTarjeta: number
}

export interface ProductoVendido {
  productoId: number
  nombre: string
  cantidadVendida: number
  importeTotal: number
}

export interface ValorInventario {
  productosActivos: number
  valorCoste: number
  valorVenta: number
}

export interface ErrorApi {
  codigo: string
  mensaje: string
  detalles?: Record<string, string>
}
