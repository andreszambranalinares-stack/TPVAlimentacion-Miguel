import Ticket from './Ticket'
import type { Venta } from '../tipos'

interface Props {
  venta: Venta
  onCerrar: () => void
  textoBotonCerrar?: string
  colorBotonCerrar?: 'amber' | 'slate'
}

/** Modal con el ticket imprimible, reutilizado desde la Caja y desde Informes. */
export default function TicketModal({
  venta,
  onCerrar,
  textoBotonCerrar = 'Cerrar',
  colorBotonCerrar = 'amber',
}: Props) {
  const claseCerrar =
    colorBotonCerrar === 'amber'
      ? 'bg-amber-500 text-white hover:bg-amber-600'
      : 'bg-slate-200 hover:bg-slate-300'

  return (
    <div className="fixed inset-0 z-20 flex items-center justify-center bg-black/50 print:bg-transparent">
      <div className="max-h-[90vh] overflow-auto rounded-lg bg-white p-4 shadow-xl">
        <Ticket venta={venta} />
        <div className="mt-4 flex gap-2 print:hidden">
          <button
            onClick={() => window.print()}
            className="flex-1 rounded bg-slate-800 py-2 font-semibold text-white hover:bg-slate-700"
          >
            Imprimir
          </button>
          <button onClick={onCerrar} className={`flex-1 rounded py-2 font-semibold ${claseCerrar}`}>
            {textoBotonCerrar}
          </button>
        </div>
      </div>
    </div>
  )
}
