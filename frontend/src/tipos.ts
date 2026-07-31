// Tipos espejo de los DTOs del backend

export type UnidadMedida = 'UNIDAD' | 'KG' | 'LITRO'
export type MetodoPago = 'EFECTIVO' | 'TARJETA'
export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'AJUSTE' | 'MERMA'

export interface Categoria {
  id: number
  nombre: string
}

export interface ComponentePack {
  id: number
  productoId: number
  productoNombre: string
  cantidad: number
}

export interface ComponentePackEntrada {
  productoId: number
  cantidad: number
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
  esPack: boolean
  componentes: ComponentePack[]
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
  esPack: boolean
  componentes: ComponentePackEntrada[]
}

export interface Proveedor {
  id: number
  nombre: string
  telefono: string | null
  contacto: string | null
}

export interface LineaVenta {
  id: number
  productoId: number
  productoNombre: string
  cantidad: number
  precioUnitario: number
  descuentoPorcentaje: number
  subtotal: number
  cantidadDevuelta: number
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
  lineas: { productoId: number; cantidad: number; descuentoPorcentaje?: number }[]
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

export type RolUsuario = 'ADMIN' | 'CAJERO'

export interface Usuario {
  nombreUsuario: string
  nombre: string
  rol: RolUsuario
}

export interface UsuarioAdmin {
  id: number
  nombreUsuario: string
  nombre: string
  rol: RolUsuario
  activo: boolean
}

export interface UsuarioEntrada {
  nombreUsuario: string
  password: string
  nombre: string
  rol: RolUsuario
}

export interface UsuarioActualizar {
  nombre: string
  rol: RolUsuario
  activo: boolean
}

export interface Denominacion {
  valor: number
  cantidad: number
}

export interface CierreCaja {
  id: number
  fecha: string
  numeroVentas: number
  totalVentas: number
  totalEfectivo: number
  totalTarjeta: number
  efectivoContado: number
  diferencia: number
  notas: string | null
  fechaHora: string
  denominaciones: Denominacion[]
}

export interface LineaDevolucion {
  id: number
  lineaVentaId: number
  productoNombre: string
  cantidad: number
  importe: number
}

export interface Devolucion {
  id: number
  ventaId: number
  fechaHora: string
  total: number
  totalIva: number
  motivo: string | null
  lineas: LineaDevolucion[]
}

export interface DevolucionEntrada {
  ventaId: number
  motivo: string | null
  lineas: { lineaVentaId: number; cantidad: number }[]
}

export type EstadoPedido = 'PENDIENTE' | 'RECIBIDO_PARCIAL' | 'RECIBIDO_COMPLETO' | 'CANCELADO'

export interface LineaPedidoProveedor {
  id: number
  productoId: number
  productoNombre: string
  cantidadPedida: number
  cantidadRecibida: number
  cantidadPendiente: number
  precioCosteUnitario: number | null
}

export interface PedidoProveedor {
  id: number
  proveedorId: number
  proveedorNombre: string
  fechaHora: string
  estado: EstadoPedido
  notas: string | null
  lineas: LineaPedidoProveedor[]
}

export interface PedidoProveedorEntrada {
  proveedorId: number
  notas: string | null
  lineas: { productoId: number; cantidad: number; precioCosteUnitario: number | null }[]
}

export interface RecepcionEntrada {
  lineas: { lineaPedidoId: number; cantidadRecibida: number }[]
}

export interface ErrorApi {
  codigo: string
  mensaje: string
  detalles?: Record<string, string>
}
