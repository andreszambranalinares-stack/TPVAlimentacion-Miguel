import { useCallback, useEffect, useState } from 'react'
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
import { euros } from '../utils'
import type { Categoria, Producto, ProductoEntrada, UnidadMedida } from '../tipos'

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

  const abrirAlta = () => {
    setEditando(null)
    setFormulario(formularioVacio)
    setMostrarFormulario(true)
    setError('')
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
    })
    setMostrarFormulario(true)
    setError('')
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
          <button
            onClick={abrirAlta}
            className="ml-auto rounded-lg bg-amber-500 px-4 py-2 font-semibold text-white hover:bg-amber-600"
          >
            + Nuevo producto
          </button>
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
                  value={formulario.nombre}
                  onChange={(e) => setFormulario({ ...formulario, nombre: e.target.value })}
                  className={campo}
                  autoFocus
                />
              </label>
              <label>
                <span className="text-sm text-slate-600">Código de barras</span>
                <input
                  value={formulario.codigoBarras ?? ''}
                  onChange={(e) => setFormulario({ ...formulario, codigoBarras: e.target.value || null })}
                  className={campo}
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
            </div>
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
    </div>
  )
}
