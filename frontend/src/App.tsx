import { NavLink, Navigate, Route, Routes } from 'react-router-dom'
import Caja from './paginas/Caja'
import Productos from './paginas/Productos'
import Stock from './paginas/Stock'
import Proveedores from './paginas/Proveedores'
import Informes from './paginas/Informes'

const enlaces = [
  { ruta: '/caja', texto: 'Caja' },
  { ruta: '/productos', texto: 'Productos' },
  { ruta: '/stock', texto: 'Stock' },
  { ruta: '/proveedores', texto: 'Proveedores' },
  { ruta: '/informes', texto: 'Informes' },
]

export default function App() {
  return (
    <div className="min-h-screen bg-slate-100">
      <nav className="bg-slate-800 text-white print:hidden">
        <div className="mx-auto flex max-w-6xl items-center gap-1 px-4">
          <span className="mr-4 py-3 text-lg font-bold">🛒 TPV Alimentación</span>
          {enlaces.map((e) => (
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
        </div>
      </nav>
      <main className="mx-auto max-w-6xl p-4">
        <Routes>
          <Route path="/" element={<Navigate to="/caja" replace />} />
          <Route path="/caja" element={<Caja />} />
          <Route path="/productos" element={<Productos />} />
          <Route path="/stock" element={<Stock />} />
          <Route path="/proveedores" element={<Proveedores />} />
          <Route path="/informes" element={<Informes />} />
        </Routes>
      </main>
    </div>
  )
}
