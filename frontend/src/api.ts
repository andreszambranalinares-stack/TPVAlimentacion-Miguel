import axios, { AxiosError } from 'axios'
import type {
  Categoria,
  ErrorApi,
  InformeVentas,
  MovimientoStock,
  Producto,
  ProductoEntrada,
  ProductoVendido,
  Proveedor,
  TipoMovimiento,
  ValorInventario,
  Venta,
  VentaEntrada,
} from './tipos'

const api = axios.create({ baseURL: '/api' })

/** Extrae el mensaje de error con el formato {codigo, mensaje, detalles} de la API. */
export function mensajeDeError(error: unknown): string {
  const axiosError = error as AxiosError<ErrorApi>
  const datos = axiosError.response?.data
  if (datos?.detalles && Object.keys(datos.detalles).length > 0) {
    return Object.values(datos.detalles).join('. ')
  }
  return datos?.mensaje ?? 'Error de conexión con el servidor'
}

// Productos
export const buscarProductos = (texto?: string, categoriaId?: number, incluirInactivos = false) =>
  api
    .get<Producto[]>('/productos', { params: { texto: texto || undefined, categoriaId, incluirInactivos } })
    .then((r) => r.data)

export const productoPorCodigoBarras = (codigo: string) =>
  api.get<Producto>(`/productos/codigo-barras/${encodeURIComponent(codigo)}`).then((r) => r.data)

export const productosBajoMinimo = () => api.get<Producto[]>('/productos/bajo-minimo').then((r) => r.data)

export const crearProducto = (datos: ProductoEntrada) => api.post<Producto>('/productos', datos).then((r) => r.data)

export const actualizarProducto = (id: number, datos: ProductoEntrada) =>
  api.put<Producto>(`/productos/${id}`, datos).then((r) => r.data)

export const desactivarProducto = (id: number) => api.delete(`/productos/${id}`)

// Categorías
export const listarCategorias = () => api.get<Categoria[]>('/categorias').then((r) => r.data)

export const crearCategoria = (nombre: string) => api.post<Categoria>('/categorias', { nombre }).then((r) => r.data)

// Proveedores
export const listarProveedores = () => api.get<Proveedor[]>('/proveedores').then((r) => r.data)

export const crearProveedor = (datos: Omit<Proveedor, 'id'>) =>
  api.post<Proveedor>('/proveedores', datos).then((r) => r.data)

export const eliminarProveedor = (id: number) => api.delete(`/proveedores/${id}`)

// Ventas
export const crearVenta = (datos: VentaEntrada) => api.post<Venta>('/ventas', datos).then((r) => r.data)

export const listarVentas = (desde?: string, hasta?: string) =>
  api.get<Venta[]>('/ventas', { params: { desde, hasta } }).then((r) => r.data)

// Movimientos de stock
export const registrarMovimiento = (datos: {
  productoId: number
  tipo: TipoMovimiento
  cantidad: number
  motivo: string
}) => api.post<MovimientoStock>('/movimientos-stock', datos).then((r) => r.data)

export const movimientosDeProducto = (productoId: number) =>
  api.get<MovimientoStock[]>('/movimientos-stock', { params: { productoId } }).then((r) => r.data)

// Informes
export const informeVentas = (desde?: string, hasta?: string) =>
  api.get<InformeVentas>('/informes/ventas', { params: { desde, hasta } }).then((r) => r.data)

export const productosMasVendidos = (desde?: string, hasta?: string, limite = 10) =>
  api
    .get<ProductoVendido[]>('/informes/productos-mas-vendidos', { params: { desde, hasta, limite } })
    .then((r) => r.data)

export const valorInventario = () => api.get<ValorInventario>('/informes/valor-inventario').then((r) => r.data)
