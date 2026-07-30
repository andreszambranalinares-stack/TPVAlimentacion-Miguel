export const euros = (n: number) => n.toFixed(2).replace('.', ',') + ' €'

export const hoy = () => new Date().toISOString().slice(0, 10)
