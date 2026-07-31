import { useEffect, useState } from 'react'
import { NavLink, Navigate, Route, Routes } from 'react-router-dom'
import { cambiarMiPassword, cerrarSesion, esErrorDeSesionCaducada, mensajeDeError, usuarioActual } from './api'
import { AuthContexto } from './AuthContexto'
import type { Usuario } from './tipos'
import Login from './paginas/Login'
import Caja from './paginas/Caja'
import Productos from './paginas/Productos'
import Stock from './paginas/Stock'
import Proveedores from './paginas/Proveedores'
import Informes from './paginas/Informes'
import Cierre from './paginas/Cierre'
import Devoluciones from './paginas/Devoluciones'
import PedidosProveedor from './paginas/PedidosProveedor'
import Usuarios from './paginas/Usuarios'

const enlaces = [
  { ruta: '/caja', texto: 'Caja', soloAdmin: false },
  { ruta: '/devoluciones', texto: 'Devoluciones', soloAdmin: false },
  { ruta: '/productos', texto: 'Productos', soloAdmin: false },
  { ruta: '/stock', texto: 'Stock', soloAdmin: false },
  { ruta: '/pedidos-proveedor', texto: 'Pedidos', soloAdmin: false },
  { ruta: '/proveedores', texto: 'Proveedores', soloAdmin: true },
  { ruta: '/informes', texto: 'Informes', soloAdmin: true },
  { ruta: '/cierre', texto: 'Cierre', soloAdmin: true },
  { ruta: '/usuarios', texto: 'Empleados', soloAdmin: true },
]

export default function App() {
  // undefined = comprobando sesión, null = sin sesión
  const [usuario, setUsuario] = useState<Usuario | null | undefined>(undefined)
  const [mostrarMiCuenta, setMostrarMiCuenta] = useState(false)
  const [passwordActual, setPasswordActual] = useState('')
  const [passwordNueva, setPasswordNueva] = useState('')
  const [errorCuenta, setErrorCuenta] = useState('')
  const [mensajeCuenta, setMensajeCuenta] = useState('')

  useEffect(() => {
    usuarioActual()
      .then(setUsuario)
      .catch(() => setUsuario(null))
    const alCaducar = () => setUsuario(null)
    window.addEventListener('sesion-caducada', alCaducar)
    return () => window.removeEventListener('sesion-caducada', alCaducar)
  }, [])

  const salir = async () => {
    await cerrarSesion().catch(() => {})
    setUsuario(null)
  }

  const guardarMiPassword = async (evento: React.FormEvent) => {
    evento.preventDefault()
    setErrorCuenta('')
    setMensajeCuenta('')
    try {
      await cambiarMiPassword(passwordActual, passwordNueva)
      setMensajeCuenta('Contraseña actualizada')
      setPasswordActual('')
      setPasswordNueva('')
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setErrorCuenta(mensajeDeError(e))
    }
  }

  if (usuario === undefined) {
    return <div className="flex min-h-screen items-center justify-center text-slate-400">Cargando…</div>
  }
  if (usuario === null) {
    return <Login alEntrar={setUsuario} />
  }

  const esAdmin = usuario.rol === 'ADMIN'
  const visibles = enlaces.filter((e) => esAdmin || !e.soloAdmin)

  return (
    <AuthContexto.Provider value={usuario}>
      <div className="min-h-screen bg-slate-100">
        <nav className="bg-slate-800 text-white print:hidden">
          <div className="mx-auto flex max-w-6xl items-center gap-1 px-4">
            <span className="mr-4 py-3 text-lg font-bold">🛒 Alimentación Miguel</span>
            {visibles.map((e) => (
              <NavLink
                key={e.ruta}
                to={e.ruta}
                className={({ isActive }) =>
                  `px-4 py-3 text-sm font-medium hover:bg-slate-700 ${isActive ? 'bg-slate-900 text-amber-300' : ''}`
                }
              >
                {e.texto}
              </NavLink>
            ))}
            <span className="ml-auto flex items-center gap-3 text-sm text-slate-300">
              <button
                onClick={() => {
                  setMostrarMiCuenta(true)
                  setErrorCuenta('')
                  setMensajeCuenta('')
                }}
                className="hover:underline"
              >
                👤 {usuario.nombre} <span className="text-xs">({usuario.rol === 'ADMIN' ? 'admin' : 'cajero'})</span>
              </button>
              <button onClick={salir} className="rounded bg-slate-700 px-3 py-1 hover:bg-slate-600">
                Salir
              </button>
            </span>
          </div>
        </nav>
        <main className="mx-auto max-w-6xl p-4">
          <Routes>
            <Route path="/" element={<Navigate to="/caja" replace />} />
            <Route path="/caja" element={<Caja />} />
            <Route path="/devoluciones" element={<Devoluciones />} />
            <Route path="/productos" element={<Productos />} />
            <Route path="/stock" element={<Stock />} />
            <Route path="/pedidos-proveedor" element={<PedidosProveedor />} />
            <Route path="/proveedores" element={esAdmin ? <Proveedores /> : <Navigate to="/caja" replace />} />
            <Route path="/informes" element={esAdmin ? <Informes /> : <Navigate to="/caja" replace />} />
            <Route path="/cierre" element={esAdmin ? <Cierre /> : <Navigate to="/caja" replace />} />
            <Route path="/usuarios" element={esAdmin ? <Usuarios /> : <Navigate to="/caja" replace />} />
          </Routes>
        </main>
      </div>

      {mostrarMiCuenta && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/50 p-4 print:hidden">
          <form onSubmit={guardarMiPassword} className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
            <h2 className="mb-4 text-xl font-bold">Cambiar mi contraseña</h2>
            {errorCuenta && <p className="mb-3 rounded bg-red-100 px-3 py-2 text-red-700">{errorCuenta}</p>}
            {mensajeCuenta && <p className="mb-3 rounded bg-green-100 px-3 py-2 text-green-700">{mensajeCuenta}</p>}
            <div className="grid gap-3">
              <label>
                <span className="text-sm text-slate-600">Contraseña actual *</span>
                <input
                  required
                  autoFocus
                  value={passwordActual}
                  onChange={(e) => setPasswordActual(e.target.value)}
                  className="w-full rounded border p-2"
                />
              </label>
              <label>
                <span className="text-sm text-slate-600">Contraseña nueva * (mínimo 6 caracteres)</span>
                <input
                  required
                  minLength={6}
                  value={passwordNueva}
                  onChange={(e) => setPasswordNueva(e.target.value)}
                  className="w-full rounded border p-2"
                />
              </label>
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setMostrarMiCuenta(false)}
                className="rounded px-4 py-2 hover:bg-slate-100"
              >
                Cerrar
              </button>
              <button type="submit" className="rounded bg-amber-500 px-4 py-2 font-semibold text-white hover:bg-amber-600">
                Guardar
              </button>
            </div>
          </form>
        </div>
      )}
    </AuthContexto.Provider>
  )
}
