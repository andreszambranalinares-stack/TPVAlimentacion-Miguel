import type { Venta } from '../tipos'
import { euros } from '../utils'

/** Ticket imprimible en formato de impresora térmica (58/80 mm). */
export default function Ticket({ venta }: { venta: Venta }) {
  const fecha = new Date(venta.fechaHora)
  return (
    <div className="ticket-print mx-auto w-72 bg-white p-3 font-mono text-xs text-black">
      <div className="text-center">
        <p className="text-sm font-bold">ALIMENTACIÓN MIGUEL</p>
        <p>
          {fecha.toLocaleDateString('es-ES')} {fecha.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })}
        </p>
        <p>Ticket nº {venta.id}</p>
      </div>
      <hr className="my-2 border-dashed border-black" />
      {venta.lineas.map((linea, i) => (
        <div key={i} className="mb-1">
          <p>{linea.productoNombre}</p>
          <p className="flex justify-between">
            <span>
              {linea.cantidad} x {euros(linea.precioUnitario)}
            </span>
            <span>{euros(linea.subtotal)}</span>
          </p>
        </div>
      ))}
      <hr className="my-2 border-dashed border-black" />
      <p className="flex justify-between text-sm font-bold">
        <span>TOTAL</span>
        <span>{euros(venta.total)}</span>
      </p>
      <p className="flex justify-between">
        <span>IVA incluido</span>
        <span>{euros(venta.totalIva)}</span>
      </p>
      <p className="flex justify-between">
        <span>Pago</span>
        <span>{venta.metodoPago === 'EFECTIVO' ? 'Efectivo' : 'Tarjeta'}</span>
      </p>
      <hr className="my-2 border-dashed border-black" />
      <p className="text-center">¡Gracias por su compra!</p>
    </div>
  )
}
