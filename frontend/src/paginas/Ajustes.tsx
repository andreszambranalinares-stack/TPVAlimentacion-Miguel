import { useEffect, useState } from 'react'
import { actualizarDatosTienda, esErrorDeSesionCaducada, mensajeDeError, obtenerDatosTienda } from '../api'
import type { DatosTiendaEntrada } from '../tipos'

const vacio: DatosTiendaEntrada = { nombre: '', direccion: null, telefono: null, nif: null }

/** Datos fiscales/de contacto de la tienda, que se imprimen en el ticket. */
export default function Ajustes() {
  const [datos, setDatos] = useState<DatosTiendaEntrada>(vacio)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')

  useEffect(() => {
    obtenerDatosTienda()
      .then(setDatos)
      .catch((e) => {
        if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
      })
      .finally(() => setCargando(false))
  }, [])

  const guardar = async (evento: React.FormEvent) => {
    evento.preventDefault()
    setError('')
    setMensaje('')
    setGuardando(true)
    try {
      const guardados = await actualizarDatosTienda(datos)
      setDatos(guardados)
      setMensaje('Datos guardados. Se usarán en el próximo ticket que se imprima.')
    } catch (e) {
      if (!esErrorDeSesionCaducada(e)) setError(mensajeDeError(e))
    } finally {
      setGuardando(false)
    }
  }

  const campo = 'w-full rounded border p-2'

  if (cargando) {
    return <p className="text-slate-400">Cargando…</p>
  }

  return (
    <div className="max-w-lg">
      <h1 className="mb-4 text-xl font-bold">Datos de la tienda</h1>
      <p className="mb-4 text-sm text-slate-600">
        Estos datos se imprimen en la cabecera de cada ticket. La dirección, el teléfono y el NIF son opcionales.
      </p>
      <form onSubmit={guardar} className="grid gap-3 rounded-lg bg-white p-4 shadow">
        {error && <p className="rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}
        {mensaje && <p className="rounded bg-green-100 px-3 py-2 text-green-700">{mensaje}</p>}
        <label>
          <span className="text-sm text-slate-600">Nombre de la tienda *</span>
          <input
            required
            value={datos.nombre}
            onChange={(e) => setDatos({ ...datos, nombre: e.target.value })}
            className={campo}
          />
        </label>
        <label>
          <span className="text-sm text-slate-600">Dirección</span>
          <input
            value={datos.direccion ?? ''}
            onChange={(e) => setDatos({ ...datos, direccion: e.target.value || null })}
            placeholder="Calle, número, ciudad"
            className={campo}
          />
        </label>
        <label>
          <span className="text-sm text-slate-600">Teléfono</span>
          <input
            value={datos.telefono ?? ''}
            onChange={(e) => setDatos({ ...datos, telefono: e.target.value || null })}
            className={campo}
          />
        </label>
        <label>
          <span className="text-sm text-slate-600">NIF / CIF</span>
          <input
            value={datos.nif ?? ''}
            onChange={(e) => setDatos({ ...datos, nif: e.target.value || null })}
            className={campo}
          />
        </label>
        <button
          disabled={guardando}
          className="mt-2 rounded bg-amber-500 py-2 font-semibold text-white hover:bg-amber-600 disabled:opacity-50"
        >
          {guardando ? 'Guardando…' : 'Guardar'}
        </button>
      </form>
    </div>
  )
}
