import { useEffect, useState } from 'react'
import {
  actualizarUsuario,
  cambiarPasswordUsuario,
  crearUsuario,
  esErrorDeSesionCaducada,
  listarUsuarios,
  mensajeDeError,
} from '../api'
import type { RolUsuario, UsuarioAdmin, UsuarioEntrada } from '../tipos'

const altaVacia: UsuarioEntrada = { nombreUsuario: '', password: '', nombre: '', rol: 'CAJERO' }

/** Gestión de empleados: alta con su propio acceso, rol, baja y reinicio de contraseña. */
export default function Usuarios() {
  const [usuarios, setUsuarios] = useState<UsuarioAdmin[]>([])
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [cargando, setCargando] = useState(true)

  const [mostrarAlta, setMostrarAlta] = useState(false)
  const [alta, setAlta] = useState<UsuarioEntrada>(altaVacia)

  const [passwordNuevaPara, setPasswordNuevaPara] = useState<UsuarioAdmin | null>(null)
  const [passwordNueva, setPasswordNueva] = useState('')

  const cargar = () => {
    setCargando(true)
    listarUsuarios()
      .then(setUsuarios)
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
      await crearUsuario(alta)
      setMostrarAlta(false)
      setAlta(altaVacia)
      setMensaje(`Empleado "${alta.nombreUsuario}" dado de alta`)
      cargar()
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  const cambiarRol = async (u: UsuarioAdmin, rol: RolUsuario) => {
    setError('')
    try {
      await actualizarUsuario(u.id, { nombre: u.nombre, rol, activo: u.activo })
      cargar()
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  const cambiarActivo = async (u: UsuarioAdmin, activo: boolean) => {
    if (!activo && !confirm(`¿Desactivar el acceso de "${u.nombre}"? Ya no podrá iniciar sesión.`)) return
    setError('')
    try {
      await actualizarUsuario(u.id, { nombre: u.nombre, rol: u.rol, activo })
      cargar()
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  const guardarNuevaPassword = async (evento: React.FormEvent) => {
    evento.preventDefault()
    if (!passwordNuevaPara) return
    setError('')
    try {
      await cambiarPasswordUsuario(passwordNuevaPara.id, passwordNueva)
      setMensaje(`Contraseña de "${passwordNuevaPara.nombre}" actualizada`)
      setPasswordNuevaPara(null)
      setPasswordNueva('')
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    }
  }

  const campo = 'w-full rounded border p-2'

  return (
    <div>
      <div className="mb-4 flex items-center">
        <h1 className="text-xl font-bold">Empleados</h1>
        <button
          onClick={() => {
            setMostrarAlta(true)
            setAlta(altaVacia)
            setError('')
          }}
          className="ml-auto rounded-lg bg-amber-500 px-4 py-2 font-semibold text-white hover:bg-amber-600"
        >
          + Nuevo empleado
        </button>
      </div>

      {error && !mostrarAlta && !passwordNuevaPara && (
        <p className="mb-2 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>
      )}
      {mensaje && <p className="mb-2 rounded bg-green-100 px-3 py-2 text-green-700">{mensaje}</p>}

      <table className="w-full rounded-lg bg-white shadow">
        <thead>
          <tr className="border-b text-left text-sm text-slate-500">
            <th className="p-3">Usuario</th>
            <th className="p-3">Nombre</th>
            <th className="p-3">Rol</th>
            <th className="p-3">Estado</th>
            <th className="p-3"></th>
          </tr>
        </thead>
        <tbody>
          {usuarios.map((u) => (
            <tr key={u.id} className={`border-b last:border-0 ${!u.activo ? 'text-slate-400' : ''}`}>
              <td className="p-3 font-mono text-sm">{u.nombreUsuario}</td>
              <td className="p-3">{u.nombre}</td>
              <td className="p-3">
                <select
                  value={u.rol}
                  onChange={(e) => cambiarRol(u, e.target.value as RolUsuario)}
                  className="rounded border p-1 text-sm"
                >
                  <option value="CAJERO">Cajero</option>
                  <option value="ADMIN">Administrador</option>
                </select>
              </td>
              <td className="p-3">
                {u.activo ? (
                  <span className="rounded bg-green-100 px-2 py-0.5 text-xs font-semibold text-green-700">Activo</span>
                ) : (
                  <span className="rounded bg-slate-200 px-2 py-0.5 text-xs font-semibold">Desactivado</span>
                )}
              </td>
              <td className="p-3 text-right">
                <button
                  onClick={() => {
                    setPasswordNuevaPara(u)
                    setPasswordNueva('')
                    setError('')
                  }}
                  className="mr-3 text-blue-600 hover:underline"
                >
                  Nueva contraseña
                </button>
                <button onClick={() => cambiarActivo(u, !u.activo)} className="text-red-600 hover:underline">
                  {u.activo ? 'Desactivar' : 'Reactivar'}
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
          {!cargando && usuarios.length === 0 && (
            <tr>
              <td colSpan={5} className="p-6 text-center text-slate-400">
                Todavía no hay empleados dados de alta.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {mostrarAlta && (
        <div className="fixed inset-0 z-20 flex items-center justify-center bg-black/50 p-4">
          <form onSubmit={crear} className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
            <h2 className="mb-4 text-xl font-bold">Nuevo empleado</h2>
            {error && <p className="mb-3 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}
            <div className="grid gap-3">
              <label>
                <span className="text-sm text-slate-600">Nombre completo *</span>
                <input
                  required
                  autoFocus
                  value={alta.nombre}
                  onChange={(e) => setAlta({ ...alta, nombre: e.target.value })}
                  className={campo}
                />
              </label>
              <label>
                <span className="text-sm text-slate-600">Usuario de acceso *</span>
                <input
                  required
                  value={alta.nombreUsuario}
                  onChange={(e) => setAlta({ ...alta, nombreUsuario: e.target.value })}
                  className={campo}
                />
              </label>
              <label>
                <span className="text-sm text-slate-600">Contraseña inicial * (mínimo 6 caracteres)</span>
                <input
                  required
                  minLength={6}
                  type="text"
                  value={alta.password}
                  onChange={(e) => setAlta({ ...alta, password: e.target.value })}
                  className={campo}
                />
              </label>
              <label>
                <span className="text-sm text-slate-600">Rol</span>
                <select
                  value={alta.rol}
                  onChange={(e) => setAlta({ ...alta, rol: e.target.value as RolUsuario })}
                  className={campo}
                >
                  <option value="CAJERO">Cajero</option>
                  <option value="ADMIN">Administrador</option>
                </select>
              </label>
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <button type="button" onClick={() => setMostrarAlta(false)} className="rounded px-4 py-2 hover:bg-slate-100">
                Cancelar
              </button>
              <button type="submit" className="rounded bg-amber-500 px-4 py-2 font-semibold text-white hover:bg-amber-600">
                Guardar
              </button>
            </div>
          </form>
        </div>
      )}

      {passwordNuevaPara && (
        <div className="fixed inset-0 z-20 flex items-center justify-center bg-black/50 p-4">
          <form onSubmit={guardarNuevaPassword} className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
            <h2 className="mb-4 text-xl font-bold">Nueva contraseña para {passwordNuevaPara.nombre}</h2>
            {error && <p className="mb-3 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}
            <label>
              <span className="text-sm text-slate-600">Contraseña nueva * (mínimo 6 caracteres)</span>
              <input
                required
                autoFocus
                minLength={6}
                value={passwordNueva}
                onChange={(e) => setPasswordNueva(e.target.value)}
                className={campo}
              />
            </label>
            <div className="mt-5 flex justify-end gap-2">
              <button type="button" onClick={() => setPasswordNuevaPara(null)} className="rounded px-4 py-2 hover:bg-slate-100">
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
