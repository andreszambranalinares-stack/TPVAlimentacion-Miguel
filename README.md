# Alimentación Miguel — TPV e inventario

Punto de venta (TPV) e inventario para Alimentación Miguel, una tienda de alimentación de barrio: gestión de productos con código de barras, pantalla de caja rápida (teclado + lector), control de stock con trazabilidad e informes.

## Estructura del monorepo

```
├── backend/    Java 21 + Spring Boot 3 (Web, Data JPA, Validation, Flyway, springdoc)
└── frontend/   React 18 + Vite + TypeScript + Tailwind CSS
```

## Uso en la tienda

Para el ordenador de la tienda hay scripts que evitan tener que usar la terminal
(`.bat` en Windows, `.command` en macOS):

| Script | Qué hace |
| --- | --- |
| `arrancar-tpv` | Arranca PostgreSQL, backend y frontend, y abre el navegador. |
| `parar-tpv` | Cierra backend y frontend. |
| `copia-seguridad` | Vuelca la base de datos a `copias/tpv_<fecha>.sql` y descarta las de más de 60 días. |

El resto de esta sección describe el flujo de desarrollo.

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

## Usuarios y roles

La API exige sesión (login en `/api/auth/login`). Flyway siembra dos usuarios
(**cámbialos en producción**; los hashes son BCrypt en la tabla `usuario`):

| Usuario | Contraseña | Rol    | Puede |
|---------|------------|--------|-------|
| `admin` | `admin123` | ADMIN  | todo: gestión de catálogo y proveedores, informes, cierre de caja, ajustes manuales de stock |
| `caja`  | `caja123`  | CAJERO | cobrar en caja, consultar productos, registrar entradas y mermas de stock |

El login se bloquea 15 minutos tras 5 intentos fallidos seguidos con el mismo
usuario (protección simple en memoria contra fuerza bruta).

## Fiabilidad y seguridad

- **Bloqueo optimista de stock** (`Producto.version`): si dos ventas o
  movimientos concurrentes intentan tocar el mismo producto, el segundo en
  confirmar recibe un 409 `CONFLICTO_CONCURRENCIA` en vez de sobrescribir
  silenciosamente el stock (evita vender más unidades de las que quedan).
- **AJUSTE de stock reservado a ADMIN**: ENTRADA (recepción de pedidos) y
  MERMA (roturas/caducados) siguen abiertos a cualquier usuario; el ajuste
  manual de un recuento de inventario requiere rol ADMIN.
- **Rango de fechas limitado a 366 días** en `/api/ventas` y `/api/informes/*`
  para evitar consultas sin límite sobre años de histórico.

## Cierre de caja y exportación

- **Cierre de caja** (`/api/cierres-caja`, pestaña «Cierre»): al final del día se
  introduce el efectivo contado en el cajón y se guarda el arqueo con la
  diferencia frente a lo esperado según las ventas. Solo un cierre por fecha.
- **Exportación CSV** (botones en «Informes»): `/api/informes/ventas.csv` y
  `/api/informes/inventario.csv`, listos para abrir en Excel (separador `;`,
  decimales con coma, UTF-8 con BOM).

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
| `prod` | (combinar con `dev` o un perfil de datos propio) | desactiva Swagger/OpenAPI y exige cookies solo por HTTPS; actívalo con `SPRING_PROFILES_ACTIVE=prod,dev` |

## Estado del proyecto

- [x] Fase 1 — Monorepo, configuración, esquema Flyway, entidades JPA y repositorios
- [x] Fase 2 — Servicios y controladores de Productos y Categorías + Swagger + tests
- [x] Fase 3 — Lógica de ventas (validación y descuento de stock, movimientos)
- [x] Fase 4 — Frontend: layout + gestión de productos
- [x] Fase 5 — Frontend: pantalla de caja (teclado/escáner)
- [x] Fase 6 — Informes, control de stock y ticket imprimible
- [x] Mejoras — Autenticación con roles (ADMIN/CAJERO), cierre de caja diario y exportación CSV
- [x] Revisión — Bloqueo optimista de stock, permisos de AJUSTE, límite de intentos de login, validaciones y correcciones de UX en caja
