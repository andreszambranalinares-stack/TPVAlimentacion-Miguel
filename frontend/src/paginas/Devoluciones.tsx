import { useEffect, useState } from 'react'
import { crearDevolucion, esErrorDeSesionCaducada, listarDevoluciones, mensajeDeError, obtenerVenta } from '../api'
import { euros, seleccionarAlFoco } from '../utils'
import type { Devolucion, Venta } from '../tipos'

/** Devolución total o parcial de una venta ya cobrada: repone el stock automáticamente. */
export default function Devoluciones() {
  const [numeroTicket, setNumeroTicket] = useState('')
  const [venta, setVenta] = useState<Venta | null>(null)
  const [cantidades, setCantidades] = useState<Record<number, string>>({})
  const [motivo, setMotivo] = useState('')
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [buscando, setBuscando] = useState(false)
  const [devoluciones, setDevoluciones] = useState<Devolucion[]>([])
  const [cargandoLista, setCargandoLista] = useState(true)

  const cargarDevoluciones = () => {
    setCargandoLista(true)
    listarDevoluciones()
      .then(setDevoluciones)
      .catch((e) => {
        if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
      })
      .finally(() => setCargandoLista(false))
  }

  useEffect(cargarDevoluciones, [])

  const buscar = async (evento: React.FormEvent) => {
    evento.preventDefault()
    if (!numeroTicket.trim()) return
    setError('')
    setMensaje('')
    setVenta(null)
    setBuscando(true)
    try {
      const v = await obtenerVenta(Number(numeroTicket))
      setVenta(v)
      setCantidades({})
      setMotivo('')
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    } finally {
      setBuscando(false)
    }
  }

  const registrar = async (evento: React.FormEvent) => {
    evento.preventDefault()
    if (!venta) return
    const lineas = venta.lineas
      .map((l) => ({ lineaVentaId: l.id, cantidad: Number(cantidades[l.id] || 0) }))
      .filter((l) => l.cantidad > 0)
    if (lineas.length === 0) {
      setError('Indica la cantidad a devolver de al menos una línea')
      return
    }
    setError('')
    setMensaje('')
    try {
      await crearDevolucion({ ventaId: venta.id, motivo: motivo || null, lineas })
      setMensaje(`Devolución registrada. Stock repuesto.`)
      setVenta(null)
      setNumeroTicket('')
      cargarDevoluciones()
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <div>
        <form onSubmit={buscar} className="mb-4 flex items-end gap-2 rounded-lg bg-white p-4 shadow">
          <label className="flex-1">
            <span className="text-sm text-slate-600">Nº de ticket a devolver</span>
            <input
              required
              type="number"
              min="1"
              autoFocus
              value={numeroTicket}
              onChange={(e) => setNumeroTicket(e.target.value)}
              onFocus={seleccionarAlFoco}
              placeholder="p. ej. 128"
              className="w-full rounded border p-2"
            />
          </label>
          <button
            disabled={buscando}
            className="rounded bg-slate-700 px-4 py-2 font-semibold text-white hover:bg-slate-600 disabled:opacity-40"
          >
            Buscar
          </button>
        </form>

        {error && <p className="mb-3 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}
        {mensaje && <p className="mb-3 rounded bg-green-100 px-3 py-2 text-green-700">{mensaje}</p>}

        {venta && (
          <form onSubmit={registrar} className="rounded-lg bg-white p-4 shadow">
            <h2 className="mb-1 text-lg font-bold">Ticket #{venta.id}</h2>
            <p className="mb-3 text-sm text-slate-500">{new Date(venta.fechaHora).toLocaleString('es-ES')}</p>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-slate-500">
                  <th className="py-1">Producto</th>
                  <th className="py-1 text-right">Vendido</th>
                  <th className="py-1 text-right">Ya devuelto</th>
                  <th className="py-1 text-right">A devolver</th>
                </tr>
              </thead>
              <tbody>
                {venta.lineas.map((l) => {
                  const disponible = l.cantidad - l.cantidadDevuelta
                  return (
                    <tr key={l.id} className="border-b last:border-0">
                      <td className="py-1.5">{l.productoNombre}</td>
                      <td className="py-1.5 text-right">{l.cantidad}</td>
                      <td className="py-1.5 text-right text-slate-500">{l.cantidadDevuelta}</td>
                      <td className="py-1.5 text-right">
                        <input
                          type="number"
                          min="0"
                          max={disponible}
                          step="any"
                          disabled={disponible <= 0}
                          value={cantidades[l.id] ?? ''}
                          onChange={(e) => setCantidades((actual) => ({ ...actual, [l.id]: e.target.value }))}
                          onFocus={seleccionarAlFoco}
                          aria-label={`Cantidad a devolver de ${l.productoNombre}`}
                          className="w-20 rounded border p-1 text-center disabled:bg-slate-100"
                        />
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
            <label className="mt-3 block">
              <span className="text-sm text-slate-600">Motivo (opcional)</span>
              <input
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
                placeholder="p. ej. producto en mal estado"
                className="w-full rounded border p-2"
              />
            </label>
            <button className="mt-3 w-full rounded bg-amber-500 py-2 font-semibold text-white hover:bg-amber-600">
              Registrar devolución
            </button>
          </form>
        )}
      </div>

      <div className="rounded-lg bg-white p-4 shadow">
        <h2 className="mb-2 font-bold">Devoluciones de hoy</h2>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-slate-500">
              <th className="py-1">Ticket</th>
              <th className="py-1">Hora</th>
              <th className="py-1">Motivo</th>
              <th className="py-1">Empleado</th>
              <th className="py-1 text-right">Importe</th>
            </tr>
          </thead>
          <tbody>
            {devoluciones.map((d) => (
              <tr key={d.id} className="border-b last:border-0">
                <td className="py-1.5">#{d.ventaId}</td>
                <td className="py-1.5">{new Date(d.fechaHora).toLocaleTimeString('es-ES')}</td>
                <td className="py-1.5">{d.motivo ?? '—'}</td>
                <td className="py-1.5">{d.usuarioNombre ?? '—'}</td>
                <td className="py-1.5 text-right font-semibold text-red-700">−{euros(d.total)}</td>
              </tr>
            ))}
            {cargandoLista && (
              <tr>
                <td colSpan={5} className="py-4 text-center text-slate-400">
                  Cargando…
                </td>
              </tr>
            )}
            {!cargandoLista && devoluciones.length === 0 && (
              <tr>
                <td colSpan={5} className="py-4 text-center text-slate-400">
                  Sin devoluciones hoy.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
