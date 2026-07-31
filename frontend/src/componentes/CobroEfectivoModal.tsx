import { useEffect, useMemo, useRef, useState } from 'react'
import { euros } from '../utils'

interface Props {
  total: number
  cobrando: boolean
  onConfirmar: (entregado: number | null) => void
  onCancelar: () => void
}

/** Billetes y monedas con los que un cliente suele pagar. */
const DENOMINACIONES = [5, 10, 20, 50, 100]

/** Acepta tanto "12,50" (como se teclea en España) como "12.50". */
const aNumero = (texto: string) => Number(texto.replace(',', '.'))

/**
 * Cuadro de cobro en efectivo: pide lo que entrega el cliente y calcula el
 * cambio antes de cerrar la venta. Se puede confirmar sin escribir nada
 * (equivale a que ha pagado el importe justo).
 */
export default function CobroEfectivoModal({ total, cobrando, onConfirmar, onCancelar }: Props) {
  const [texto, setTexto] = useState('')
  const entradaRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    entradaRef.current?.focus()
  }, [])

  const entregado = texto.trim() === '' ? null : aNumero(texto)
  const entregadoValido = entregado === null || (!Number.isNaN(entregado) && entregado > 0)
  const cambio = entregado === null ? 0 : entregado - total
  const faltaDinero = entregado !== null && !Number.isNaN(entregado) && cambio < 0

  // Solo tiene sentido ofrecer billetes que cubran el total.
  const sugerencias = useMemo(() => DENOMINACIONES.filter((d) => d > total).slice(0, 4), [total])

  const confirmar = () => {
    if (cobrando || !entregadoValido || faltaDinero) return
    onConfirmar(entregado)
  }

  const alPulsarTecla = (evento: React.KeyboardEvent) => {
    if (evento.key === 'Enter') {
      evento.preventDefault()
      confirmar()
    } else if (evento.key === 'Escape') {
      evento.preventDefault()
      onCancelar()
    }
  }

  return (
    <div
      className="fixed inset-0 z-30 flex items-center justify-center bg-black/50 p-4"
      onKeyDown={alPulsarTecla}
      role="dialog"
      aria-modal="true"
      aria-label="Cobro en efectivo"
    >
      <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
        <p className="flex items-baseline justify-between">
          <span className="text-sm text-slate-500">A pagar</span>
          <span className="text-3xl font-bold">{euros(total)}</span>
        </p>

        <label className="mt-5 block">
          <span className="text-sm text-slate-600">El cliente entrega</span>
          <input
            ref={entradaRef}
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            inputMode="decimal"
            placeholder="Déjalo vacío si paga justo"
            aria-label="Importe entregado por el cliente"
            className="mt-1 w-full rounded border p-3 text-right text-2xl font-semibold focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-200"
          />
        </label>

        <div className="mt-2 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setTexto(total.toFixed(2).replace('.', ','))}
            className="rounded bg-slate-200 px-3 py-1.5 text-sm font-semibold hover:bg-slate-300"
          >
            Justo
          </button>
          {sugerencias.map((d) => (
            <button
              key={d}
              type="button"
              onClick={() => setTexto(String(d))}
              className="rounded bg-slate-200 px-3 py-1.5 text-sm font-semibold hover:bg-slate-300"
            >
              {d} €
            </button>
          ))}
        </div>

        <div
          className={`mt-5 rounded-lg p-4 ${faltaDinero ? 'bg-red-50' : 'bg-green-50'}`}
          role="status"
          aria-live="polite"
        >
          {faltaDinero ? (
            <p className="flex items-baseline justify-between text-red-700">
              <span className="text-sm font-semibold">Faltan</span>
              <span className="text-2xl font-bold">{euros(Math.abs(cambio))}</span>
            </p>
          ) : (
            <p className="flex items-baseline justify-between text-green-800">
              <span className="text-sm font-semibold">Cambio a devolver</span>
              <span className="text-3xl font-bold">{euros(cambio)}</span>
            </p>
          )}
        </div>

        <div className="mt-5 grid grid-cols-2 gap-2">
          <button
            type="button"
            onClick={onCancelar}
            className="rounded-lg bg-slate-200 py-3 font-semibold hover:bg-slate-300"
          >
            Cancelar <kbd className="ml-1 rounded bg-slate-400 px-1.5 text-xs text-white">Esc</kbd>
          </button>
          <button
            type="button"
            onClick={confirmar}
            disabled={cobrando || !entregadoValido || faltaDinero}
            className="rounded-lg bg-green-600 py-3 font-bold text-white hover:bg-green-700 disabled:opacity-40"
          >
            Cobrar <kbd className="ml-1 rounded bg-green-800 px-1.5 text-xs">Enter</kbd>
          </button>
        </div>
      </div>
    </div>
  )
}
