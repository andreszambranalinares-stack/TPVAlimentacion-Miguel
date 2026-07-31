import { useCallback, useEffect, useRef, useState } from 'react'
import {
  actualizarProducto,
  buscarProductos,
  crearCategoria,
  crearProducto,
  desactivarProducto,
  esErrorDeSesionCaducada,
  listarCategorias,
  mensajeDeError,
} from '../api'
import { useEsAdmin } from '../AuthContexto'
import { euros, normalizarTexto, parsearProductosCsv } from '../utils'
import type { Categoria, Producto, ProductoEntrada, UnidadMedida } from '../tipos'

interface ResultadoImportacion {
  creados: number
  errores: { fila: number; motivo: string }[]
}

const formularioVacio: ProductoEntrada = {
  nombre: '',
  codigoBarras: null,
  categoriaId: null,
  precioVenta: 0,
  precioCoste: null,
  ivaPorcentaje: 21,
  stockMinimo: null,
  stockInicial: null,
  unidadMedida: 'UNIDAD',
  activo: true,
  esPack: false,
  componentes: [],
}

export default function Productos() {
  const esAdmin = useEsAdmin()
  const [productos, setProductos] = useState<Producto[]>([])
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [texto, setTexto] = useState('')
  const [categoriaId, setCategoriaId] = useState<number | ''>('')
  const [incluirInactivos, setIncluirInactivos] = useState(false)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(true)

  const [editando, setEditando] = useState<Producto | null>(null)
  const [mostrarFormulario, setMostrarFormulario] = useState(false)
  const [formulario, setFormulario] = useState<ProductoEntrada>(formularioVacio)
  const [nuevaCategoria, setNuevaCategoria] = useState('')
  const [nombresComponentes, setNombresComponentes] = useState<Record<number, string>>({})
  const [catalogo, setCatalogo] = useState<Producto[]>([])
  const [componenteAAgregar, setComponenteAAgregar] = useState<number | ''>('')
  const [cantidadComponente, setCantidadComponente] = useState('1')
  const nombreRef = useRef<HTMLInputElement>(null)
  const archivoCsvRef = useRef<HTMLInputElement>(null)
  const [importando, setImportando] = useState(false)
  const [resultadoImportacion, setResultadoImportacion] = useState<ResultadoImportacion | null>(null)

  const cargar = useCallback(() => {
    setCargando(true)
    buscarProductos(texto, categoriaId === '' ? undefined : categoriaId, incluirInactivos)
      .then(setProductos)
      .catch((e) => {
        if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
      })
      .finally(() => setCargando(false))
  }, [texto, categoriaId, incluirInactivos])

  useEffect(() => {
    const temporizador = setTimeout(cargar, 200)
    return () => clearTimeout(temporizador)
  }, [cargar])

  useEffect(() => {
    listarCategorias()
      .then(setCategorias)
      .catch((e) => {
        if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
      })
  }, [])

  const cargarCatalogo = () => {
    buscarProductos()
      .then((lista) => setCatalogo(lista.filter((p) => !p.esPack)))
      .catch(() => setCatalogo([]))
  }

  useEffect(cargarCatalogo, [])

  const abrirAlta = () => {
    setEditando(null)
    setFormulario(formularioVacio)
    setNombresComponentes({})
    setComponenteAAgregar('')
    setCantidadComponente('1')
    setMostrarFormulario(true)
    setError('')
    cargarCatalogo()
  }

  const abrirEdicion = (p: Producto) => {
    setEditando(p)
    setFormulario({
      nombre: p.nombre,
      codigoBarras: p.codigoBarras,
      categoriaId: p.categoriaId,
      precioVenta: p.precioVenta,
      precioCoste: p.precioCoste,
      ivaPorcentaje: p.ivaPorcentaje,
      stockMinimo: p.stockMinimo,
      stockInicial: null,
      unidadMedida: p.unidadMedida,
      activo: p.activo,
      esPack: p.esPack,
      componentes: p.componentes.map((c) => ({ productoId: c.productoId, cantidad: c.cantidad })),
    })
    setNombresComponentes(Object.fromEntries(p.componentes.map((c) => [c.productoId, c.productoNombre])))
    setComponenteAAgregar('')
    setCantidadComponente('1')
    setMostrarFormulario(true)
    setError('')
    cargarCatalogo()
  }

  const agregarComponente = () => {
    if (componenteAAgregar === '') return
    const cantidad = Number(cantidadComponente)
    if (!cantidad || cantidad <= 0) return
    const producto = catalogo.find((p) => p.id === componenteAAgregar)
    if (!producto) return
    setFormulario((f) => ({
      ...f,
      componentes: [...f.componentes.filter((c) => c.productoId !== componenteAAgregar), { productoId: componenteAAgregar, cantidad }],
    }))
    setNombresComponentes((actual) => ({ ...actual, [componenteAAgregar]: producto.nombre }))
    setComponenteAAgregar('')
    setCantidadComponente('1')
  }

  const quitarComponente = (productoId: number) => {
    setFormulario((f) => ({ ...f, componentes: f.componentes.filter((c) => c.productoId !== productoId) }))
  }

  const guardar = async (evento: React.FormEvent) => {
    evento.preventDefault()
    try {
      if (editando) {
        await actualizarProducto(editando.id, formulario)
      } else {
        await crearProducto(formulario)
      }
      setMostrarFormulario(false)
      cargar()
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  const darDeBaja = async (p: Producto) => {
    if (!confirm(`¿Dar de baja "${p.nombre}"? Dejará de venderse pero conservará su histórico.`)) return
    await desactivarProducto(p.id)
    cargar()
  }

  const agregarCategoria = async () => {
    if (!nuevaCategoria.trim()) return
    try {
      const creada = await crearCategoria(nuevaCategoria.trim())
      setCategorias((actual) => [...actual, creada].sort((a, b) => a.nombre.localeCompare(b.nombre)))
      setFormulario((f) => ({ ...f, categoriaId: creada.id }))
      setNuevaCategoria('')
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  const importarCsv = async (evento: React.ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0]
    evento.target.value = ''
    if (!archivo) return

    const texto = await archivo.text()
    const { productos: filas, invalidas } = parsearProductosCsv(texto)

    if (filas.length === 0 && invalidas.length === 0) {
      setError('El archivo no tiene productos que importar. Revisa que tenga cabecera y al menos una fila.')
      return
    }

    setImportando(true)
    setError('')
    const errores: { fila: number; motivo: string }[] = invalidas.map((i) => ({ fila: i.fila, motivo: i.motivo }))
    let creados = 0

    // Cache local de categorías para no crear la misma dos veces durante la importación.
    const categoriasPorNombre = new Map(categorias.map((c) => [normalizarTexto(c.nombre), c.id]))

    for (let i = 0; i < filas.length; i++) {
      const { categoriaNombre, ...datos } = filas[i]
      try {
        let categoriaId: number | null = null
        if (categoriaNombre) {
          const clave = normalizarTexto(categoriaNombre)
          categoriaId = categoriasPorNombre.get(clave) ?? null
          if (categoriaId === null) {
            const creada = await crearCategoria(categoriaNombre)
            categoriaId = creada.id
            categoriasPorNombre.set(clave, creada.id)
            setCategorias((actual) => [...actual, creada].sort((a, b) => a.nombre.localeCompare(b.nombre)))
          }
        }
        await crearProducto({ ...datos, categoriaId })
        creados++
      } catch (e) {
        if (esErrorDeSesionCaducada(e)) {
          setImportando(false)
          return
        }
        errores.push({ fila: i + 2, motivo: `"${datos.nombre}": ${mensajeDeError(e)}` })
      }
    }

    setImportando(false)
    setResultadoImportacion({ creados, errores })
    if (creados > 0) cargar()
  }

  const campo = 'w-full rounded border p-2'

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <input
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          placeholder="Buscar por nombre o código de barras…"
          className="w-72 rounded-lg border p-2"
        />
        <select
          value={categoriaId}
          onChange={(e) => setCategoriaId(e.target.value === '' ? '' : Number(e.target.value))}
          className="rounded-lg border p-2"
        >
          <option value="">Todas las categorías</option>
          {categorias.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre}
            </option>
          ))}
        </select>
        <label className="flex items-center gap-1 text-sm">
          <input type="checkbox" checked={incluirInactivos} onChange={(e) => setIncluirInactivos(e.target.checked)} />
          Ver dados de baja
        </label>
        {esAdmin && (
          <div className="ml-auto flex gap-2">
            <input
              ref={archivoCsvRef}
              type="file"
              accept=".csv,text/csv"
              onChange={importarCsv}
              className="hidden"
            />
            <button
              onClick={() => archivoCsvRef.current?.click()}
              disabled={importando}
              className="rounded-lg border border-amber-500 px-4 py-2 font-semibold text-amber-700 hover:bg-amber-50 disabled:opacity-50"
            >
              {importando ? 'Importando…' : 'Importar CSV'}
            </button>
            <button
              onClick={abrirAlta}
              className="rounded-lg bg-amber-500 px-4 py-2 font-semibold text-white hover:bg-amber-600"
            >
              + Nuevo producto
            </button>
          </div>
        )}
      </div>

      {error && !mostrarFormulario && <p className="mb-2 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}

      <table className="w-full rounded-lg bg-white shadow">
        <thead>
          <tr className="border-b text-left text-sm text-slate-500">
            <th className="p-3">Nombre</th>
            <th className="p-3">Código de barras</th>
            <th className="p-3">Categoría</th>
            <th className="p-3 text-right">PVP</th>
            <th className="p-3 text-right">IVA</th>
            <th className="p-3 text-right">Stock</th>
            <th className="p-3"></th>
          </tr>
        </thead>
        <tbody>
          {productos.map((p) => (
            <tr key={p.id} className={`border-b last:border-0 ${!p.activo ? 'text-slate-400' : ''}`}>
              <td className="p-3">
                {p.nombre}
                {p.esPack && <span className="ml-2 rounded bg-amber-100 px-1.5 text-xs text-amber-700">pack</span>}
                {!p.activo && <span className="ml-2 rounded bg-slate-200 px-1.5 text-xs">baja</span>}
              </td>
              <td className="p-3 font-mono text-sm">{p.codigoBarras ?? '—'}</td>
              <td className="p-3">{p.categoriaNombre ?? '—'}</td>
              <td className="p-3 text-right">{euros(p.precioVenta)}</td>
              <td className="p-3 text-right">{p.ivaPorcentaje}%</td>
              <td className="p-3 text-right">
                <span className={p.bajoMinimo ? 'rounded bg-red-100 px-2 py-0.5 font-bold text-red-700' : ''}>
                  {p.stockActual} {p.unidadMedida !== 'UNIDAD' ? p.unidadMedida.toLowerCase() : ''}
                </span>
              </td>
              <td className="p-3 text-right">
                {esAdmin && (
                  <>
                    <button onClick={() => abrirEdicion(p)} className="mr-2 text-blue-600 hover:underline">
                      Editar
                    </button>
                    {p.activo && (
                      <button onClick={() => darDeBaja(p)} className="text-red-600 hover:underline">
                        Baja
                      </button>
                    )}
                  </>
                )}
              </td>
            </tr>
          ))}
          {cargando && (
            <tr>
              <td colSpan={7} className="p-6 text-center text-slate-400">
                Cargando…
              </td>
            </tr>
          )}
          {!cargando && productos.length === 0 && (
            <tr>
              <td colSpan={7} className="p-6 text-center text-slate-400">
                No hay productos que coincidan.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {mostrarFormulario && (
        <div className="fixed inset-0 z-20 flex items-center justify-center bg-black/50 p-4">
          <form onSubmit={guardar} className="max-h-[90vh] w-full max-w-lg overflow-auto rounded-lg bg-white p-6 shadow-xl">
            <h2 className="mb-4 text-xl font-bold">{editando ? 'Editar producto' : 'Nuevo producto'}</h2>
            {error && <p className="mb-3 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="sm:col-span-2">
                <span className="text-sm text-slate-600">Nombre *</span>
                <input
                  required
                  ref={nombreRef}
                  value={formulario.nombre}
                  onChange={(e) => setFormulario({ ...formulario, nombre: e.target.value })}
                  className={campo}
                />
              </label>
              <label>
                <span className="text-sm text-slate-600">Código de barras</span>
                <input
                  value={formulario.codigoBarras ?? ''}
                  onChange={(e) => setFormulario({ ...formulario, codigoBarras: e.target.value || null })}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault()
                      nombreRef.current?.focus()
                    }
                  }}
                  placeholder="Escanea el código o escríbelo…"
                  className={campo}
                  autoFocus
                />
              </label>
              <label>
                <span className="text-sm text-slate-600">Categoría</span>
                <select
                  value={formulario.categoriaId ?? ''}
                  onChange={(e) =>
                    setFormulario({ ...formulario, categoriaId: e.target.value === '' ? null : Number(e.target.value) })
                  }
                  className={campo}
                >
                  <option value="">Sin categoría</option>
                  {categorias.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.nombre}
                    </option>
                  ))}
                </select>
              </label>
              <div className="flex items-end gap-1 sm:col-span-2">
                <label className="flex-1">
                  <span className="text-sm text-slate-600">Crear categoría nueva</span>
                  <input
                    value={nuevaCategoria}
                    onChange={(e) => setNuevaCategoria(e.target.value)}
                    className={campo}
                    placeholder="p. ej. Congelados"
                  />
                </label>
                <button
                  type="button"
                  onClick={agregarCategoria}
                  className="rounded bg-slate-200 px-3 py-2 font-semibold hover:bg-slate-300"
                >
                  Añadir
                </button>
              </div>
              <label>
                <span className="text-sm text-slate-600">Precio de venta (IVA incluido) *</span>
                <input
                  required
                  type="number"
                  min="0"
                  step="0.01"
                  value={formulario.precioVenta}
                  onChange={(e) => setFormulario({ ...formulario, precioVenta: Number(e.target.value) })}
                  className={campo}
                />
              </label>
              <label>
                <span className="text-sm text-slate-600">Precio de coste</span>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={formulario.precioCoste ?? ''}
                  onChange={(e) =>
                    setFormulario({ ...formulario, precioCoste: e.target.value === '' ? null : Number(e.target.value) })
                  }
                  className={campo}
                />
              </label>
              <label>
                <span className="text-sm text-slate-600">IVA *</span>
                <select
                  value={formulario.ivaPorcentaje}
                  onChange={(e) => setFormulario({ ...formulario, ivaPorcentaje: Number(e.target.value) })}
                  className={campo}
                >
                  {[0, 4, 10, 21].map((iva) => (
                    <option key={iva} value={iva}>
                      {iva}%
                    </option>
                  ))}
                </select>
              </label>
              <label>
                <span className="text-sm text-slate-600">Unidad de medida</span>
                <select
                  value={formulario.unidadMedida}
                  onChange={(e) => setFormulario({ ...formulario, unidadMedida: e.target.value as UnidadMedida })}
                  className={campo}
                >
                  <option value="UNIDAD">Unidad</option>
                  <option value="KG">Kilo</option>
                  <option value="LITRO">Litro</option>
                </select>
              </label>
              <label>
                <span className="text-sm text-slate-600">Stock mínimo (aviso)</span>
                <input
                  type="number"
                  min="0"
                  step="any"
                  value={formulario.stockMinimo ?? ''}
                  onChange={(e) =>
                    setFormulario({ ...formulario, stockMinimo: e.target.value === '' ? null : Number(e.target.value) })
                  }
                  className={campo}
                />
              </label>
              {!editando && (
                <label>
                  <span className="text-sm text-slate-600">Stock inicial</span>
                  <input
                    type="number"
                    min="0"
                    step="any"
                    value={formulario.stockInicial ?? ''}
                    onChange={(e) =>
                      setFormulario({ ...formulario, stockInicial: e.target.value === '' ? null : Number(e.target.value) })
                    }
                    className={campo}
                  />
                </label>
              )}
              {editando && (
                <label className="flex items-center gap-2 self-end">
                  <input
                    type="checkbox"
                    checked={formulario.activo}
                    onChange={(e) => setFormulario({ ...formulario, activo: e.target.checked })}
                  />
                  <span className="text-sm text-slate-600">Activo (a la venta)</span>
                </label>
              )}
              <label className="flex items-center gap-2 self-end sm:col-span-2">
                <input
                  type="checkbox"
                  checked={formulario.esPack}
                  onChange={(e) => setFormulario({ ...formulario, esPack: e.target.checked, componentes: e.target.checked ? formulario.componentes : [] })}
                />
                <span className="text-sm text-slate-600">Es un pack o lote de varios productos</span>
              </label>
            </div>

            {formulario.esPack && (
              <div className="mt-3 rounded border border-amber-200 bg-amber-50 p-3">
                <p className="mb-2 text-sm font-semibold text-amber-800">
                  Componentes del pack (se descuentan de su propio stock al vender)
                </p>
                <ul className="mb-2 divide-y">
                  {formulario.componentes.length === 0 && (
                    <li className="py-1.5 text-sm text-slate-500">Añade al menos un componente.</li>
                  )}
                  {formulario.componentes.map((c) => (
                    <li key={c.productoId} className="flex items-center justify-between py-1.5 text-sm">
                      <span>
                        {nombresComponentes[c.productoId] ?? `Producto #${c.productoId}`} × {c.cantidad}
                      </span>
                      <button type="button" onClick={() => quitarComponente(c.productoId)} className="text-red-600 hover:underline">
                        Quitar
                      </button>
                    </li>
                  ))}
                </ul>
                <div className="flex items-end gap-2">
                  <label className="flex-1">
                    <span className="text-xs text-slate-600">Producto componente</span>
                    <select
                      value={componenteAAgregar}
                      onChange={(e) => setComponenteAAgregar(e.target.value === '' ? '' : Number(e.target.value))}
                      className={campo}
                    >
                      <option value="">Selecciona…</option>
                      {catalogo
                        .filter((p) => p.id !== editando?.id)
                        .map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.nombre}
                          </option>
                        ))}
                    </select>
                  </label>
                  <label className="w-24">
                    <span className="text-xs text-slate-600">Cantidad</span>
                    <input
                      type="number"
                      min="0"
                      step="any"
                      value={cantidadComponente}
                      onChange={(e) => setCantidadComponente(e.target.value)}
                      className={campo}
                    />
                  </label>
                  <button
                    type="button"
                    onClick={agregarComponente}
                    className="rounded bg-slate-700 px-3 py-2 font-semibold text-white hover:bg-slate-600"
                  >
                    Añadir
                  </button>
                </div>
              </div>
            )}

            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setMostrarFormulario(false)}
                className="rounded px-4 py-2 hover:bg-slate-100"
              >
                Cancelar
              </button>
              <button type="submit" className="rounded bg-amber-500 px-4 py-2 font-semibold text-white hover:bg-amber-600">
                Guardar
              </button>
            </div>
          </form>
        </div>
      )}

      {resultadoImportacion && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/50 p-4">
          <div className="max-h-[85vh] w-full max-w-md overflow-auto rounded-lg bg-white p-6 shadow-xl">
            <h2 className="mb-3 text-xl font-bold">Importación de productos</h2>
            <p className="mb-3 rounded bg-green-100 px-3 py-2 text-green-800">
              {resultadoImportacion.creados} producto{resultadoImportacion.creados === 1 ? '' : 's'} creado
              {resultadoImportacion.creados === 1 ? '' : 's'} correctamente.
            </p>
            {resultadoImportacion.errores.length > 0 && (
              <div className="rounded bg-red-100 px-3 py-2 text-red-800">
                <p className="mb-1 font-semibold">
                  {resultadoImportacion.errores.length} fila{resultadoImportacion.errores.length === 1 ? '' : 's'} con
                  errores:
                </p>
                <ul className="list-inside list-disc space-y-0.5 text-sm">
                  {resultadoImportacion.errores.map((e, i) => (
                    <li key={i}>
                      Fila {e.fila}: {e.motivo}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            <div className="mt-5 flex justify-end">
              <button
                onClick={() => setResultadoImportacion(null)}
                className="rounded bg-amber-500 px-4 py-2 font-semibold text-white hover:bg-amber-600"
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
