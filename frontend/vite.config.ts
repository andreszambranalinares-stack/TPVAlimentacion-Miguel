import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El proxy evita problemas de CORS en desarrollo: /api → backend Spring Boot.
// host: 'localhost' (ya es el valor por defecto de Vite) deja explícito que
// no se expone al resto de la red/wifi de la tienda, solo al propio PC.
export default defineConfig({
  plugins: [react()],
  server: {
    host: 'localhost',
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
