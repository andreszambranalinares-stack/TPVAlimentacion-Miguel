# TPV Alimentación

Punto de venta (TPV) e inventario para una tienda de alimentación de barrio: gestión de productos con código de barras, pantalla de caja rápida (teclado + lector), control de stock con trazabilidad e informes.

## Estructura del monorepo

```
├── backend/    Java 21 + Spring Boot 3 (Web, Data JPA, Validation, Flyway, springdoc)
└── frontend/   React 18 + Vite + TypeScript + Tailwind CSS
```

## Requisitos

- Java 21 y Maven 3.9+
- PostgreSQL 14+ (solo para desarrollo; los tests usan H2 en memoria)
- Node 20+

## Base de datos (desarrollo)

Crear la base de datos y el usuario que espera el perfil `dev`:

```sql
CREATE USER tpv WITH PASSWORD 'tpv';
CREATE DATABASE tpv OWNER tpv;
```

Con Docker es más rápido:

```bash
docker run -d --name tpv-postgres -p 5432:5432 \
  -e POSTGRES_USER=tpv -e POSTGRES_PASSWORD=tpv -e POSTGRES_DB=tpv \
  postgres:16
```

Las migraciones Flyway (`backend/src/main/resources/db/migration`) se aplican solas al arrancar.

## Backend

```bash
cd backend
mvn spring-boot:run          # arranca con el perfil dev (Postgres local)
mvn test                     # tests con perfil test (H2 en memoria)
```

- API REST bajo `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Frontend

```bash
cd frontend
npm install
npm run dev                  # abre http://localhost:5173 (proxy /api → :8080)
npm run build                # compila TypeScript y genera dist/
```

### Pantalla de caja (uso con teclado y lector)

- El foco vive siempre en el buscador: el lector de códigos de barras "teclea"
  el código y su Enter final añade el producto al carrito.
- Escribiendo texto busca por nombre; flechas ↑/↓ para elegir y **Enter** añade.
- **F2** cobra en efectivo, **F3** con tarjeta, **F4** vacía el carrito.
- Al finalizar la venta se muestra el ticket (formato térmico 58/80 mm) listo
  para imprimir; **Enter** inicia la venta siguiente.

## Perfiles

| Perfil | Base de datos | Uso |
|--------|---------------|-----|
| `dev`  | PostgreSQL local (`jdbc:postgresql://localhost:5432/tpv`) | desarrollo (activo por defecto) |
| `test` | H2 en memoria en modo PostgreSQL | tests automáticos |

## Estado del proyecto

- [x] Fase 1 — Monorepo, configuración, esquema Flyway, entidades JPA y repositorios
- [x] Fase 2 — Servicios y controladores de Productos y Categorías + Swagger + tests
- [x] Fase 3 — Lógica de ventas (validación y descuento de stock, movimientos)
- [x] Fase 4 — Frontend: layout + gestión de productos
- [x] Fase 5 — Frontend: pantalla de caja (teclado/escáner)
- [x] Fase 6 — Informes, control de stock y ticket imprimible
