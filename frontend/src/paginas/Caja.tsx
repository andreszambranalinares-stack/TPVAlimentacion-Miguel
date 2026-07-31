import axios from 'axios'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  buscarProductos,
  crearVenta,
  esErrorDeSesionCaducada,
  mensajeDeError,
  productoPorCodigoBarras,
  productosBajoMinimo,
} from '../api'
import CobroEfectivoModal from '../componentes/CobroEfectivoModal'
import TicketModal from '../componentes/TicketModal'
import { useEsAdmin } from '../AuthContexto'
import { euros, pareceCodigoBarras, seleccionarAlFoco } from '../utils'
import type { MetodoPago, Producto, Venta } from '../tipos'

interface LineaCarrito {
  producto: Producto
  cantidad: number
  descuentoPorcentaje: number
}

interface VentaAparcada {
  id: string
  nota: string
  creadaEn: string
  carrito: LineaCarrito[]
}

const subtotalLinea = (l: LineaCarrito) =>
  l.producto.precioVenta * l.cantidad * (1 - l.descuentoPorcentaje / 100)

const CLAVE_APARCADAS = 'tpv-ventas-aparcadas'

const leerAparcadas = (): VentaAparcada[] => {
  try {
    const guardado = localStorage.getItem(CLAVE_APARCADAS)
    return guardado ? (JSON.parse(guardado) as VentaAparcada[]) : []
  } catch {
    return []
  }
}

const guardarAparcadas = (ventas: VentaAparcada[]) => {
  localStorage.setItem(CLAVE_APARCADAS, JSON.stringify(ventas))
}

/**
 * Pantalla de caja pensada para teclado y lector de códigos de barras:
 * el foco vive en el buscador, Enter añade y F2/F3 cobran.
 */
