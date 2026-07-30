# TPV Alimentación

Punto de venta (TPV) e inventario para una tienda de alimentación de barrio: gestión de productos con código de barras, pantalla de caja rápida (teclado + lector), control de stock con trazabilidad e informes.

## Estructura del monorepo

```
├── backend/    Java 21 + Spring Boot 3 (Web, Data JPA, Validation, Flyway, springdoc)
└── frontend/   React 18 + Vite + TypeScript + Tailwind CSS  (pendiente, fase 4)
```

## Requisitos

- Java 21 y Maven 3.9+
- PostgreSQL 14+ (solo para desarrollo; los tests usan H2 en memoria)
- Node 20+ (para el frontend, cuando exista)

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

## Perfiles

| Perfil | Base de datos | Uso |
|--------|---------------|-----|
| `dev`  | PostgreSQL local (`jdbc:postgresql://localhost:5432/tpv`) | desarrollo (activo por defecto) |
| `test` | H2 en memoria en modo PostgreSQL | tests automáticos |

## Estado del proyecto

- [x] Fase 1 — Monorepo, configuración, esquema Flyway, entidades JPA y repositorios
- [x] Fase 2 — Servicios y controladores de Productos y Categorías + Swagger + tests
- [ ] Fase 3 — Lógica de ventas (validación y descuento de stock, movimientos)
- [ ] Fase 4 — Frontend: layout + gestión de productos
- [ ] Fase 5 — Frontend: pantalla de caja (teclado/escáner)
- [ ] Fase 6 — Informes, control de stock y ticket imprimible
