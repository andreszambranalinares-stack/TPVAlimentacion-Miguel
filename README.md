# Alimentación Miguel — TPV e inventario

Punto de venta (TPV) e inventario para Alimentación Miguel, una tienda de alimentación de barrio: gestión de productos con código de barras, pantalla de caja rápida (teclado + lector), control de stock con trazabilidad e informes.

## Estructura del monorepo

```
├── backend/    Java 21 + Spring Boot 3 (Web, Data JPA, Validation, Flyway, springdoc)
└── frontend/   React 18 + Vite + TypeScript + Tailwind CSS
```

## Instalación en el ordenador de la tienda

El PC de la tienda solo necesita **dos cosas instaladas**: **Java 21+** y
**PostgreSQL**. No hace falta Node: la pantalla va compilada dentro del propio
programa.

1. Instala [Java 21 (Temurin)](https://adoptium.net) y
   [PostgreSQL](https://www.postgresql.org/download/) (apunta la contraseña que
   pongas para el usuario `postgres`, la pedirá el paso siguiente).
2. Ejecuta **`preparar-pc`** (`.bat` en Windows, `.command` en macOS). Una sola
   vez. Comprueba Java, crea la base de datos, compila el programa en `tpv.jar`
   y deja los accesos directos en el escritorio.
3. A partir de ahí, el día a día es solo `arrancar-tpv` y `parar-tpv`.

## Uso en la tienda

Scripts pensados para no tener que tocar la terminal (`.bat` en Windows,
`.command` en macOS):

| Script | Qué hace |
| --- | --- |
| `preparar-pc` (una sola vez) | Comprueba Java y PostgreSQL, crea la base de datos, compila `tpv.jar` y crea los accesos directos. Da instrucciones concretas de qué descargar si falta algo. |
| `arrancar-tpv` | Arranca PostgreSQL y el programa, espera a que responda y abre el navegador. Tarda unos segundos. |
| `parar-tpv` | Cierra el programa y hace una copia de seguridad automática antes de terminar (no hace falta acordarse). |
| `copia-seguridad` | Copia de seguridad manual: vuelca la base de datos a `copias/tpv_<fecha>.sql` y descarta las de más de 60 días. Útil antes de un cambio importante. |
| `copia-seguridad-automatica` | Versión silenciosa (sin ventanas ni esperas) que usa `parar-tpv` internamente. No hace falta ejecutarla a mano. |
| `crear-accesos-directos.bat` (solo Windows) | Crea los accesos directos del escritorio con el icono de la app. Ya lo llama `preparar-pc`; solo hace falta a mano si se borran. |

Cada arranque y cierre, y cada copia de seguridad, queda anotado en
`.registros/actividad.log` (con fecha y hora) para poder revisar qué pasó si
algún día algo falla.

El resto de este documento describe el flujo de desarrollo.

## Antes de abrir la tienda con esta app (checklist)

1. **Cambia las contraseñas por defecto** (`admin/admin123`, `caja/caja123`,
   ver más abajo): desde "Empleados" en la app, con el usuario admin.
2. El motor solo escucha en el propio ordenador (`server.address: 127.0.0.1`
   en `application.yml`), no en el resto de la red/wifi de la tienda — no
   hace falta tocar nada, ya viene así de fábrica.
3. Sube el catálogo real de productos (pantalla "Productos" → "Importar
   CSV") y confirma que no queda ningún producto de prueba.
4. Prueba el lector de código de barras y la impresora de tickets con el
   hardware real de la tienda antes del primer día — todavía no se ha
   probado con hardware físico, solo en navegador.
5. Llévate las copias de seguridad fuera del ordenador de vez en cuando (un
   pendrive o la nube): una copia en el mismo disco no sirve de nada si es el
   disco el que falla.

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
npm run test                 # tests con Vitest + React Testing Library
```

## Empaquetado para la tienda (un solo programa)

En desarrollo, backend y frontend van por separado (`:8080` y `:5173`). Para la
tienda se compila todo en **un único jar**, con la pantalla ya construida dentro
en `/static`:

```bash
cd backend
./mvnw -Pcompleto clean package -DskipTests
java -jar target/tpv-backend-0.1.0-SNAPSHOT.jar   # todo en http://localhost:8080
```

El perfil `completo` usa `frontend-maven-plugin`, que **se descarga su propia
copia de Node dentro de `target/`** solo para compilar: el ordenador que ejecuta
el jar no necesita Node instalado, y el que lo compila tampoco. Es un perfil
aparte a propósito — si se activara siempre, cada `mvn test` tendría que
compilar el frontend entero.

`PantallaConfig` sirve esos ficheros y redirige las rutas de React Router
(`/caja`, `/productos`…) a `index.html`, dejando fuera `/api/**` para que una
dirección de API inexistente siga devolviendo un 404 de verdad y no la página
web con un engañoso 200 OK.

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
