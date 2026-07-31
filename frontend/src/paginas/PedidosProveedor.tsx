import { useEffect, useState } from 'react'
import {
  buscarProductos,
  cancelarPedidoProveedor,
  crearPedidoProveedor,
  esErrorDeSesionCaducada,
  listarPedidosProveedor,
  listarProveedores,
  mensajeDeError,
  obtenerPedidoProveedor,
  recibirPedidoProveedor,
} from '../api'
import { useEsAdmin } from '../AuthContexto'
import { euros } from '../utils'
import type { EstadoPedido, PedidoProveedor, Producto, Proveedor } from '../tipos'

const nombresEstado: Record<EstadoPedido, string> = {
  PENDIENTE: 'Pendiente',
  RECIBIDO_PARCIAL: 'Recibido parcial',
  RECIBIDO_COMPLETO: 'Completo',
  CANCELADO: 'Cancelado',
}

const colorEstado: Record<EstadoPedido, string> = {
  PENDIENTE: 'bg-amber-100 text-amber-700',
  RECIBIDO_PARCIAL: 'bg-blue-100 text-blue-700',
  RECIBIDO_COMPLETO: 'bg-green-100 text-green-700',
  CANCELADO: 'bg-slate-200 text-slate-500',
}

interface LineaNueva {
  productoId: number
  productoNombre: string
  cantidad: string
  precioCosteUnitario: string
}

