import { useEffect, useState } from 'react'
import { NavLink, Navigate, Route, Routes } from 'react-router-dom'
import { cerrarSesion, usuarioActual } from './api'
import { AuthContexto } from './AuthContexto'
import type { Usuario } from './tipos'
import Login from './paginas/Login'
import Caja from './paginas/Caja'
import Productos from './paginas/Productos'
import Stock from './paginas/Stock'
import Proveedores from './paginas/Proveedores'
import Informes from './paginas/Informes'
import Cierre from './paginas/Cierre'

const enlaces = [
  { ruta: '/caja', texto: 'Caja', soloAdmin: false },
  { ruta: '/productos', texto: 'Productos', soloAdmin: false },
  { ruta: '/stock', texto: 'Stock', soloAdmin: false },
  { ruta: '/proveedores', texto: 'Proveedores', soloAdmin: true },
  { ruta: '/informes', texto: 'Informes', soloAdmin: true },
  { ruta: '/cierre', texto: 'Cierre', soloAdmin: true },
]

export default function App() {
  // undefined = comprobando sesión, null = sin sesión
  const [usuario, setUsuario] = useState<Usuario | null | undefined>(undefined)

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
            <span className="mr-4 py-3 text-lg font-bold">🛒 TPV Alimentación</span>
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
              <span>
                👤 {usuario.nombre} <span className="text-xs">({usuario.rol === 'ADMIN' ? 'admin' : 'cajero'})</span>
              </span>
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
            <Route path="/productos" element={<Productos />} />
            <Route path="/stock" element={<Stock />} />
            <Route path="/proveedores" element={esAdmin ? <Proveedores /> : <Navigate to="/caja" replace />} />
            <Route path="/informes" element={esAdmin ? <Informes /> : <Navigate to="/caja" replace />} />
            <Route path="/cierre" element={esAdmin ? <Cierre /> : <Navigate to="/caja" replace />} />
          </Routes>
        </main>
      </div>
    </AuthContexto.Provider>
  )
}
