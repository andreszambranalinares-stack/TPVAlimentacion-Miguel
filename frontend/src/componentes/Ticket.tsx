import { useEffect, useState } from 'react'
import { obtenerDatosTienda } from '../api'
import type { DatosTienda, Venta } from '../tipos'
import { euros } from '../utils'

interface Props {
  venta: Venta
  /** Efectivo entregado por el cliente, si se registró al cobrar. */
  entregado?: number | null
}

/** Ticket imprimible en formato de impresora térmica (58/80 mm). */
export default function Ticket({ venta, entregado }: Props) {
  const fecha = new Date(venta.fechaHora)
  const [datos, setDatos] = useState<DatosTienda | null>(null)

  useEffect(() => {
    obtenerDatosTienda()
      .then(setDatos)
      .catch(() => {})
  }, [])

  return (
    <div className="ticket-print mx-auto w-72 bg-white p-3 font-mono text-xs text-black">
      <div className="text-center">
        <p className="text-sm font-bold">{(datos?.nombre ?? 'ALIMENTACIÓN MIGUEL').toUpperCase()}</p>
        {datos?.direccion && <p>{datos.direccion}</p>}
        {datos?.telefono && <p>Tel: {datos.telefono}</p>}
        {datos?.nif && <p>NIF: {datos.nif}</p>}
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
              {linea.descuentoPorcentaje > 0 ? ` (−${linea.descuentoPorcentaje}%)` : ''}
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
      {entregado != null && (
        <>
          <p className="flex justify-between">
            <span>Entregado</span>
            <span>{euros(entregado)}</span>
          </p>
          <p className="flex justify-between font-bold">
            <span>Cambio</span>
            <span>{euros(entregado - venta.total)}</span>
          </p>
        </>
      )}
      <hr className="my-2 border-dashed border-black" />
      <p className="text-center">¡Gracias por su compra!</p>
    </div>
  )
}
