import { useCallback, useEffect, useRef, useState } from 'react'
import { buscarProductos, crearVenta, mensajeDeError, productoPorCodigoBarras } from '../api'
import Ticket from '../componentes/Ticket'
import type { MetodoPago, Producto, Venta } from '../tipos'

interface LineaCarrito {
  producto: Producto
  cantidad: number
}

const euros = (n: number) => n.toFixed(2).replace('.', ',') + ' €'
const pareceCodigoBarras = (texto: string) => /^\d{6,}$/.test(texto.trim())

/**
 * Pantalla de caja pensada para teclado y lector de códigos de barras:
 * el foco vive en el buscador, Enter añade y F2/F3 cobran.
 */
export default function Caja() {
  const [busqueda, setBusqueda] = useState('')
  const [resultados, setResultados] = useState<Producto[]>([])
  const [indice, setIndice] = useState(0)
  const [carrito, setCarrito] = useState<LineaCarrito[]>([])
  const [error, setError] = useState('')
  const [cobrando, setCobrando] = useState(false)
  const [ticket, setTicket] = useState<Venta | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  const enfocarBuscador = () => inputRef.current?.focus()

  useEffect(() => {
    enfocarBuscador()
  }, [])

  // Búsqueda incremental por nombre (el código de barras va por Enter)
  useEffect(() => {
    const texto = busqueda.trim()
    if (texto.length < 2 || pareceCodigoBarras(texto)) {
      setResultados([])
      setIndice(0)
      return
    }
    const temporizador = setTimeout(() => {
      buscarProductos(texto)
        .then((lista) => {
          setResultados(lista.slice(0, 8))
          setIndice(0)
        })
        .catch(() => setResultados([]))
    }, 150)
    return () => clearTimeout(temporizador)
  }, [busqueda])

  const agregarAlCarrito = useCallback((producto: Producto) => {
    setError('')
    setCarrito((actual) => {
      const existente = actual.find((l) => l.producto.id === producto.id)
      if (existente) {
        return actual.map((l) => (l.producto.id === producto.id ? { ...l, cantidad: l.cantidad + 1 } : l))
      }
      return [...actual, { producto, cantidad: 1 }]
    })
    setBusqueda('')
    setResultados([])
    enfocarBuscador()
  }, [])

  const cambiarCantidad = (productoId: number, cantidad: number) => {
    if (cantidad <= 0) {
      setCarrito((actual) => actual.filter((l) => l.producto.id !== productoId))
    } else {
      setCarrito((actual) => actual.map((l) => (l.producto.id === productoId ? { ...l, cantidad } : l)))
    }
    enfocarBuscador()
  }

  const alPulsarTecla = async (evento: React.KeyboardEvent<HTMLInputElement>) => {
    if (evento.key === 'ArrowDown') {
      evento.preventDefault()
      setIndice((i) => Math.min(i + 1, resultados.length - 1))
    } else if (evento.key === 'ArrowUp') {
      evento.preventDefault()
      setIndice((i) => Math.max(i - 1, 0))
    } else if (evento.key === 'Enter') {
      evento.preventDefault()
      const texto = busqueda.trim()
      if (!texto && resultados.length === 0) return
      if (pareceCodigoBarras(texto)) {
        try {
          agregarAlCarrito(await productoPorCodigoBarras(texto))
        } catch (e) {
          setError(mensajeDeError(e))
          setBusqueda('')
        }
      } else if (resultados[indice]) {
        agregarAlCarrito(resultados[indice])
      }
    } else if (evento.key === 'Escape') {
      setBusqueda('')
      setResultados([])
    }
  }

  const cobrar = useCallback(
    async (metodoPago: MetodoPago) => {
      if (carrito.length === 0 || cobrando) return
      setCobrando(true)
      setError('')
      try {
        const venta = await crearVenta({
          metodoPago,
          lineas: carrito.map((l) => ({ productoId: l.producto.id, cantidad: l.cantidad })),
        })
        setTicket(venta)
        setCarrito([])
      } catch (e) {
        setError(mensajeDeError(e))
      } finally {
        setCobrando(false)
      }
    },
    [carrito, cobrando],
  )

  // Teclas rápidas globales: F2 efectivo, F3 tarjeta, F4 vaciar carrito
  useEffect(() => {
    const manejador = (evento: KeyboardEvent) => {
      if (ticket) {
        if (evento.key === 'Enter' || evento.key === 'Escape') {
          evento.preventDefault()
          setTicket(null)
          setTimeout(enfocarBuscador, 0)
        }
        return
      }
      if (evento.key === 'F2') {
        evento.preventDefault()
        void cobrar('EFECTIVO')
      } else if (evento.key === 'F3') {
        evento.preventDefault()
        void cobrar('TARJETA')
      } else if (evento.key === 'F4') {
        evento.preventDefault()
        setCarrito([])
        enfocarBuscador()
      }
    }
    window.addEventListener('keydown', manejador)
    return () => window.removeEventListener('keydown', manejador)
  }, [cobrar, ticket])

  const total = carrito.reduce((suma, l) => suma + l.producto.precioVenta * l.cantidad, 0)
  const desgloseIva = carrito.reduce<Record<string, number>>((acc, l) => {
    const subtotal = l.producto.precioVenta * l.cantidad
    const iva = (subtotal * l.producto.ivaPorcentaje) / (100 + l.producto.ivaPorcentaje)
    const clave = `${l.producto.ivaPorcentaje}`
    acc[clave] = (acc[clave] ?? 0) + iva
    return acc
  }, {})

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <div className="lg:col-span-2">
        <div className="relative">
          <input
            ref={inputRef}
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            onKeyDown={alPulsarTecla}
            placeholder="Escanea un código de barras o escribe el nombre y pulsa Enter…"
            className="w-full rounded-lg border-2 border-slate-300 p-4 text-lg shadow focus:border-amber-500 focus:outline-none"
            autoComplete="off"
          />
          {resultados.length > 0 && (
            <ul className="absolute z-10 mt-1 w-full rounded-lg border bg-white shadow-lg">
              {resultados.map((p, i) => (
                <li
                  key={p.id}
                  onMouseDown={() => agregarAlCarrito(p)}
                  className={`flex cursor-pointer justify-between px-4 py-2 ${i === indice ? 'bg-amber-100' : 'hover:bg-slate-50'}`}
                >
                  <span>
                    {p.nombre}
                    {p.codigoBarras && <span className="ml-2 text-xs text-slate-400">{p.codigoBarras}</span>}
                  </span>
                  <span className="font-semibold">{euros(p.precioVenta)}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        {error && <p className="mt-2 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}

        <table className="mt-4 w-full rounded-lg bg-white shadow">
          <thead>
            <tr className="border-b text-left text-sm text-slate-500">
              <th className="p-3">Producto</th>
              <th className="p-3">Precio</th>
              <th className="p-3">Cantidad</th>
              <th className="p-3 text-right">Subtotal</th>
              <th className="p-3"></th>
            </tr>
          </thead>
          <tbody>
            {carrito.length === 0 && (
              <tr>
                <td colSpan={5} className="p-6 text-center text-slate-400">
                  El carrito está vacío. Escanea o busca un producto.
                </td>
              </tr>
            )}
            {carrito.map((l) => (
              <tr key={l.producto.id} className="border-b last:border-0">
                <td className="p-3">{l.producto.nombre}</td>
                <td className="p-3">{euros(l.producto.precioVenta)}</td>
                <td className="p-3">
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => cambiarCantidad(l.producto.id, l.cantidad - 1)}
                      className="h-7 w-7 rounded bg-slate-200 font-bold hover:bg-slate-300"
                    >
                      −
                    </button>
                    <input
                      type="number"
                      min="0"
                      step={l.producto.unidadMedida === 'UNIDAD' ? 1 : 0.001}
                      value={l.cantidad}
                      onChange={(e) => cambiarCantidad(l.producto.id, Number(e.target.value))}
                      className="w-20 rounded border p-1 text-center"
                    />
                    <button
                      onClick={() => cambiarCantidad(l.producto.id, l.cantidad + 1)}
                      className="h-7 w-7 rounded bg-slate-200 font-bold hover:bg-slate-300"
                    >
                      +
                    </button>
                  </div>
                </td>
                <td className="p-3 text-right font-semibold">{euros(l.producto.precioVenta * l.cantidad)}</td>
                <td className="p-3 text-right">
                  <button
                    onClick={() => cambiarCantidad(l.producto.id, 0)}
                    className="rounded px-2 py-1 text-red-600 hover:bg-red-50"
                  >
                    ✕
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="rounded-lg bg-white p-4 shadow lg:sticky lg:top-4 lg:self-start">
        <p className="flex justify-between text-3xl font-bold">
          <span>Total</span>
          <span>{euros(total)}</span>
        </p>
        <div className="mt-2 border-t pt-2 text-sm text-slate-500">
          {Object.entries(desgloseIva).map(([tipo, importe]) => (
            <p key={tipo} className="flex justify-between">
              <span>IVA {tipo}% incluido</span>
              <span>{euros(importe)}</span>
            </p>
          ))}
        </div>
        <div className="mt-4 grid gap-2">
          <button
            onClick={() => cobrar('EFECTIVO')}
            disabled={carrito.length === 0 || cobrando}
            className="rounded-lg bg-green-600 py-3 text-lg font-bold text-white hover:bg-green-700 disabled:opacity-40"
          >
            Cobrar en efectivo <kbd className="ml-1 rounded bg-green-800 px-1.5 text-xs">F2</kbd>
          </button>
          <button
            onClick={() => cobrar('TARJETA')}
            disabled={carrito.length === 0 || cobrando}
            className="rounded-lg bg-blue-600 py-3 text-lg font-bold text-white hover:bg-blue-700 disabled:opacity-40"
          >
            Cobrar con tarjeta <kbd className="ml-1 rounded bg-blue-800 px-1.5 text-xs">F3</kbd>
          </button>
          <button
            onClick={() => {
              setCarrito([])
              enfocarBuscador()
            }}
            disabled={carrito.length === 0}
            className="rounded-lg bg-slate-200 py-2 text-sm font-semibold hover:bg-slate-300 disabled:opacity-40"
          >
            Vaciar carrito <kbd className="ml-1 rounded bg-slate-400 px-1.5 text-xs text-white">F4</kbd>
          </button>
        </div>
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
                onClick={() => {
                  setTicket(null)
                  setTimeout(enfocarBuscador, 0)
                }}
                className="flex-1 rounded bg-amber-500 py-2 font-semibold text-white hover:bg-amber-600"
              >
                Nueva venta (Enter)
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
