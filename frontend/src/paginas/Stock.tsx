import { useCallback, useEffect, useState } from 'react'
import {
  buscarProductos,
  esErrorDeSesionCaducada,
  listarProveedores,
  mensajeDeError,
  movimientosDeProducto,
  registrarMovimiento,
} from '../api'
import { useEsAdmin } from '../AuthContexto'
import { seleccionarAlFoco } from '../utils'
import type { MovimientoStock, Producto, Proveedor, TipoMovimiento } from '../tipos'

const nombresTipo: Record<TipoMovimiento, string> = {
  ENTRADA: 'Entrada (compra)',
  SALIDA: 'Salida',
  AJUSTE: 'Ajuste de recuento',
  MERMA: 'Merma (caducado/roto)',
}

export default function Stock() {
  const esAdmin = useEsAdmin()
  const [productos, setProductos] = useState<Producto[]>([])
  const [proveedores, setProveedores] = useState<Proveedor[]>([])
  const [texto, setTexto] = useState('')
  const [soloBajoMinimo, setSoloBajoMinimo] = useState(false)
  const [seleccionado, setSeleccionado] = useState<Producto | null>(null)
  const [movimientos, setMovimientos] = useState<MovimientoStock[]>([])
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [cargando, setCargando] = useState(true)

  const [tipo, setTipo] = useState<TipoMovimiento>('ENTRADA')
  const [cantidad, setCantidad] = useState('')
  const [motivo, setMotivo] = useState('')
  const [proveedorId, setProveedorId] = useState<number | ''>('')

  const cargarProductos = useCallback(() => {
    setCargando(true)
    buscarProductos(texto)
      .then((lista) => setProductos(soloBajoMinimo ? lista.filter((p) => p.bajoMinimo) : lista))
      .catch((e) => {
        if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
      })
      .finally(() => setCargando(false))
  }, [texto, soloBajoMinimo])

  useEffect(() => {
    const temporizador = setTimeout(cargarProductos, 200)
    return () => clearTimeout(temporizador)
  }, [cargarProductos])

  useEffect(() => {
    listarProveedores()
      .then(setProveedores)
      .catch((e) => {
        if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
      })
  }, [])

  const seleccionar = (p: Producto) => {
    setSeleccionado(p)
    setMensaje('')
    setError('')
    movimientosDeProducto(p.id).then(setMovimientos).catch(() => setMovimientos([]))
  }

  const registrar = async (evento: React.FormEvent) => {
    evento.preventDefault()
    if (!seleccionado) return
    if (tipo === 'AJUSTE') {
      const confirmado = confirm(
        `¿Confirmas el ajuste manual de ${cantidad} en "${seleccionado.nombre}"? Esta operación corrige el stock directamente.`,
      )
      if (!confirmado) return
    }
    setError('')
    setMensaje('')
    const proveedor = proveedores.find((prov) => prov.id === proveedorId)
    const motivoCompleto =
      tipo === 'ENTRADA' && proveedor
        ? `Compra a ${proveedor.nombre}${motivo ? ` — ${motivo}` : ''}`
        : motivo
    try {
      await registrarMovimiento({
        productoId: seleccionado.id,
        tipo,
        cantidad: Number(cantidad),
        motivo: motivoCompleto,
      })
      setMensaje('Movimiento registrado')
      setCantidad('')
      setMotivo('')
      setProveedorId('')
      cargarProductos()
      const actualizados = await buscarProductos(seleccionado.nombre)
      const actualizado = actualizados.find((p) => p.id === seleccionado.id)
      if (actualizado) setSeleccionado(actualizado)
      movimientosDeProducto(seleccionado.id).then(setMovimientos)
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <div>
        <div className="mb-3 flex items-center gap-3">
          <input
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Buscar producto…"
            className="w-64 rounded-lg border p-2"
          />
          <label className="flex items-center gap-1 text-sm">
            <input type="checkbox" checked={soloBajoMinimo} onChange={(e) => setSoloBajoMinimo(e.target.checked)} />
            Solo bajo mínimo
          </label>
        </div>
        <table className="w-full rounded-lg bg-white shadow">
          <thead>
            <tr className="border-b text-left text-sm text-slate-500">
              <th className="p-3">Producto</th>
              <th className="p-3 text-right">Stock</th>
              <th className="p-3 text-right">Mínimo</th>
            </tr>
          </thead>
          <tbody>
            {productos.map((p) => (
              <tr
                key={p.id}
                onClick={() => seleccionar(p)}
                className={`cursor-pointer border-b last:border-0 hover:bg-amber-50 ${
                  seleccionado?.id === p.id ? 'bg-amber-100' : ''
                }`}
              >
                <td className="p-3">
                  {p.nombre}
                  {p.bajoMinimo && (
                    <span className="ml-2 rounded bg-red-100 px-1.5 text-xs font-bold text-red-700">¡reponer!</span>
                  )}
                </td>
                <td className={`p-3 text-right font-semibold ${p.bajoMinimo ? 'text-red-700' : ''}`}>{p.stockActual}</td>
                <td className="p-3 text-right text-slate-500">{p.stockMinimo}</td>
              </tr>
            ))}
            {cargando && (
              <tr>
                <td colSpan={3} className="p-6 text-center text-slate-400">
                  Cargando…
                </td>
              </tr>
            )}
            {!cargando && productos.length === 0 && (
              <tr>
                <td colSpan={3} className="p-6 text-center text-slate-400">
                  Sin productos.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div>
        {!seleccionado && (
          <p className="rounded-lg bg-white p-6 text-center text-slate-400 shadow">
            Selecciona un producto para registrar movimientos y ver su historial.
          </p>
        )}
        {seleccionado && (
          <div className="rounded-lg bg-white p-4 shadow">
            <h2 className="text-lg font-bold">
              {seleccionado.nombre}
              <span className="ml-2 text-sm font-normal text-slate-500">stock actual: {seleccionado.stockActual}</span>
            </h2>
            <form onSubmit={registrar} className="mt-3 grid gap-3">
              <div className="grid grid-cols-2 gap-3">
                <label>
                  <span className="text-sm text-slate-600">Tipo</span>
                  <select
                    value={tipo}
                    onChange={(e) => setTipo(e.target.value as TipoMovimiento)}
                    className="w-full rounded border p-2"
                  >
                    {(Object.keys(nombresTipo) as TipoMovimiento[])
                      .filter((t) => t !== 'AJUSTE' || esAdmin)
                      .map((t) => (
                        <option key={t} value={t}>
                          {nombresTipo[t]}
                        </option>
                      ))}
                  </select>
                </label>
                <label>
                  <span className="text-sm text-slate-600">
                    Cantidad {tipo === 'AJUSTE' ? '(con signo, p. ej. -2)' : ''}
                  </span>
                  <input
                    required
                    type="number"
                    step="any"
                    value={cantidad}
                    onChange={(e) => setCantidad(e.target.value)}
                    onFocus={seleccionarAlFoco}
                    className="w-full rounded border p-2"
                  />
                </label>
              </div>
              {tipo === 'ENTRADA' && proveedores.length > 0 && (
                <label>
                  <span className="text-sm text-slate-600">Proveedor (opcional)</span>
                  <select
                    value={proveedorId}
                    onChange={(e) => setProveedorId(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full rounded border p-2"
                  >
                    <option value="">Sin proveedor</option>
                    {proveedores.map((prov) => (
                      <option key={prov.id} value={prov.id}>
                        {prov.nombre}
                      </option>
                    ))}
                  </select>
                </label>
              )}
              <label>
                <span className="text-sm text-slate-600">Motivo</span>
                <input
                  value={motivo}
                  onChange={(e) => setMotivo(e.target.value)}
                  placeholder="p. ej. Pedido semanal, caducados…"
                  className="w-full rounded border p-2"
                />
              </label>
              {error && <p className="rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}
              {mensaje && <p className="rounded bg-green-100 px-3 py-2 text-green-700">{mensaje}</p>}
              <button className="rounded bg-amber-500 py-2 font-semibold text-white hover:bg-amber-600">
                Registrar movimiento
              </button>
            </form>

            <h3 className="mt-5 mb-2 font-semibold text-slate-600">Últimos movimientos</h3>
            <ul className="max-h-72 divide-y overflow-auto text-sm">
              {movimientos.map((m) => (
                <li key={m.id} className="flex items-center justify-between py-2">
                  <div>
                    <span
                      className={`mr-2 rounded px-1.5 text-xs font-bold ${
                        m.cantidad >= 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                      }`}
                    >
                      {m.tipo}
                    </span>
                    {m.motivo ?? '—'}
                    <span className="ml-2 text-xs text-slate-400">
                      {new Date(m.fechaHora).toLocaleString('es-ES')}
                      {m.usuarioNombre ? ` · ${m.usuarioNombre}` : ''}
                    </span>
                  </div>
                  <span className={`font-mono font-semibold ${m.cantidad >= 0 ? 'text-green-700' : 'text-red-700'}`}>
                    {m.cantidad > 0 ? `+${m.cantidad}` : m.cantidad}
                  </span>
                </li>
              ))}
              {movimientos.length === 0 && <li className="py-2 text-slate-400">Sin movimientos.</li>}
            </ul>
          </div>
        )}
      </div>
    </div>
  )
}
