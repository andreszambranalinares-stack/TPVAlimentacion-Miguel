import type { FocusEvent } from 'react'
import type { ProductoEntrada, UnidadMedida } from './tipos'

export const euros = (n: number) => n.toFixed(2).replace('.', ',') + ' €'

export const hoy = () => new Date().toISOString().slice(0, 10)

export const pareceCodigoBarras = (texto: string) => /^\d{6,}$/.test(texto.trim())

/**
 * Para inputs numéricos: selecciona todo el contenido al enfocar, para que
 * el primer dígito que se teclee sustituya al 0 (u otro valor) en vez de
 * añadirse detrás.
 */
export const seleccionarAlFoco = (e: FocusEvent<HTMLInputElement>) => e.target.select()

/** Cabeceras aceptadas por columna, en minúsculas y sin acentos. */
const CABECERAS: Record<string, string[]> = {
  nombre: ['nombre', 'producto', 'descripcion'],
  codigoBarras: ['codigobarras', 'codigo', 'codigodebarras', 'ean'],
  categoria: ['categoria'],
  precioVenta: ['preciodeventa', 'precioventa', 'pvp', 'precio'],
  precioCoste: ['preciodecoste', 'preciocoste', 'coste'],
  ivaPorcentaje: ['iva', 'ivaporcentaje'],
  unidadMedida: ['unidaddemedida', 'unidadmedida', 'unidad'],
  stockInicial: ['stockinicial', 'stock'],
  stockMinimo: ['stockminimo', 'minimo'],
}

const UNIDADES: Record<string, UnidadMedida> = {
  unidad: 'UNIDAD',
  ud: 'UNIDAD',
  uds: 'UNIDAD',
  kg: 'KG',
  kilo: 'KG',
  kilos: 'KG',
  l: 'LITRO',
  litro: 'LITRO',
  litros: 'LITRO',
}

/** Minúsculas y sin acentos, para comparar textos de forma tolerante (p. ej. nombres de categoría). */
export const normalizarTexto = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()

/** Parsea una línea CSV respetando comillas dobles (permite comas dentro de "..."). */
function parsearLinea(linea: string): string[] {
  const campos: string[] = []
  let actual = ''
  let entreComillas = false
  for (let i = 0; i < linea.length; i++) {
    const c = linea[i]
    if (entreComillas) {
      if (c === '"' && linea[i + 1] === '"') {
        actual += '"'
        i++
      } else if (c === '"') {
        entreComillas = false
      } else {
        actual += c
      }
    } else if (c === '"') {
      entreComillas = true
    } else if (c === ',' || c === ';') {
      campos.push(actual)
      actual = ''
    } else {
      actual += c
    }
  }
  campos.push(actual)
  return campos.map((c) => c.trim())
}

export interface FilaCsvInvalida {
  fila: number
  motivo: string
}

export interface ProductosCsv {
  productos: (ProductoEntrada & { categoriaNombre: string | null })[]
  invalidas: FilaCsvInvalida[]
}

/**
 * Convierte el texto de un CSV de productos en entradas listas para crear.
 * Cabeceras admitidas (cualquier orden, mayúsculas/minúsculas indiferente):
 * nombre, codigoBarras, categoria, precioVenta, precioCoste, ivaPorcentaje,
 * unidadMedida, stockInicial, stockMinimo. Solo nombre y precioVenta son obligatorios.
 */
export function parsearProductosCsv(texto: string): ProductosCsv {
  const lineas = texto.split(/\r\n|\n|\r/).filter((l) => l.trim() !== '')
  if (lineas.length === 0) return { productos: [], invalidas: [] }

  const cabecera = parsearLinea(lineas[0]).map((h) => normalizarTexto(h).replace(/[^a-z]/g, ''))
  const indices: Partial<Record<keyof typeof CABECERAS, number>> = {}
  for (const [campo, alias] of Object.entries(CABECERAS)) {
    const idx = cabecera.findIndex((h) => alias.includes(h))
    if (idx !== -1) indices[campo as keyof typeof CABECERAS] = idx
  }

  const productos: (ProductoEntrada & { categoriaNombre: string | null })[] = []
  const invalidas: FilaCsvInvalida[] = []

  for (let i = 1; i < lineas.length; i++) {
    const numeroFila = i + 1
    const valores = parsearLinea(lineas[i])
    const leer = (campo: keyof typeof CABECERAS) => {
      const idx = indices[campo]
      return idx === undefined ? '' : (valores[idx] ?? '').trim()
    }

    const nombre = leer('nombre')
    const precioVentaTexto = leer('precioVenta').replace(',', '.')
    if (!nombre) {
      invalidas.push({ fila: numeroFila, motivo: 'Falta el nombre' })
      continue
    }
    const precioVenta = Number(precioVentaTexto)
    if (!precioVentaTexto || Number.isNaN(precioVenta)) {
      invalidas.push({ fila: numeroFila, motivo: `Precio de venta inválido ("${leer('precioVenta')}")` })
      continue
    }

    const unidadTexto = normalizarTexto(leer('unidadMedida'))
    const unidadMedida = UNIDADES[unidadTexto] ?? 'UNIDAD'

    const precioCosteTexto = leer('precioCoste').replace(',', '.')
    const ivaTexto = leer('ivaPorcentaje').replace(',', '.')
    const stockInicialTexto = leer('stockInicial').replace(',', '.')
    const stockMinimoTexto = leer('stockMinimo').replace(',', '.')

    productos.push({
      nombre,
      codigoBarras: leer('codigoBarras') || null,
      categoriaId: null,
      categoriaNombre: leer('categoria') || null,
      precioVenta,
      precioCoste: precioCosteTexto ? Number(precioCosteTexto) : null,
      ivaPorcentaje: ivaTexto ? Number(ivaTexto) : 21,
      stockMinimo: stockMinimoTexto ? Number(stockMinimoTexto) : null,
      stockInicial: stockInicialTexto ? Number(stockInicialTexto) : null,
      unidadMedida,
      activo: true,
      esPack: false,
      componentes: [],
    })
  }

  return { productos, invalidas }
}
