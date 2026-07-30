import { useCallback, useEffect, useState } from 'react'
import {
  informeVentas,
  listarVentas,
  mensajeDeError,
  productosBajoMinimo,
  productosMasVendidos,
  valorInventario,
} from '../api'
import Ticket from '../componentes/Ticket'
import type { InformeVentas, Producto, ProductoVendido, ValorInventario, Venta } from '../tipos'

const euros = (n: number) => n.toFixed(2).replace('.', ',') + ' €'
const hoy = () => new Date().toISOString().slice(0, 10)

export default function Informes() {
  const [desde, setDesde] = useState(hoy())
  const [hasta, setHasta] = useState(hoy())
  const [resumen, setResumen] = useState<InformeVentas | null>(null)
  const [masVendidos, setMasVendidos] = useState<ProductoVendido[]>([])
  const [inventario, setInventario] = useState<ValorInventario | null>(null)
  const [bajoMinimo, setBajoMinimo] = useState<Producto[]>([])
  const [ventas, setVentas] = useState<Venta[]>([])
  const [ticket, setTicket] = useState<Venta | null>(null)
  const [error, setError] = useState('')

  const cargar = useCallback(() => {
    setError('')
    Promise.all([
      informeVentas(desde, hasta),
      productosMasVendidos(desde, hasta),
      valorInventario(),
      productosBajoMinimo(),
      listarVentas(desde, hasta),
    ])
      .then(([r, top, inv, bajo, listaVentas]) => {
        setResumen(r)
        setMasVendidos(top)
        setInventario(inv)
        setBajoMinimo(bajo)
        setVentas(listaVentas)
      })
      .catch((e) => setError(mensajeDeError(e)))
  }, [desde, hasta])

  useEffect(cargar, [cargar])

  const tarjeta = 'rounded-lg bg-white p-4 shadow'

  return (
    <div className="grid gap-4">
      <div className="flex flex-wrap items-end gap-3">
        <label>
          <span className="block text-sm text-slate-600">Desde</span>
          <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} className="rounded border p-2" />
        </label>
        <label>
          <span className="block text-sm text-slate-600">Hasta</span>
          <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} className="rounded border p-2" />
        </label>
        <button
          onClick={() => {
            setDesde(hoy())
            setHasta(hoy())
          }}
          className="rounded bg-slate-200 px-3 py-2 text-sm font-semibold hover:bg-slate-300"
        >
          Hoy
        </button>
        <span className="ml-auto flex gap-2">
          <a
            href={`/api/informes/ventas.csv?desde=${desde}&hasta=${hasta}`}
            className="rounded bg-slate-700 px-3 py-2 text-sm font-semibold text-white hover:bg-slate-600"
          >
            ⬇ Ventas (CSV)
          </a>
          <a
            href="/api/informes/inventario.csv"
            className="rounded bg-slate-700 px-3 py-2 text-sm font-semibold text-white hover:bg-slate-600"
          >
            ⬇ Inventario (CSV)
          </a>
        </span>
      </div>

      {error && <p className="rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}

      {resumen && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          <div className={tarjeta}>
            <p className="text-sm text-slate-500">Nº de ventas</p>
            <p className="text-2xl font-bold">{resumen.numeroVentas}</p>
          </div>
          <div className={tarjeta}>
            <p className="text-sm text-slate-500">Total vendido</p>
            <p className="text-2xl font-bold">{euros(resumen.totalVentas)}</p>
          </div>
          <div className={tarjeta}>
            <p className="text-sm text-slate-500">IVA incluido</p>
            <p className="text-2xl font-bold">{euros(resumen.totalIva)}</p>
          </div>
          <div className={tarjeta}>
            <p className="text-sm text-slate-500">Efectivo</p>
            <p className="text-2xl font-bold">{euros(resumen.totalEfectivo)}</p>
          </div>
          <div className={tarjeta}>
            <p className="text-sm text-slate-500">Tarjeta</p>
            <p className="text-2xl font-bold">{euros(resumen.totalTarjeta)}</p>
          </div>
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-2">
        <div className={tarjeta}>
          <h2 className="mb-2 font-bold">Productos más vendidos</h2>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left text-slate-500">
                <th className="py-1">Producto</th>
                <th className="py-1 text-right">Cantidad</th>
                <th className="py-1 text-right">Importe</th>
              </tr>
            </thead>
            <tbody>
              {masVendidos.map((p) => (
                <tr key={p.productoId} className="border-b last:border-0">
                  <td className="py-1.5">{p.nombre}</td>
                  <td className="py-1.5 text-right">{p.cantidadVendida}</td>
                  <td className="py-1.5 text-right">{euros(p.importeTotal)}</td>
                </tr>
              ))}
              {masVendidos.length === 0 && (
                <tr>
                  <td colSpan={3} className="py-4 text-center text-slate-400">
                    Sin ventas en el rango.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="grid gap-4">
          {inventario && (
            <div className={tarjeta}>
              <h2 className="mb-2 font-bold">Valor del inventario</h2>
              <p className="flex justify-between">
                <span>Productos activos</span>
                <span className="font-semibold">{inventario.productosActivos}</span>
              </p>
              <p className="flex justify-between">
                <span>A precio de coste</span>
                <span className="font-semibold">{euros(inventario.valorCoste)}</span>
              </p>
              <p className="flex justify-between">
                <span>A precio de venta</span>
                <span className="font-semibold">{euros(inventario.valorVenta)}</span>
              </p>
            </div>
          )}
          <div className={tarjeta}>
            <h2 className="mb-2 font-bold">Stock bajo mínimo ({bajoMinimo.length})</h2>
            <ul className="max-h-48 divide-y overflow-auto text-sm">
              {bajoMinimo.map((p) => (
                <li key={p.id} className="flex justify-between py-1.5">
                  <span>{p.nombre}</span>
                  <span className="font-semibold text-red-700">
                    {p.stockActual} / mín. {p.stockMinimo}
                  </span>
                </li>
              ))}
              {bajoMinimo.length === 0 && <li className="py-2 text-slate-400">Todo el stock está por encima del mínimo.</li>}
            </ul>
          </div>
        </div>
      </div>

      <div className={tarjeta}>
        <h2 className="mb-2 font-bold">Ventas del rango ({ventas.length})</h2>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-slate-500">
              <th className="py-1">Ticket</th>
              <th className="py-1">Fecha y hora</th>
              <th className="py-1">Pago</th>
              <th className="py-1 text-right">IVA</th>
              <th className="py-1 text-right">Total</th>
              <th className="py-1"></th>
            </tr>
          </thead>
          <tbody>
            {ventas.map((v) => (
              <tr key={v.id} className="border-b last:border-0">
                <td className="py-1.5">#{v.id}</td>
                <td className="py-1.5">{new Date(v.fechaHora).toLocaleString('es-ES')}</td>
                <td className="py-1.5">{v.metodoPago === 'EFECTIVO' ? 'Efectivo' : 'Tarjeta'}</td>
                <td className="py-1.5 text-right">{euros(v.totalIva)}</td>
                <td className="py-1.5 text-right font-semibold">{euros(v.total)}</td>
                <td className="py-1.5 text-right">
                  <button onClick={() => setTicket(v)} className="text-blue-600 hover:underline">
                    Ver ticket
                  </button>
                </td>
              </tr>
            ))}
            {ventas.length === 0 && (
              <tr>
                <td colSpan={6} className="py-4 text-center text-slate-400">
                  Sin ventas en el rango.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {ticket && (
        <div className="fixed inset-0 z-20 flex items-center justify-center bg-black/50 print:bg-transparent">
          <div className="max-h-[90vh] overflow-auto rounded-lg bg-white p-4 shadow-xl">
            <Ticket venta={ticket} />
            <div className="mt-4 flex gap-2 print:hidden">
              <button
                onClick={() => window.print()}
                className="flex-1 rounded bg-slate-800 py-2 font-semibold text-white hover:bg-slate-700"
              >
                Imprimir
              </button>
              <button
                onClick={() => setTicket(null)}
                className="flex-1 rounded bg-slate-200 py-2 font-semibold hover:bg-slate-300"
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
