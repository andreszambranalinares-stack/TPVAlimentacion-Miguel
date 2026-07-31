import { useCallback, useEffect, useState } from 'react'
import { cerrarCaja, esErrorDeSesionCaducada, informeVentas, listarCierres, mensajeDeError } from '../api'
import { euros, hoy, seleccionarAlFoco } from '../utils'
import type { CierreCaja, InformeVentas } from '../tipos'

/** Billetes y monedas de euro, de mayor a menor valor. */
const DENOMINACIONES = [500, 200, 100, 50, 20, 10, 5, 2, 1, 0.5, 0.2, 0.1, 0.05, 0.02, 0.01]

/** Arqueo diario: comparar el efectivo contado con lo que dicen las ventas. */
export default function Cierre() {
  const [fecha, setFecha] = useState(hoy())
  const [resumen, setResumen] = useState<InformeVentas | null>(null)
  const [cierres, setCierres] = useState<CierreCaja[]>([])
  const [efectivoContado, setEfectivoContado] = useState('')
  const [notas, setNotas] = useState('')
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [cargando, setCargando] = useState(true)
  const [contarPorDenominacion, setContarPorDenominacion] = useState(false)
  const [cantidades, setCantidades] = useState<Record<number, string>>({})

  const cierreExistente = cierres.find((c) => c.fecha === fecha)

  const cargar = useCallback(() => {
    setError('')
    setCargando(true)
    Promise.all([informeVentas(fecha, fecha), listarCierres()])
      .then(([r, lista]) => {
        setResumen(r)
        setCierres(lista)
      })
      .catch((e) => {
        if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
      })
      .finally(() => setCargando(false))
  }, [fecha])

  useEffect(cargar, [cargar])

  const totalDenominaciones = DENOMINACIONES.reduce((suma, valor) => suma + valor * Number(cantidades[valor] || 0), 0)

  const cambiarCantidadDenominacion = (valor: number, texto: string) => {
    setCantidades((actual) => {
      const nuevas = { ...actual, [valor]: texto }
      const total = DENOMINACIONES.reduce((suma, v) => suma + v * Number(nuevas[v] || 0), 0)
      setEfectivoContado(total.toFixed(2))
      return nuevas
    })
  }

  const diferencia =
    resumen && efectivoContado !== '' ? Number(efectivoContado) - resumen.totalEfectivo : null

  const cerrar = async (evento: React.FormEvent) => {
    evento.preventDefault()
    setError('')
    setMensaje('')
    try {
      const denominaciones = contarPorDenominacion
        ? DENOMINACIONES.filter((v) => Number(cantidades[v] || 0) > 0).map((valor) => ({
            valor,
            cantidad: Number(cantidades[valor]),
          }))
        : undefined
      await cerrarCaja({ fecha, efectivoContado: Number(efectivoContado), notas, denominaciones })
      setMensaje('Caja cerrada correctamente')
      setEfectivoContado('')
      setNotas('')
      setCantidades({})
      cargar()
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <div className="rounded-lg bg-white p-4 shadow lg:self-start">
        <h2 className="mb-3 text-lg font-bold">Cierre de caja</h2>
        <label className="mb-3 block">
          <span className="text-sm text-slate-600">Fecha</span>
          <input
            type="date"
            value={fecha}
            max={hoy()}
            onChange={(e) => setFecha(e.target.value)}
            className="w-full rounded border p-2"
          />
        </label>

        {resumen && (
          <div className="mb-4 rounded bg-slate-50 p-3 text-sm">
            <p className="flex justify-between">
              <span>Nº de ventas</span>
              <span className="font-semibold">{resumen.numeroVentas}</span>
            </p>
            <p className="flex justify-between">
              <span>Total vendido</span>
              <span className="font-semibold">{euros(resumen.totalVentas)}</span>
            </p>
            <p className="flex justify-between">
              <span>Cobrado con tarjeta</span>
              <span className="font-semibold">{euros(resumen.totalTarjeta)}</span>
            </p>
            <p className="flex justify-between text-base">
              <span className="font-semibold">Efectivo esperado en cajón</span>
              <span className="font-bold">{euros(resumen.totalEfectivo)}</span>
            </p>
          </div>
        )}

        {cierreExistente ? (
          <div className="rounded bg-amber-50 p-3 text-sm">
            <p className="font-semibold">Esta caja ya está cerrada.</p>
            <p className="mt-1 flex justify-between">
              <span>Efectivo contado</span>
              <span>{euros(cierreExistente.efectivoContado)}</span>
            </p>
            <p className="flex justify-between">
              <span>Diferencia</span>
              <span className={cierreExistente.diferencia < 0 ? 'font-bold text-red-700' : 'font-bold text-green-700'}>
                {euros(cierreExistente.diferencia)}
              </span>
            </p>
            {cierreExistente.notas && <p className="mt-1 text-slate-500">{cierreExistente.notas}</p>}
            {cierreExistente.denominaciones.length > 0 && (
              <div className="mt-2 border-t pt-2 text-xs text-slate-500">
                <p className="mb-1 font-semibold">Desglose contado</p>
                {cierreExistente.denominaciones.map((d) => (
                  <p key={d.valor} className="flex justify-between">
                    <span>{euros(d.valor)}</span>
                    <span>× {d.cantidad}</span>
                  </p>
                ))}
              </div>
            )}
          </div>
        ) : (
          <form onSubmit={cerrar} className="grid gap-3">
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={contarPorDenominacion}
                onChange={(e) => {
                  setContarPorDenominacion(e.target.checked)
                  if (!e.target.checked) {
                    setCantidades({})
                  } else {
                    setEfectivoContado(totalDenominaciones.toFixed(2))
                  }
                }}
              />
              Contar por billetes y monedas
            </label>

            {contarPorDenominacion && (
              <div className="grid grid-cols-3 gap-2 rounded border bg-slate-50 p-3 sm:grid-cols-4">
                {DENOMINACIONES.map((valor) => (
                  <label key={valor} className="text-xs">
                    <span className="block text-slate-600">{euros(valor)}</span>
                    <input
                      type="number"
                      min="0"
                      step="1"
                      value={cantidades[valor] ?? ''}
                      onChange={(e) => cambiarCantidadDenominacion(valor, e.target.value)}
                      onFocus={seleccionarAlFoco}
                      aria-label={`Cantidad de billetes/monedas de ${euros(valor)}`}
                      className="w-full rounded border p-1 text-center"
                    />
                  </label>
                ))}
              </div>
            )}

            <label>
              <span className="text-sm text-slate-600">Efectivo contado en el cajón *</span>
              <input
                required
                type="number"
                min="0"
                step="0.01"
                readOnly={contarPorDenominacion}
                value={efectivoContado}
                onChange={(e) => setEfectivoContado(e.target.value)}
                onFocus={seleccionarAlFoco}
                className={`w-full rounded border p-2 ${contarPorDenominacion ? 'bg-slate-100' : ''}`}
              />
            </label>
            {diferencia !== null && (
              <p
                className={`rounded px-3 py-2 text-sm font-semibold ${
                  Math.abs(diferencia) < 0.005
                    ? 'bg-green-100 text-green-700'
                    : diferencia < 0
                      ? 'bg-red-100 text-red-700'
                      : 'bg-amber-100 text-amber-700'
                }`}
              >
                {Math.abs(diferencia) < 0.005
                  ? 'El arqueo cuadra'
                  : diferencia < 0
                    ? `Faltan ${euros(Math.abs(diferencia))}`
                    : `Sobran ${euros(diferencia)}`}
              </p>
            )}
            <label>
              <span className="text-sm text-slate-600">Notas</span>
              <input
                value={notas}
                onChange={(e) => setNotas(e.target.value)}
                placeholder="p. ej. faltan monedas de cambio"
                className="w-full rounded border p-2"
              />
            </label>
            {error && <p className="rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}
            {mensaje && <p className="rounded bg-green-100 px-3 py-2 text-green-700">{mensaje}</p>}
            <button className="rounded bg-amber-500 py-2 font-semibold text-white hover:bg-amber-600">
              Cerrar caja del {fecha}
            </button>
          </form>
        )}
      </div>

      <div className="rounded-lg bg-white p-4 shadow">
        <h2 className="mb-2 font-bold">Cierres anteriores</h2>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-slate-500">
              <th className="py-1">Fecha</th>
              <th className="py-1 text-right">Ventas</th>
              <th className="py-1 text-right">Total</th>
              <th className="py-1 text-right">Efectivo</th>
              <th className="py-1 text-right">Contado</th>
              <th className="py-1 text-right">Diferencia</th>
              <th className="py-1">Empleado</th>
            </tr>
          </thead>
          <tbody>
            {cierres.map((c) => (
              <tr key={c.id} className="border-b last:border-0">
                <td className="py-1.5">{new Date(c.fecha).toLocaleDateString('es-ES')}</td>
                <td className="py-1.5 text-right">{c.numeroVentas}</td>
                <td className="py-1.5 text-right">{euros(c.totalVentas)}</td>
                <td className="py-1.5 text-right">{euros(c.totalEfectivo)}</td>
                <td className="py-1.5 text-right">{euros(c.efectivoContado)}</td>
                <td
                  className={`py-1.5 text-right font-semibold ${
                    c.diferencia < 0 ? 'text-red-700' : c.diferencia > 0 ? 'text-amber-600' : 'text-green-700'
                  }`}
                >
                  {euros(c.diferencia)}
                </td>
                <td className="py-1.5">{c.usuarioNombre ?? '—'}</td>
              </tr>
            ))}
            {cargando && (
              <tr>
                <td colSpan={7} className="py-4 text-center text-slate-400">
                  Cargando…
                </td>
              </tr>
            )}
            {!cargando && cierres.length === 0 && (
              <tr>
                <td colSpan={7} className="py-4 text-center text-slate-400">
                  Todavía no hay cierres registrados.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