export default function Caja() {
  const esAdmin = useEsAdmin()
  const [busqueda, setBusqueda] = useState('')
  const [resultados, setResultados] = useState<Producto[]>([])
  const [indice, setIndice] = useState(0)
  const [carrito, setCarrito] = useState<LineaCarrito[]>([])
  const [textoCantidad, setTextoCantidad] = useState<Record<number, string>>({})
  const [error, setError] = useState('')
  const [cobrando, setCobrando] = useState(false)
  const [pidiendoEfectivo, setPidiendoEfectivo] = useState(false)
  const [ticket, setTicket] = useState<Venta | null>(null)
  const [entregadoTicket, setEntregadoTicket] = useState<number | null>(null)
  const [rapidos, setRapidos] = useState<Producto[]>([])
  const [bajoMinimo, setBajoMinimo] = useState<Producto[]>([])
  const [avisoStockAbierto, setAvisoStockAbierto] = useState(false)
  const [aparcadas, setAparcadas] = useState<VentaAparcada[]>(leerAparcadas)
  const [listaAparcadasAbierta, setListaAparcadasAbierta] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  // Casillas de cantidad del carrito, para saltar a la del producto a peso recién añadido.
  const cantidadRefs = useRef<Record<number, HTMLInputElement | null>>({})
  const productoAPesarRef = useRef<number | null>(null)
  // Búsqueda que corresponde a los `resultados` mostrados; evita añadir un
  // producto de una lista de sugerencias ya obsoleta si el usuario teclea rápido.
  const ultimaConsultaRef = useRef('')

  const enfocarBuscador = () => inputRef.current?.focus()

  useEffect(() => {
    enfocarBuscador()
  }, [])

  // Productos sin código de barras (fruta, pan, granel): los que más se teclean.
  useEffect(() => {
    buscarProductos()
      .then((lista) => setRapidos(lista.filter((p) => !p.codigoBarras).slice(0, 12)))
      .catch(() => setRapidos([]))
  }, [])

  // Aviso de stock bajo: se comprueba al entrar y cada pocos minutos, para que
  // el cajero lo vea sin tener que ir a Informes.
  const cargarBajoMinimo = useCallback(() => {
    productosBajoMinimo()
      .then(setBajoMinimo)
      .catch(() => {})
  }, [])

  useEffect(() => {
    cargarBajoMinimo()
    const intervalo = setInterval(cargarBajoMinimo, 5 * 60 * 1000)
    return () => clearInterval(intervalo)
  }, [cargarBajoMinimo])

  // Un producto a peso se añade con cantidad 1: llevamos el cursor a la casilla
  // de cantidad para que el cajero teclee los kilos directamente.
  useEffect(() => {
    const id = productoAPesarRef.current
    if (id === null) return
    productoAPesarRef.current = null
    const casilla = cantidadRefs.current[id]
    casilla?.focus()
    casilla?.select()
  }, [carrito])

  // Búsqueda incremental por nombre (el código de barras va por Enter)
  useEffect(() => {
    const texto = busqueda.trim()
    if (texto.length < 2 || pareceCodigoBarras(texto)) {
      setResultados([])
      setIndice(0)
      return
    }
    const controlador = new AbortController()
    const temporizador = setTimeout(() => {
      buscarProductos(texto, undefined, undefined, controlador.signal)
        .then((lista) => {
          ultimaConsultaRef.current = texto
          setResultados(lista.slice(0, 8))
          setIndice(0)
        })
        .catch((e) => {
          if (axios.isCancel(e)) return
          setResultados([])
        })
    }, 150)
    return () => {
      clearTimeout(temporizador)
      controlador.abort()
    }
  }, [busqueda])

  const agregarAlCarrito = useCallback((producto: Producto) => {
    setError('')
    setCarrito((actual) => {
      const existente = actual.find((l) => l.producto.id === producto.id)
      if (existente) {
        return actual.map((l) => (l.producto.id === producto.id ? { ...l, cantidad: l.cantidad + 1 } : l))
      }
      return [...actual, { producto, cantidad: 1, descuentoPorcentaje: 0 }]
    })
    setBusqueda('')
    setResultados([])
    if (producto.unidadMedida === 'UNIDAD') {
      enfocarBuscador()
    } else {
      productoAPesarRef.current = producto.id
    }
  }, [])

  const cambiarDescuento = (productoId: number, descuentoPorcentaje: number) => {
    const acotado = Math.min(100, Math.max(0, descuentoPorcentaje))
    setCarrito((actual) =>
      actual.map((l) => (l.producto.id === productoId ? { ...l, descuentoPorcentaje: acotado } : l)),
    )
  }

  const quitarTextoCantidad = (productoId: number) => {
    setTextoCantidad((actual) => {
      const { [productoId]: _omitido, ...resto } = actual
      return resto
    })
  }

  const cambiarCantidad = (productoId: number, cantidad: number) => {
    if (cantidad <= 0) {
      setCarrito((actual) => actual.filter((l) => l.producto.id !== productoId))
      quitarTextoCantidad(productoId)
    } else {
      setCarrito((actual) => actual.map((l) => (l.producto.id === productoId ? { ...l, cantidad } : l)))
    }
    enfocarBuscador()
  }

  // Deja el carrito actual en espera (por ejemplo si el cliente se va a buscar
  // algo) para poder atender a otro cliente sin perderlo.
  const aparcarVenta = () => {
    if (carrito.length === 0) return
    const nota = prompt('Nombre o nota para esta venta aparcada (opcional):', '')
    if (nota === null) return // Canceló: no aparcamos nada.
    const nueva: VentaAparcada = {
      id: crypto.randomUUID(),
      nota: nota.trim(),
      creadaEn: new Date().toISOString(),
      carrito,
    }
    setAparcadas((actual) => {
      const actualizadas = [...actual, nueva]
      guardarAparcadas(actualizadas)
      return actualizadas
    })
    setCarrito([])
    setTextoCantidad({})
    enfocarBuscador()
  }

  const recuperarAparcada = (id: string) => {
    const venta = aparcadas.find((v) => v.id === id)
    if (!venta) return
    if (carrito.length > 0 && !confirm('Ya hay una venta en marcha en el carrito. ¿Sustituirla por la aparcada?')) {
      return
    }
    setCarrito(venta.carrito)
    setTextoCantidad({})
    setAparcadas((actual) => {
      const actualizadas = actual.filter((v) => v.id !== id)
      guardarAparcadas(actualizadas)
      return actualizadas
    })
    setListaAparcadasAbierta(false)
    enfocarBuscador()
  }

  const descartarAparcada = (id: string) => {
    if (!confirm('¿Descartar esta venta aparcada? No se podrá recuperar.')) return
    setAparcadas((actual) => {
      const actualizadas = actual.filter((v) => v.id !== id)
      guardarAparcadas(actualizadas)
      return actualizadas
    })
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
          if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
          setBusqueda('')
        }
      } else if (resultados[indice] && ultimaConsultaRef.current === texto) {
        agregarAlCarrito(resultados[indice])
      }
    } else if (evento.key === 'Escape') {
      setBusqueda('')
      setResultados([])
    }
  }

  const cobrar = useCallback(
    async (metodoPago: MetodoPago, entregado: number | null = null) => {
      if (carrito.length === 0 || cobrando) return
      setCobrando(true)
      setError('')
      try {
        const venta = await crearVenta({
          metodoPago,
          lineas: carrito.map((l) => ({
            productoId: l.producto.id,
            cantidad: l.cantidad,
            descuentoPorcentaje: l.descuentoPorcentaje || undefined,
          })),
        })
        setTicket(venta)
        setEntregadoTicket(entregado)
        setPidiendoEfectivo(false)
        setCarrito([])
        setTextoCantidad({})
        cargarBajoMinimo()
      } catch (e) {
        if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
      } finally {
        setCobrando(false)
      }
    },
    [carrito, cobrando, cargarBajoMinimo],
  )

  const pedirEfectivo = useCallback(() => {
    if (carrito.length === 0 || cobrando) return
    setError('')
    setPidiendoEfectivo(true)
  }, [carrito, cobrando])

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
      // El cuadro de efectivo gestiona sus propias teclas (Enter/Escape).
      if (pidiendoEfectivo) return
      if (evento.key === 'F2') {
        evento.preventDefault()
        pedirEfectivo()
      } else if (evento.key === 'F3') {
        evento.preventDefault()
        void cobrar('TARJETA')
      } else if (evento.key === 'F4') {
        evento.preventDefault()
        setCarrito([])
        setTextoCantidad({})
        enfocarBuscador()
      }
    }
    window.addEventListener('keydown', manejador)
    return () => window.removeEventListener('keydown', manejador)
  }, [cobrar, pedirEfectivo, pidiendoEfectivo, ticket])

  const total = carrito.reduce((suma, l) => suma + subtotalLinea(l), 0)
  const desgloseIva = carrito.reduce<Record<string, number>>((acc, l) => {
    const subtotal = subtotalLinea(l)
    const iva = (subtotal * l.producto.ivaPorcentaje) / (100 + l.producto.ivaPorcentaje)
    const clave = `${l.producto.ivaPorcentaje}`
    acc[clave] = (acc[clave] ?? 0) + iva
    return acc
  }, {})

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <div className="lg:col-span-2">
        {bajoMinimo.length > 0 && (
          <div className="mb-3 rounded-lg border border-amber-300 bg-amber-50">
            <button
              onClick={() => setAvisoStockAbierto((a) => !a)}
              className="flex w-full items-center justify-between px-4 py-2 text-left text-sm font-semibold text-amber-800"
            >
              <span>
                ⚠ {bajoMinimo.length} producto{bajoMinimo.length === 1 ? '' : 's'} con poco stock
              </span>
              <span className="text-xs underline">{avisoStockAbierto ? 'Ocultar' : 'Ver'}</span>
            </button>
            {avisoStockAbierto && (
              <ul className="border-t border-amber-200 px-4 py-2 text-sm text-amber-900">
                {bajoMinimo.map((p) => (
                  <li key={p.id} className="flex justify-between py-0.5">
                    <span>{p.nombre}</span>
                    <span className="font-semibold">
                      {p.stockActual} {p.unidadMedida !== 'UNIDAD' ? p.unidadMedida.toLowerCase() : 'ud.'}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
        <div className="relative">
          <input
            ref={inputRef}
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            onKeyDown={alPulsarTecla}
            placeholder="Escanea un código de barras o escribe el nombre y pulsa Enter…"
            className="w-full rounded-lg border-2 border-slate-300 p-4 text-lg shadow focus:border-amber-500 focus:outline-none focus:ring-4 focus:ring-amber-300"
            autoComplete="off"
            aria-label="Buscar producto por nombre o código de barras"
            role="combobox"
            aria-expanded={resultados.length > 0}
            aria-controls="resultados-busqueda-caja"
          />
          {resultados.length > 0 && (
            <ul
              id="resultados-busqueda-caja"
              role="listbox"
              aria-label="Resultados de búsqueda"
              className="absolute z-10 mt-1 w-full rounded-lg border bg-white shadow-lg"
            >
              {resultados.map((p, i) => (
                <li
                  key={p.id}
                  role="option"
                  aria-selected={i === indice}
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

        {error && (
          <p role="alert" className="mt-2 rounded bg-red-100 px-3 py-2 text-red-700">
            {error}
          </p>
        )}

        <table className="mt-4 w-full rounded-lg bg-white shadow">
          <thead>
            <tr className="border-b text-left text-sm text-slate-500">
              <th className="p-3">Producto</th>
              <th className="p-3">Precio</th>
              <th className="p-3">Cantidad</th>
              {esAdmin && <th className="p-3">Dto. %</th>}
              <th className="p-3 text-right">Subtotal</th>
              <th className="p-3"></th>
            </tr>
          </thead>
          <tbody>
            {carrito.length === 0 && (
              <tr>
                <td colSpan={esAdmin ? 6 : 5} className="p-6 text-center text-slate-400">
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
                      aria-label={`Disminuir cantidad de ${l.producto.nombre}`}
                      className="h-7 w-7 rounded bg-slate-200 font-bold hover:bg-slate-300"
                    >
                      −
                    </button>
                    <input
                      ref={(el) => {
                        cantidadRefs.current[l.producto.id] = el
                      }}
                      type="number"
                      min="0"
                      step={l.producto.unidadMedida === 'UNIDAD' ? 1 : 0.001}
                      value={textoCantidad[l.producto.id] ?? String(l.cantidad)}
                      onChange={(e) => {
                        const texto = e.target.value
                        setTextoCantidad((actual) => ({ ...actual, [l.producto.id]: texto }))
                        const numero = Number(texto)
                        if (texto !== '' && !Number.isNaN(numero) && numero > 0) {
                          setCarrito((actual) =>
                            actual.map((linea) =>
                              linea.producto.id === l.producto.id ? { ...linea, cantidad: numero } : linea,
                            ),
                          )
                        }
                      }}
                      onBlur={() => {
                        const texto = textoCantidad[l.producto.id]
                        if (texto === undefined) return
                        const numero = Number(texto)
                        if (texto === '' || Number.isNaN(numero) || numero <= 0) {
                          cambiarCantidad(l.producto.id, 0)
                        }
                        quitarTextoCantidad(l.producto.id)
                      }}
                      onFocus={seleccionarAlFoco}
                      aria-label={`Cantidad de ${l.producto.nombre}`}
                      className="w-20 rounded border p-1 text-center"
                    />
                    <button
                      onClick={() => cambiarCantidad(l.producto.id, l.cantidad + 1)}
                      aria-label={`Aumentar cantidad de ${l.producto.nombre}`}
                      className="h-7 w-7 rounded bg-slate-200 font-bold hover:bg-slate-300"
                    >
                      +
                    </button>
                  </div>
                </td>
                {esAdmin && (
                  <td className="p-3">
                    <input
                      type="number"
                      min="0"
                      max="100"
                      step="1"
                      value={l.descuentoPorcentaje}
                      onChange={(e) => cambiarDescuento(l.producto.id, Number(e.target.value))}
                      onFocus={seleccionarAlFoco}
                      aria-label={`Descuento en % para ${l.producto.nombre}`}
                      className="w-16 rounded border p-1 text-center"
                    />
                  </td>
                )}
                <td className="p-3 text-right font-semibold">{euros(subtotalLinea(l))}</td>
                <td className="p-3 text-right">
                  <button
                    onClick={() => cambiarCantidad(l.producto.id, 0)}
                    aria-label={`Quitar ${l.producto.nombre} del carrito`}
                    className="rounded px-2 py-1 text-red-600 hover:bg-red-50"
                  >
                    ✕
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {rapidos.length > 0 && (
          <div className="mt-4 rounded-lg bg-white p-4 shadow">
            <p className="mb-3 text-sm font-semibold text-slate-500">Sin código de barras</p>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4">
              {rapidos.map((p) => (
                <button
                  key={p.id}
                  onClick={() => agregarAlCarrito(p)}
                  className="rounded-lg border border-slate-200 p-3 text-left hover:border-amber-400 hover:bg-amber-50"
                >
                  <span className="block text-sm font-semibold leading-tight">{p.nombre}</span>
                  <span className="mt-1 block text-xs text-slate-500">
                    {euros(p.precioVenta)}
                    {p.unidadMedida !== 'UNIDAD' && ` / ${p.unidadMedida.toLowerCase()}`}
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}
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
            onClick={pedirEfectivo}
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
              setTextoCantidad({})
              enfocarBuscador()
            }}
            disabled={carrito.length === 0}
            className="rounded-lg bg-slate-200 py-2 text-sm font-semibold hover:bg-slate-300 disabled:opacity-40"
          >
            Vaciar carrito <kbd className="ml-1 rounded bg-slate-400 px-1.5 text-xs text-white">F4</kbd>
          </button>
          <button
            onClick={aparcarVenta}
            disabled={carrito.length === 0}
            className="rounded-lg border border-slate-300 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 disabled:opacity-40"
          >
            Aparcar venta (atender a otro cliente)
          </button>
        </div>

        {aparcadas.length > 0 && (
          <div className="mt-4 border-t pt-3">
            <button
              onClick={() => setListaAparcadasAbierta((a) => !a)}
              className="flex w-full items-center justify-between text-left text-sm font-semibold text-slate-700"
            >
              <span>
                🅿️ {aparcadas.length} venta{aparcadas.length === 1 ? '' : 's'} aparcada
                {aparcadas.length === 1 ? '' : 's'}
              </span>
              <span className="text-xs text-blue-600 underline">{listaAparcadasAbierta ? 'Ocultar' : 'Ver'}</span>
            </button>
            {listaAparcadasAbierta && (
              <ul className="mt-2 space-y-2">
                {aparcadas.map((v) => {
                  const totalVenta = v.carrito.reduce((suma, l) => suma + subtotalLinea(l), 0)
                  const hora = new Date(v.creadaEn).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
                  return (
                    <li key={v.id} className="rounded border p-2 text-sm">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold">{v.nota || `Venta de las ${hora}`}</span>
                        <span>{euros(totalVenta)}</span>
                      </div>
                      <p className="text-xs text-slate-500">
                        {v.carrito.length} producto{v.carrito.length === 1 ? '' : 's'} · {hora}
                      </p>
                      <div className="mt-1 flex gap-2">
                        <button
                          onClick={() => recuperarAparcada(v.id)}
                          className="rounded bg-amber-500 px-2 py-1 text-xs font-semibold text-white hover:bg-amber-600"
                        >
                          Recuperar
                        </button>
                        <button
                          onClick={() => descartarAparcada(v.id)}
                          className="rounded px-2 py-1 text-xs text-red-600 hover:bg-red-50"
                        >
                          Descartar
                        </button>
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>
        )}
      </div>

      {pidiendoEfectivo && (
        <CobroEfectivoModal
          total={total}
          cobrando={cobrando}
          onConfirmar={(entregado) => void cobrar('EFECTIVO', entregado)}
          onCancelar={() => {
            setPidiendoEfectivo(false)
            enfocarBuscador()
          }}
        />
      )}

      {ticket && (
        <TicketModal
          venta={ticket}
          entregado={entregadoTicket}
          textoBotonCerrar="Nueva venta (Enter)"
          onCerrar={() => {
            setTicket(null)
            setEntregadoTicket(null)
            setTimeout(enfocarBuscador, 0)
          }}
        />
      )}
    </div>
  )
}
