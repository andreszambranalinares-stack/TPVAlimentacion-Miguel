import { useEffect, useState } from 'react'
import {
  actualizarProveedor,
  crearProveedor,
  eliminarProveedor,
  esErrorDeSesionCaducada,
  listarProveedores,
  mensajeDeError,
} from '../api'
import type { Proveedor } from '../tipos'

const formularioVacio = { nombre: '', telefono: '', contacto: '', email: '', direccion: '' }

export default function Proveedores() {
  const [proveedores, setProveedores] = useState<Proveedor[]>([])
  const [editando, setEditando] = useState<Proveedor | null>(null)
  const [formulario, setFormulario] = useState(formularioVacio)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(true)

  const cargar = () => {
    setCargando(true)
    listarProveedores()
      .then(setProveedores)
      .catch((e) => {
        if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
      })
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [])

  const abrirEdicion = (p: Proveedor) => {
    setEditando(p)
    setFormulario({
      nombre: p.nombre,
      telefono: p.telefono ?? '',
      contacto: p.contacto ?? '',
      email: p.email ?? '',
      direccion: p.direccion ?? '',
    })
    setError('')
  }

  const cancelarEdicion = () => {
    setEditando(null)
    setFormulario(formularioVacio)
    setError('')
  }

  const guardar = async (evento: React.FormEvent) => {
    evento.preventDefault()
    setError('')
    const datos = {
      nombre: formulario.nombre,
      telefono: formulario.telefono || null,
      contacto: formulario.contacto || null,
      email: formulario.email || null,
      direccion: formulario.direccion || null,
    }
    try {
      if (editando) {
        await actualizarProveedor(editando.id, datos)
      } else {
        await crearProveedor(datos)
      }
      setEditando(null)
      setFormulario(formularioVacio)
      cargar()
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  const eliminar = async (p: Proveedor) => {
    if (!confirm(`¿Eliminar el proveedor "${p.nombre}"?`)) return
    await eliminarProveedor(p.id)
    if (editando?.id === p.id) cancelarEdicion()
    cargar()
  }

  const campo = 'rounded border p-2'

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <form onSubmit={guardar} className="rounded-lg bg-white p-4 shadow lg:self-start">
        <h2 className="mb-3 text-lg font-bold">{editando ? `Editar "${editando.nombre}"` : 'Nuevo proveedor'}</h2>
        {error && <p className="mb-2 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}
        <div className="grid gap-3">
          <input
            required
            value={formulario.nombre}
            onChange={(e) => setFormulario({ ...formulario, nombre: e.target.value })}
            placeholder="Nombre *"
            className={campo}
          />
          <input
            value={formulario.telefono}
            onChange={(e) => setFormulario({ ...formulario, telefono: e.target.value })}
            placeholder="Teléfono"
            className={campo}
          />
          <input
            type="email"
            value={formulario.email}
            onChange={(e) => setFormulario({ ...formulario, email: e.target.value })}
            placeholder="Email (para mandar los pedidos)"
            className={campo}
          />
          <input
            value={formulario.direccion}
            onChange={(e) => setFormulario({ ...formulario, direccion: e.target.value })}
            placeholder="Dirección"
            className={campo}
          />
          <input
            value={formulario.contacto}
            onChange={(e) => setFormulario({ ...formulario, contacto: e.target.value })}
            placeholder="Persona de contacto"
            className={campo}
          />
          <div className="flex gap-2">
            {editando && (
              <button type="button" onClick={cancelarEdicion} className="flex-1 rounded bg-slate-200 py-2 font-semibold hover:bg-slate-300">
                Cancelar
              </button>
            )}
            <button className="flex-1 rounded bg-amber-500 py-2 font-semibold text-white hover:bg-amber-600">
              {editando ? 'Guardar cambios' : 'Guardar'}
            </button>
          </div>
        </div>
      </form>

      <table className="w-full rounded-lg bg-white shadow lg:col-span-2">
        <thead>
          <tr className="border-b text-left text-sm text-slate-500">
            <th className="p-3">Nombre</th>
            <th className="p-3">Teléfono</th>
            <th className="p-3">Email</th>
            <th className="p-3">Contacto</th>
            <th className="p-3"></th>
          </tr>
        </thead>
        <tbody>
          {proveedores.map((p) => (
            <tr key={p.id} className="border-b last:border-0">
              <td className="p-3">{p.nombre}</td>
              <td className="p-3">{p.telefono ?? '—'}</td>
              <td className="p-3">{p.email ?? '—'}</td>
              <td className="p-3">{p.contacto ?? '—'}</td>
              <td className="p-3 text-right">
                <button onClick={() => abrirEdicion(p)} className="mr-3 text-blue-600 hover:underline">
                  Editar
                </button>
                <button onClick={() => eliminar(p)} className="text-red-600 hover:underline">
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
          {cargando && (
            <tr>
              <td colSpan={5} className="p-6 text-center text-slate-400">
                Cargando…
              </td>
            </tr>
          )}
          {!cargando && proveedores.length === 0 && (
            <tr>
              <td colSpan={5} className="p-6 text-center text-slate-400">
                Sin proveedores todavía.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
