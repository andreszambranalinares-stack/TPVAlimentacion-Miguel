import { useState } from 'react'
import { iniciarSesion, mensajeDeError } from '../api'
import type { Usuario } from '../tipos'

export default function Login({ alEntrar }: { alEntrar: (usuario: Usuario) => void }) {
  const [nombreUsuario, setNombreUsuario] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [entrando, setEntrando] = useState(false)

  const entrar = async (evento: React.FormEvent) => {
    evento.preventDefault()
    setError('')
    setEntrando(true)
    try {
      alEntrar(await iniciarSesion(nombreUsuario, password))
    } catch (e) {
      setError(mensajeDeError(e))
    } finally {
      setEntrando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100">
      <form onSubmit={entrar} className="w-full max-w-sm rounded-lg bg-white p-8 shadow-lg">
        <h1 className="mb-1 text-center text-2xl font-bold">🛒 TPV Alimentación</h1>
        <p className="mb-6 text-center text-sm text-slate-500">Inicia sesión para abrir la caja</p>
        {error && <p className="mb-3 rounded bg-red-100 px-3 py-2 text-sm text-red-700">{error}</p>}
        <label className="mb-3 block">
          <span className="text-sm text-slate-600">Usuario</span>
          <input
            required
            autoFocus
            value={nombreUsuario}
            onChange={(e) => setNombreUsuario(e.target.value)}
            className="w-full rounded border p-2"
            autoComplete="username"
          />
        </label>
        <label className="mb-5 block">
          <span className="text-sm text-slate-600">Contraseña</span>
          <input
            required
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full rounded border p-2"
            autoComplete="current-password"
          />
        </label>
        <button
          disabled={entrando}
          className="w-full rounded bg-amber-500 py-2 font-semibold text-white hover:bg-amber-600 disabled:opacity-50"
        >
          {entrando ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </div>
  )
}