export default function PedidosProveedor() {
  const esAdmin = useEsAdmin()
  const [pedidos, setPedidos] = useState<PedidoProveedor[]>([])
  const [filtroEstado, setFiltroEstado] = useState<EstadoPedido | ''>('')
  const [proveedores, setProveedores] = useState<Proveedor[]>([])
  const [catalogo, setCatalogo] = useState<Producto[]>([])
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [cargando, setCargando] = useState(true)

  const [seleccionado, setSeleccionado] = useState<PedidoProveedor | null>(null)
  const [cantidadesRecibir, setCantidadesRecibir] = useState<Record<number, string>>({})

  const [mostrarAlta, setMostrarAlta] = useState(false)
  const [proveedorId, setProveedorId] = useState<number | ''>('')
  const [notas, setNotas] = useState('')
  const [lineasNuevas, setLineasNuevas] = useState<LineaNueva[]>([])
  const [productoAAgregar, setProductoAAgregar] = useState<number | ''>('')
  const [cantidadAAgregar, setCantidadAAgregar] = useState('1')
  const [costeAAgregar, setCosteAAgregar] = useState('')

  const cargar = () => {
    setCargando(true)
    listarPedidosProveedor(filtroEstado || undefined)
      .then(setPedidos)
      .catch((e) => {
        if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
      })
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [filtroEstado])

  useEffect(() => {
    listarProveedores()
      .then(setProveedores)
      .catch(() => {})
    buscarProductos()
      .then(setCatalogo)
      .catch(() => {})
  }, [])

  const abrirDetalle = async (id: number) => {
    setError('')
    try {
      const p = await obtenerPedidoProveedor(id)
      setSeleccionado(p)
      setCantidadesRecibir({})
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  const recibir = async (evento: React.FormEvent) => {
    evento.preventDefault()
    if (!seleccionado) return
    const lineas = seleccionado.lineas
      .map((l) => ({ lineaPedidoId: l.id, cantidadRecibida: Number(cantidadesRecibir[l.id] || 0) }))
      .filter((l) => l.cantidadRecibida > 0)
    if (lineas.length === 0) {
      setError('Indica la cantidad recibida de al menos una línea')
      return
    }
    setError('')
    setMensaje('')
    try {
      const actualizado = await recibirPedidoProveedor(seleccionado.id, { lineas })
      setSeleccionado(actualizado)
      setCantidadesRecibir({})
      setMensaje('Recepción registrada, stock actualizado')
      cargar()
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  const cancelar = async (p: PedidoProveedor) => {
    if (!confirm(`¿Cancelar el pedido #${p.id} a ${p.proveedorNombre}?`)) return
    setError('')
    try {
      await cancelarPedidoProveedor(p.id)
      setSeleccionado(null)
      cargar()
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  const agregarLinea = () => {
    if (productoAAgregar === '') return
    const cantidad = Number(cantidadAAgregar)
    if (!cantidad || cantidad <= 0) return
    const producto = catalogo.find((p) => p.id === productoAAgregar)
    if (!producto) return
    setLineasNuevas((actual) => [
      ...actual.filter((l) => l.productoId !== productoAAgregar),
      {
        productoId: producto.id,
        productoNombre: producto.nombre,
        cantidad: cantidadAAgregar,
        precioCosteUnitario: costeAAgregar,
      },
    ])
    setProductoAAgregar('')
    setCantidadAAgregar('1')
    setCosteAAgregar('')
  }

  const quitarLinea = (productoId: number) => {
    setLineasNuevas((actual) => actual.filter((l) => l.productoId !== productoId))
  }

  const crear = async (evento: React.FormEvent) => {
    evento.preventDefault()
    if (proveedorId === '' || lineasNuevas.length === 0) return
    setError('')
    try {
      await crearPedidoProveedor({
        proveedorId,
        notas: notas || null,
        lineas: lineasNuevas.map((l) => ({
          productoId: l.productoId,
          cantidad: Number(l.cantidad),
          precioCosteUnitario: l.precioCosteUnitario === '' ? null : Number(l.precioCosteUnitario),
        })),
      })
      setMostrarAlta(false)
      setProveedorId('')
      setNotas('')
      setLineasNuevas([])
      setMensaje('Pedido creado')
      cargar()
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  const campo = 'w-full rounded border p-2'

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <div>
        <div className="mb-3 flex items-center gap-2">
          <select
            value={filtroEstado}
            onChange={(e) => setFiltroEstado(e.target.value as EstadoPedido | '')}
            className="rounded-lg border p-2 text-sm"
          >
            <option value="">Todos los estados</option>
            {(Object.keys(nombresEstado) as EstadoPedido[]).map((e) => (
              <option key={e} value={e}>
                {nombresEstado[e]}
              </option>
            ))}
          </select>
          {esAdmin && (
            <button
              onClick={() => {
                setMostrarAlta(true)
                setError('')
              }}
              className="ml-auto rounded-lg bg-amber-500 px-4 py-2 font-semibold text-white hover:bg-amber-600"
            >
              + Nuevo pedido
            </button>
          )}
        </div>

        {mensaje && <p className="mb-2 rounded bg-green-100 px-3 py-2 text-green-700">{mensaje}</p>}
        {error && !mostrarAlta && !seleccionado && (
          <p className="mb-2 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>
        )}

        <table className="w-full rounded-lg bg-white shadow">
          <thead>
            <tr className="border-b text-left text-sm text-slate-500">
              <th className="p-3">Pedido</th>
              <th className="p-3">Proveedor</th>
              <th className="p-3">Estado</th>
              <th className="p-3"></th>
            </tr>
          </thead>
          <tbody>
            {pedidos.map((p) => (
              <tr key={p.id} className="cursor-pointer border-b last:border-0 hover:bg-amber-50" onClick={() => abrirDetalle(p.id)}>
                <td className="p-3">
                  #{p.id} <span className="text-xs text-slate-400">{new Date(p.fechaHora).toLocaleDateString('es-ES')}</span>
                </td>
                <td className="p-3">{p.proveedorNombre}</td>
                <td className="p-3">
                  <span className={`rounded px-2 py-0.5 text-xs font-semibold ${colorEstado[p.estado]}`}>
                    {nombresEstado[p.estado]}
                  </span>
                </td>
                <td className="p-3 text-right text-blue-600">Ver</td>
              </tr>
            ))}
            {cargando && (
              <tr>
                <td colSpan={4} className="p-6 text-center text-slate-400">
                  Cargando…
                </td>
              </tr>
            )}
            {!cargando && pedidos.length === 0 && (
              <tr>
                <td colSpan={4} className="p-6 text-center text-slate-400">
                  Sin pedidos.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div>
        {!seleccionado && (
          <p className="rounded-lg bg-white p-6 text-center text-slate-400 shadow">
            Selecciona un pedido para ver sus líneas y registrar la recepción de mercancía.
          </p>
        )}
        {seleccionado && (
          <div className="rounded-lg bg-white p-4 shadow">
            <div className="mb-2 flex items-start justify-between">
              <div>
                <h2 className="text-lg font-bold">
                  Pedido #{seleccionado.id} — {seleccionado.proveedorNombre}
                </h2>
                <span className={`mt-1 inline-block rounded px-2 py-0.5 text-xs font-semibold ${colorEstado[seleccionado.estado]}`}>
                  {nombresEstado[seleccionado.estado]}
                </span>
              </div>
              {esAdmin && seleccionado.estado === 'PENDIENTE' && (
                <button onClick={() => cancelar(seleccionado)} className="text-sm text-red-600 hover:underline">
                  Cancelar pedido
                </button>
              )}
            </div>
            {seleccionado.notas && <p className="mb-2 text-sm text-slate-500">{seleccionado.notas}</p>}

            <form onSubmit={recibir}>
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-left text-slate-500">
                    <th className="py-1">Producto</th>
                    <th className="py-1 text-right">Pedido</th>
                    <th className="py-1 text-right">Recibido</th>
                    <th className="py-1 text-right">Pendiente</th>
                    {seleccionado.estado !== 'CANCELADO' && seleccionado.estado !== 'RECIBIDO_COMPLETO' && (
                      <th className="py-1 text-right">Recibir ahora</th>
                    )}
                  </tr>
                </thead>
                <tbody>
                  {seleccionado.lineas.map((l) => (
                    <tr key={l.id} className="border-b last:border-0">
                      <td className="py-1.5">{l.productoNombre}</td>
                      <td className="py-1.5 text-right">{l.cantidadPedida}</td>
                      <td className="py-1.5 text-right text-slate-500">{l.cantidadRecibida}</td>
                      <td className="py-1.5 text-right font-semibold">{l.cantidadPendiente}</td>
                      {seleccionado.estado !== 'CANCELADO' && seleccionado.estado !== 'RECIBIDO_COMPLETO' && (
                        <td className="py-1.5 text-right">
                          <input
                            type="number"
                            min="0"
                            max={l.cantidadPendiente}
                            step="any"
                            disabled={l.cantidadPendiente <= 0}
                            value={cantidadesRecibir[l.id] ?? ''}
                            onChange={(e) => setCantidadesRecibir((actual) => ({ ...actual, [l.id]: e.target.value }))}
                            aria-label={`Cantidad recibida de ${l.productoNombre}`}
                            className="w-20 rounded border p-1 text-center disabled:bg-slate-100"
                          />
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
              {error && <p className="mt-3 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}
              {seleccionado.estado !== 'CANCELADO' && seleccionado.estado !== 'RECIBIDO_COMPLETO' && (
                <button className="mt-3 w-full rounded bg-amber-500 py-2 font-semibold text-white hover:bg-amber-600">
                  Registrar recepción
                </button>
              )}
            </form>
          </div>
        )}
      </div>

      {mostrarAlta && (
        <div className="fixed inset-0 z-20 flex items-center justify-center bg-black/50 p-4">
          <form onSubmit={crear} className="max-h-[90vh] w-full max-w-lg overflow-auto rounded-lg bg-white p-6 shadow-xl">
            <h2 className="mb-4 text-xl font-bold">Nuevo pedido a proveedor</h2>
            {error && <p className="mb-3 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}
            <label className="mb-3 block">
              <span className="text-sm text-slate-600">Proveedor *</span>
              <select
                required
                value={proveedorId}
                onChange={(e) => setProveedorId(e.target.value === '' ? '' : Number(e.target.value))}
                className={campo}
              >
                <option value="">Selecciona…</option>
                {proveedores.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nombre}
                  </option>
                ))}
              </select>
            </label>

            <p className="mb-2 text-sm font-semibold text-slate-700">Líneas del pedido</p>
            <ul className="mb-2 divide-y">
              {lineasNuevas.length === 0 && <li className="py-1.5 text-sm text-slate-500">Añade al menos un producto.</li>}
              {lineasNuevas.map((l) => (
                <li key={l.productoId} className="flex items-center justify-between py-1.5 text-sm">
                  <span>
                    {l.productoNombre} × {l.cantidad}
                    {l.precioCosteUnitario && ` (${euros(Number(l.precioCosteUnitario))}/ud)`}
                  </span>
                  <button type="button" onClick={() => quitarLinea(l.productoId)} className="text-red-600 hover:underline">
                    Quitar
                  </button>
                </li>
              ))}
            </ul>
            <div className="mb-3 flex items-end gap-2">
              <label className="flex-1">
                <span className="text-xs text-slate-600">Producto</span>
                <select
                  value={productoAAgregar}
                  onChange={(e) => setProductoAAgregar(e.target.value === '' ? '' : Number(e.target.value))}
                  className={campo}
                >
                  <option value="">Selecciona…</option>
                  {catalogo.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.nombre}
                    </option>
                  ))}
                </select>
              </label>
              <label className="w-20">
                <span className="text-xs text-slate-600">Cantidad</span>
                <input
                  type="number"
                  min="0"
                  step="any"
                  value={cantidadAAgregar}
                  onChange={(e) => setCantidadAAgregar(e.target.value)}
                  className={campo}
                />
              </label>
              <label className="w-24">
                <span className="text-xs text-slate-600">Coste/ud</span>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={costeAAgregar}
                  onChange={(e) => setCosteAAgregar(e.target.value)}
                  className={campo}
                />
              </label>
              <button
                type="button"
                onClick={agregarLinea}
                className="rounded bg-slate-700 px-3 py-2 font-semibold text-white hover:bg-slate-600"
              >
                Añadir
              </button>
            </div>

            <label className="mb-3 block">
              <span className="text-sm text-slate-600">Notas</span>
              <input value={notas} onChange={(e) => setNotas(e.target.value)} className={campo} />
            </label>

            <div className="flex justify-end gap-2">
              <button type="button" onClick={() => setMostrarAlta(false)} className="rounded px-4 py-2 hover:bg-slate-100">
                Cancelar
              </button>
              <button type="submit" className="rounded bg-amber-500 px-4 py-2 font-semibold text-white hover:bg-amber-600">
                Crear pedido
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
