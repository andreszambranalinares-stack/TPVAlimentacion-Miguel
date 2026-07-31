import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// No usamos `globals: true` (para no tocar tsconfig.json), así que Testing
// Library no registra la limpieza automática del DOM entre tests: hay que
// hacerlo a mano o cada test dejaría montado el componente del anterior.
afterEach(() => {
  cleanup()
})
