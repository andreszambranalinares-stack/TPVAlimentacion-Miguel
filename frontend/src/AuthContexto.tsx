import { createContext, useContext } from 'react'
import type { Usuario } from './tipos'

/** Usuario de la sesión, disponible en toda la aplicación. */
export const AuthContexto = createContext<Usuario | null>(null)

export const useUsuario = () => useContext(AuthContexto)

export const useEsAdmin = () => useContext(AuthContexto)?.rol === 'ADMIN'
