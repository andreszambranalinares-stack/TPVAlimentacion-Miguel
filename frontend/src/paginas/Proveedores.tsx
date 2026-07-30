import { useEffect, useState } from 'react'
import { crearProveedor, eliminarProveedor, esErrorDeSesionCaducada, listarProveedores, mensajeDeError } from '../api'
import type { Proveedor } from '../tipos'

export default function Proveedores() {
  const [proveedores, setProveedores] = useState<Proveedor[]>([])
  const [nombre, setNombre] = useState('')
  const [telefono, setTelefono] = useState('')
  const [contacto, setContacto] = useState('')
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

  const crear = async (evento: React.FormEvent) => {
    evento.preventDefault()
    setError('')
    try {
      await crearProveedor({ nombre, telefono: telefono || null, contacto: contacto || null })
      setNombre('')
      setTelefono('')
      setContacto('')
      cargar()
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  const eliminar = async (p: Proveedor) => {
    if (!confirm(`¿Eliminar el proveedor "${p.nombre}"?`)) return
    await eliminarProveedor(p.id)
    cargar()
  }

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <form onSubmit={crear} className="rounded-lg bg-white p-4 shadow lg:self-start">
        <h2 className="mb-3 text-lg font-bold">Nuevo proveedor</h2>
        {error && <p className="mb-2 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}
        <div className="grid gap-3">
          <input
            required
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            placeholder="Nombre *"
            className="rounded border p-2"
          />
          <input
            value={telefono}
            onChange={(e) => setTelefono(e.target.value)}
            placeholder="Teléfono"
            className="rounded border p-2"
          />
          <input
            value={contacto}
            onChange={(e) => setContacto(e.target.value)}
            placeholder="Persona de contacto"
            className="rounded border p-2"
          />
          <button className="rounded bg-amber-500 py-2 font-semibold text-white hover:bg-amber-600">Guardar</button>
        </div>
      </form>

      <table className="w-full rounded-lg bg-white shadow lg:col-span-2">
        <thead>
          <tr className="border-b text-left text-sm text-slate-500">
            <th className="p-3">Nombre</th>
            <th className="p-3">Teléfono</th>
            <th className="p-3">Contacto</th>
            <th className="p-3"></th>
          </tr>
        </thead>
        <tbody>
          {proveedores.map((p) => (
            <tr key={p.id} className="border-b last:border-0">
              <td className="p-3">{p.nombre}</td>
              <td className="p-3">{p.telefono ?? '—'}</td>
              <td className="p-3">{p.contacto ?? '—'}</td>
              <td className="p-3 text-right">
                <button onClick={() => eliminar(p)} className="text-red-600 hover:underline">
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
          {cargando && (
            <tr>
              <td colSpan={4} className="p-6 text-center text-slate-400">
                Cargando…
              </td>
            </tr>
          )}
          {!cargando && proveedores.length === 0 && (
            <tr>
              <td colSpan={4} className="p-6 text-center text-slate-400">
                Sin proveedores todavía.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
