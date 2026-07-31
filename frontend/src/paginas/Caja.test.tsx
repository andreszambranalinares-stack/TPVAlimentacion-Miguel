import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContexto } from '../AuthContexto'
import * as api from '../api'
import type { Producto, Usuario, Venta } from '../tipos'
import Caja from './Caja'

vi.mock('../api')

const usuarioAdmin: Usuario = { nombreUsuario: 'admin', nombre: 'Administrador', rol: 'ADMIN' }

const productoAgua: Producto = {
  id: 1,
  nombre: 'Agua Mineral 50cl',
  codigoBarras: null,
  categoriaId: null,
  categoriaNombre: null,
  precioVenta: 0.5,
  precioCoste: 0.2,
  ivaPorcentaje: 21,
  stockActual: 100,
  stockMinimo: 10,
  unidadMedida: 'UNIDAD',
  activo: true,
  bajoMinimo: false,
  esPack: false,
  componentes: [],
}

const ventaFixture: Venta = {
  id: 99,
  fechaHora: new Date().toISOString(),
  total: 0.5,
  totalIva: 0.09,
  metodoPago: 'TARJETA',
  lineas: [],
  usuarioNombre: 'Administrador',
}

function renderCaja(usuario: Usuario = usuarioAdmin) {
  return render(
    <AuthContexto.Provider value={usuario}>
      <Caja />
    </AuthContexto.Provider>,
  )
}

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  vi.mocked(api.buscarProductos).mockResolvedValue([productoAgua])
  vi.mocked(api.productosBajoMinimo).mockResolvedValue([])
  vi.mocked(api.obtenerDatosTienda).mockResolvedValue({
    nombre: 'Alimentación Miguel',
    direccion: null,
    telefono: null,
    nif: null,
  })
  vi.mocked(api.esErrorDeSesionCaducada).mockReturnValue(false)
  vi.mocked(api.mensajeDeError).mockReturnValue('Error de prueba')
})

describe('búsqueda y carrito', () => {
  it('busca un producto por nombre, lo selecciona de la lista y aparece en el carrito', async () => {
    const usuario = userEvent.setup()
    renderCaja()

    const buscador = screen.getByPlaceholderText(/escanea un código de barras/i)
    await usuario.type(buscador, 'Agua')

    const opcion = await screen.findByRole('option', { name: /Agua Mineral 50cl/i })
    await usuario.click(opcion)

    const tabla = screen.getByRole('table')
    expect(within(tabla).getByText('Agua Mineral 50cl')).toBeInTheDocument()

    const totalTexto = screen.getByText('Total').closest('p')
    expect(totalTexto).toHaveTextContent('0,50 €')
  })
})

describe('cobro con teclas rápidas', () => {
  it('F2 abre el modal de cobro en efectivo', async () => {
    const usuario = userEvent.setup()
    renderCaja()

    const botonRapido = await screen.findByRole('button', { name: /Agua Mineral 50cl/i })
    await usuario.click(botonRapido)

    await usuario.keyboard('{F2}')

    expect(await screen.findByText(/el cliente entrega/i)).toBeInTheDocument()
  })

  it('F3 cobra con tarjeta', async () => {
    const usuario = userEvent.setup()
    vi.mocked(api.crearVenta).mockResolvedValue(ventaFixture)
    renderCaja()

    const botonRapido = await screen.findByRole('button', { name: /Agua Mineral 50cl/i })
    await usuario.click(botonRapido)

    await usuario.keyboard('{F3}')

    await waitFor(() => {
      expect(api.crearVenta).toHaveBeenCalledWith(expect.objectContaining({ metodoPago: 'TARJETA' }))
    })
  })
})

describe('aparcar venta', () => {
  it('aparca el carrito actual y lo recupera después', async () => {
    const usuario = userEvent.setup()
    vi.spyOn(window, 'prompt').mockReturnValue('Cliente Juan')
    renderCaja()

    const botonRapido = await screen.findByRole('button', { name: /Agua Mineral 50cl/i })
    await usuario.click(botonRapido)

    await usuario.click(screen.getByRole('button', { name: /aparcar venta/i }))

    expect(screen.getByText(/el carrito está vacío/i)).toBeInTheDocument()
    const botonLista = await screen.findByRole('button', { name: /venta aparcada/i })
    expect(botonLista).toHaveTextContent('1 venta aparcada')

    await usuario.click(botonLista)
    expect(await screen.findByText('Cliente Juan')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: /recuperar/i }))

    const tabla = screen.getByRole('table')
    expect(within(tabla).getByText('Agua Mineral 50cl')).toBeInTheDocument()
    expect(screen.queryByText(/venta aparcada/i)).not.toBeInTheDocument()
  })
})

describe('aviso de stock bajo', () => {
  it('se muestra cuando hay productos por debajo del mínimo', async () => {
    vi.mocked(api.productosBajoMinimo).mockResolvedValue([
      { ...productoAgua, id: 2, nombre: 'Leche Entera', stockActual: 1, stockMinimo: 5, bajoMinimo: true },
    ])
    renderCaja()

    expect(await screen.findByText(/1 producto con poco stock/i)).toBeInTheDocument()
  })

  it('no se muestra si no hay productos por debajo del mínimo', async () => {
    renderCaja()

    await screen.findByRole('button', { name: /Agua Mineral 50cl/i })
    expect(screen.queryByText(/con poco stock/i)).not.toBeInTheDocument()
  })
})
