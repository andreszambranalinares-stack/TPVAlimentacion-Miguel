import { describe, expect, it, vi } from 'vitest'
import { euros, normalizarTexto, pareceCodigoBarras, parsearProductosCsv, seleccionarAlFoco } from './utils'

describe('euros', () => {
  it('formatea con coma decimal y símbolo €', () => {
    expect(euros(5)).toBe('5,00 €')
    expect(euros(1234.5)).toBe('1234,50 €')
  })

  it('admite negativos (p. ej. cambio a devolver)', () => {
    expect(euros(-3.2)).toBe('-3,20 €')
  })
})

describe('pareceCodigoBarras', () => {
  it('reconoce cadenas de 6 o más dígitos', () => {
    expect(pareceCodigoBarras('8412345678901')).toBe(true)
    expect(pareceCodigoBarras('123456')).toBe(true)
  })

  it('rechaza menos de 6 dígitos', () => {
    expect(pareceCodigoBarras('12345')).toBe(false)
  })

  it('rechaza texto con letras', () => {
    expect(pareceCodigoBarras('ABC123456')).toBe(false)
    expect(pareceCodigoBarras('Manzana')).toBe(false)
  })

  it('ignora espacios al principio y al final', () => {
    expect(pareceCodigoBarras('  8412345678901  ')).toBe(true)
  })

  it('rechaza cadena vacía', () => {
    expect(pareceCodigoBarras('')).toBe(false)
  })
})

describe('normalizarTexto', () => {
  it('quita acentos y pasa a minúsculas', () => {
    expect(normalizarTexto('Categoría')).toBe('categoria')
    expect(normalizarTexto('CÓDIGO')).toBe('codigo')
  })

  it('deja intacto el texto que ya está normalizado', () => {
    expect(normalizarTexto('lacteos')).toBe('lacteos')
  })
})

describe('seleccionarAlFoco', () => {
  it('llama a select() del input recibido', () => {
    const select = vi.fn()
    const evento = { target: { select } } as unknown as Parameters<typeof seleccionarAlFoco>[0]
    seleccionarAlFoco(evento)
    expect(select).toHaveBeenCalledOnce()
  })
})

describe('parsearProductosCsv', () => {
  it('parsea una fila válida con todas las columnas', () => {
    const csv = [
      'nombre,codigoBarras,categoria,precioVenta,precioCoste,ivaPorcentaje,unidadMedida,stockInicial,stockMinimo',
      'Manzana Fuji,8412345678901,Frutas,0.99,0.40,21,KG,20,5',
    ].join('\n')

    const { productos, invalidas } = parsearProductosCsv(csv)

    expect(invalidas).toEqual([])
    expect(productos).toHaveLength(1)
    expect(productos[0]).toMatchObject({
      nombre: 'Manzana Fuji',
      codigoBarras: '8412345678901',
      categoriaNombre: 'Frutas',
      precioVenta: 0.99,
      precioCoste: 0.4,
      ivaPorcentaje: 21,
      unidadMedida: 'KG',
      stockInicial: 20,
      stockMinimo: 5,
    })
  })

  it('reconoce cabeceras con distintos alias y mayúsculas', () => {
    const csv = ['Producto;PVP;Unidad', 'Leche;1.25;Litro'].join('\n')

    const { productos, invalidas } = parsearProductosCsv(csv)

    expect(invalidas).toEqual([])
    expect(productos[0]).toMatchObject({ nombre: 'Leche', precioVenta: 1.25, unidadMedida: 'LITRO' })
  })

  it('respeta las comas dentro de valores entre comillas', () => {
    const csv = ['nombre,precioVenta', '"Lentejas, paquete 1kg",1.99'].join('\n')

    const { productos } = parsearProductosCsv(csv)

    expect(productos[0].nombre).toBe('Lentejas, paquete 1kg')
    expect(productos[0].precioVenta).toBe(1.99)
  })

  it.each([
    ['kg', 'KG'],
    ['Kilo', 'KG'],
    ['KILOS', 'KG'],
    ['l', 'LITRO'],
    ['Litro', 'LITRO'],
    ['ud', 'UNIDAD'],
    ['', 'UNIDAD'],
    ['algo-desconocido', 'UNIDAD'],
  ])('traduce la unidad "%s" a %s', (entrada, esperado) => {
    const csv = ['nombre,precioVenta,unidadMedida', `Producto,1,${entrada}`].join('\n')
    const { productos } = parsearProductosCsv(csv)
    expect(productos[0].unidadMedida).toBe(esperado)
  })

  it('usa 21% de IVA por defecto si no se indica', () => {
    const csv = ['nombre,precioVenta', 'Producto,1'].join('\n')
    const { productos } = parsearProductosCsv(csv)
    expect(productos[0].ivaPorcentaje).toBe(21)
  })

  it('marca como inválida una fila sin nombre, con el número de fila correcto', () => {
    const csv = ['nombre,precioVenta', 'Producto A,1', ',2', 'Producto C,3'].join('\n')

    const { productos, invalidas } = parsearProductosCsv(csv)

    expect(productos).toHaveLength(2)
    expect(invalidas).toEqual([{ fila: 3, motivo: 'Falta el nombre' }])
  })

  it('marca como inválida una fila con precio no numérico', () => {
    const csv = ['nombre,precioVenta', 'Producto,no-es-un-numero'].join('\n')

    const { productos, invalidas } = parsearProductosCsv(csv)

    expect(productos).toHaveLength(0)
    expect(invalidas).toEqual([{ fila: 2, motivo: 'Precio de venta inválido ("no-es-un-numero")' }])
  })

  it('devuelve listas vacías para un CSV vacío', () => {
    expect(parsearProductosCsv('')).toEqual({ productos: [], invalidas: [] })
  })

  it('devuelve listas vacías para un CSV que solo tiene cabecera', () => {
    expect(parsearProductosCsv('nombre,precioVenta')).toEqual({ productos: [], invalidas: [] })
  })
})
