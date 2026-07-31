export const euros = (n: number) => n.toFixed(2).replace('.', ',') + ' €'

export const hoy = () => new Date().toISOString().slice(0, 10)

export const pareceCodigoBarras = (texto: string) => /^\d{6,}$/.test(texto.trim())
