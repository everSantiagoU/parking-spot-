# Backend

API Spring Boot 3.5.7 con Java 21 y Maven. El paquete raíz es `com.parkingspot`:

- `domain`: modelos, reglas y puertos del negocio.
- `application`: casos de uso y coordinación de los puertos.
- `infrastructure`: adaptadores y configuración técnica.
- `api`: entrada HTTP y representación de respuestas.

La clasificación de placas se implementa en dominio y aplicación, con un adaptador REST en infraestructura. El endpoint de salud lo proporciona Actuator y Springdoc genera la documentación OpenAPI.

## Ejecutar

Se requieren JDK 21, Maven y una base PostgreSQL. La forma más simple es levantar solo la base con Docker Compose (ver el README raíz) y exportar la contraseña. Desde la raíz del repositorio:

```sh
docker compose up -d db
export DB_PASSWORD=<la de POSTGRES_PASSWORD en .env>   # PowerShell: $env:DB_PASSWORD="..."
mvn -f backend/pom.xml spring-boot:run
```

Para levantar todo (base, backend y frontend) en contenedores: `docker compose up -d --build`.

Consultar las rutas disponibles:

```sh
curl http://localhost:8080/actuator/health
curl http://localhost:8080/v3/api-docs
```

Swagger UI está en `http://localhost:8080/swagger-ui/index.html` (`/swagger-ui.html` redirige allí). La respuesta de salud esperada en una aplicación sana contiene `"status":"UP"`.

El esquema OpenAPI incluye `POST /api/v1/vehicles/classify` y describe `GET /actuator/health` a partir del endpoint real de Actuator. Su estado normal responde HTTP 200; con la configuración predeterminada, `DOWN` y `OUT_OF_SERVICE` responden HTTP 503. Springdoc también incluye `GET /actuator`, la ruta de enlaces de Actuator. El esquema de la respuesta de salud se mantiene genérico porque Actuator puede variar su contenido según la configuración y los indicadores disponibles.

Para compilar sin ejecutar pruebas:

```sh
mvn -f backend/pom.xml -DskipTests compile
```

## Pruebas

```sh
mvn -f backend/pom.xml test
```

`DatabaseMigrationTest` levanta un PostgreSQL 16 real con Testcontainers y verifica las migraciones y las restricciones (HU-05). Necesita Docker Desktop encendido; si Docker no está disponible, esas pruebas se omiten y las demás se ejecutan igual.

## Configuración

`backend/.env.example` documenta las variables. Spring Boot lee las variables exportadas en el entorno del proceso; no carga el archivo `.env` por sí solo.

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Puerto HTTP, incluido el endpoint de salud. |
| `DB_URL` | `jdbc:postgresql://localhost:5432/parkingspot` | URL JDBC de PostgreSQL. En Compose apunta al servicio `db`. |
| `DB_USERNAME` | `parkingspot` | Usuario de la base de datos. |
| `DB_PASSWORD` | *(vacío)* | Contraseña de la base de datos. No tiene valor por defecto. |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | Origen permitido por CORS. |
| `TZ` | `America/Bogota` | Zona horaria usada por Hibernate. |

La persistencia usa Spring Data JPA con el driver de PostgreSQL. `ddl-auto` está en `none`: el esquema lo crean las migraciones de Flyway.

## Base de datos y migraciones (Flyway)

Las migraciones están en `src/main/resources/db/migration` y se aplican solas al arrancar el backend:

- `V1__esquema_inicial.sql`: tablas `roles`, `sedes`, `usuarios`, `franjas`, `tarifas`, `ocupacion`, `cobros`, `pagos` y `eventos`, con sus llaves foráneas, restricciones de unicidad (por ejemplo, un cobro por placa, sede y día operativo) y validaciones (`CHECK`).
- `V2__datos_iniciales.sql`: roles, sede principal, franjas, tarifas y cupos iniciales.

Nunca modifique una migración ya aplicada: cree una nueva (`V3__descripcion.sql`). El diccionario de datos está en `docs/architecture/modelo-datos.md`. `/actuator/health` muestra el componente `db`, así que responde `DOWN` (HTTP 503) si la base no está disponible.

Springdoc publica `/v3/api-docs` y Swagger UI en el mismo puerto. `springdoc.show-actuator=true` incorpora los endpoints de Actuator al esquema; la exposición HTTP de Actuator sigue limitada a `health` en `application.yml` (además de la ruta raíz de enlaces propia de Actuator). 

## Autenticación y roles (HU-06)

| Método y ruta | Acceso |
| --- | --- |
| `GET /api/v1/auth/csrf` | Público. Devuelve `headerName` y `token`. |
| `POST /api/v1/auth/login` | Público con CSRF. Recibe `username` y `password` en formato `application/x-www-form-urlencoded`. |
| `GET /api/v1/auth/me` | Sesión válida. Devuelve `{ "username": "...", "role": "OPERADOR" }` o el rol `ADMIN`. |
| `POST /api/v1/auth/logout` | Sesión válida y CSRF. Invalida la sesión y responde 204. |
| `POST /api/v1/vehicles/classify` | Solo `OPERADOR`, con CSRF. |
| `/api/v1/admin/**` | Solo `ADMIN`. Las solicitudes que modifican estado requieren CSRF. |

El inicio de sesión devuelve el mismo objeto que `/auth/me` y renueva el identificador de sesión. Spring mantiene la sesión en memoria con 30 minutos de inactividad y usa la cookie `JSESSIONID`, configurada con `HttpOnly` y `SameSite=Lax`. Para servir la API por HTTPS, activa `SESSION_COOKIE_SECURE=true`. En local, usa el mismo nombre de host para ambas aplicaciones (por ejemplo, `localhost` en ambas) y configura `FRONTEND_ORIGIN` con el origen exacto del frontend. CORS permite credenciales desde ese origen. El cliente incluye la cookie y solicita un token CSRF antes de cada operación de escritura, incluidas login y logout. Envía el token con el nombre de cabecera devuelto por el servidor (`X-CSRF-TOKEN`).

La API devuelve 401 ante credenciales inválidas o solicitudes protegidas con sesión ausente, inválida o expirada. Devuelve 403 ante permisos insuficientes o CSRF inválido en una sesión válida. El login también devuelve 403 si falta su token CSRF o es inválido.

El acceso GET local a `/actuator`, `/actuator/health`, `/swagger-ui.html`, `/swagger-ui/**` y `/v3/api-docs/**` es público. En Swagger puedes consultar la documentación sin sesión. Para ejecutar una operación protegida, la solicitud necesita la cookie de una sesión con el rol indicado y, para escrituras, el token CSRF. Puedes probar el flujo completo desde el frontend.

`POST /api/v1/vehicles/classify` recibe, por ejemplo, `{"plate":" abc123 "}` y responde `{"plate":"ABC123","type":"CARRO"}`. `ABC12D` se reconoce como `MOTO`. Un formato inválido devuelve 400 con código `INVALID_PLATE`. La clasificación sigue sin persistir datos.

### Preparar PostgreSQL y crear cuentas de prueba

Aplica primero `V1__esquema_inicial.sql` y `V2__datos_iniciales.sql` de [HU-05 (PR #23)](https://github.com/everSantiagoU/parking-spot-/pull/23). Cuando HU-05 esté integrada, Flyway las aplica durante el arranque. Para ensayar esta rama por separado, puedes extraer ambos archivos del PR y aplicarlos con `psql -v ON_ERROR_STOP=1 -f archivo.sql` en orden V1 y V2, sobre una base vacía de prueba. Usa una base descartable para esa aplicación manual, ya que `psql` no registra las versiones en Flyway.

Con `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` apuntando a la base preparada, ejecuta desde la raíz:

```sh
mvn -f backend/pom.xml -DskipTests package
java -jar backend/target/parking-spot-0.0.1-SNAPSHOT.jar \
  --spring.main.web-application-type=none --spring.profiles.active=create-account
```

El comando pide el rol, el usuario, el nombre completo y la contraseña en una terminal interactiva. La contraseña se lee sin eco ni argumentos de línea de comandos. El alta guarda el hash BCrypt y asocia a los operadores con la sede activa `Sede Principal` de HU-05. Ejecuta el comando una vez para cada rol. En HU-06 no hay cuentas predeterminadas ni un endpoint HTTP de alta.

Arranca la aplicación en modo web con `mvn -f backend/pom.xml spring-boot:run`. Desde `frontend/`, ejecuta `npm ci` y `npm run dev`. Abre `http://localhost:5173/login` y prueba cada cuenta. Como operador, clasifica una placa, recarga y visita `/admin` para comprobar la denegación. Como administrador, recarga `/admin` y verifica la restricción al visitar `/operator`. Cierra sesión desde cada panel y vuelve a abrir una ruta protegida.

### Verificación de tarifas (HU-12)

`mvn -f backend/pom.xml test` prueba `PUT /api/v1/admin/tariffs/1` con un controlador que existe exclusivamente en el contexto de pruebas. Con una sesión válida y CSRF, el operador recibe 403 y el administrador llega al controlador (204). Cuando HU-12 implemente las tarifas, ubica las operaciones administrativas bajo `/api/v1/admin/**` y verifica la misma política para cada método real de modificación, incluida la ausencia de cambios en la base cuando se rechace una solicitud.

## Cobros del día operativo (HU-14 y HU-15)

Endpoints para el rol `OPERADOR`. La sede se toma del usuario autenticado.

| Método | Ruta | Respuesta |
| --- | --- | --- |
| `POST` | `/api/v1/charges` con `{"plate":"ABC123"}` | `201` cobro nuevo (`outcome: CREADO`); `200` reingreso (`EXISTENTE`) o vehículo sin obligación de pago (`EXENTO`); `400 INVALID_PLATE`; `422 TARIFA_NO_CONFIGURADA`; `409 OPERADOR_SIN_SEDE` |
| `GET` | `/api/v1/charges/today?plate=ABC123` | `200` cobro del día operativo actual; `404 COBRO_NO_ENCONTRADO` |

Reglas implementadas:

- **Día operativo (S-02):** de 06:00 a 05:59:59 del día siguiente, en la zona `parking.time-zone` (por defecto `America/Bogota`). Un ingreso a las 03:00 pertenece al día anterior. Lo calcula `OperatingDayCalculator`.
- **Franja:** se toma de la tabla `franjas` según la hora de ingreso (`TimeSlotResolver`). La franja nocturna cruza la medianoche.
- **Patrón Strategy:** `TariffStrategy` define la regla de cobro de cada tipo de vehículo. `MotorcycleTariffStrategy` no genera obligación de pago si la tarifa es $0 (RN-01). `CarTariffStrategy` exige una tarifa mayor a $0. Un tipo nuevo solo requiere una estrategia nueva (principio abierto/cerrado).
- **Tarifa conservada (RN-07):** el cobro guarda `tarifa_aplicada` y `valor`; cambiar la tarifa después no lo altera.
- **Unicidad y concurrencia:** un solo cobro por placa, sede y día operativo. `JdbcChargeRepository` inserta con `ON CONFLICT ON CONSTRAINT uq_cobros_placa_sede_dia DO NOTHING`, así dos solicitudes simultáneas dejan un único cobro.

Pruebas: `OperatingDayCalculatorTest`, `TimeSlotResolverTest`, `TariffStrategyTest` y `ChargeServiceTest` (unitarias), y `ChargeApiTest` (integración con PostgreSQL real; necesita Docker).

## Pago en efectivo idempotente (HU-16)

Endpoints para el rol `OPERADOR`. Sin sesión responden `401` y con otro rol `403` (CA-07).

| Método | Ruta | Respuesta |
| --- | --- | --- |
| `POST` | `/api/v1/payments` con `{"chargeId":1,"amount":10000,"idempotencyKey":"<uuid>"}` | `201` pago nuevo; `200` repetición con la misma clave (`replayed: true`); `400 SOLICITUD_INVALIDA` o `IMPORTE_INCORRECTO`; `404 COBRO_NO_ENCONTRADO`; `409 COBRO_YA_PAGADO`, `COBRO_NO_PAGABLE` o `IDEMPOTENCIA_CONFLICTO` |
| `GET` | `/api/v1/payments/charge/{chargeId}` | Pago con importe, fecha, método y operador (CA-03); `404 PAGO_NO_ENCONTRADO` |

Cómo funciona (`CashPaymentService`, todo en una transacción):

1. Bloquea el cobro con `SELECT … FOR UPDATE`: un segundo pago del mismo cobro espera a que el primero termine.
2. Si la clave de idempotencia ya existe, devuelve el pago original cuando los datos coinciden, o `409 IDEMPOTENCIA_CONFLICTO` cuando no (CA-02).
3. **Patrón State** (`ChargeState`): `PENDIENTE` pasa a `PAGADO`; `PAGADO` responde `COBRO_YA_PAGADO` (CA-06); `VENCIDO` y `ANULADO` responden `COBRO_NO_PAGABLE`.
4. El importe debe ser exactamente el valor del cobro (CA-05).
5. Guarda el pago y deja el cobro `PAGADO` con `vigente_hasta` en las 06:00 del día siguiente: el pase del día operativo (CA-08, RN-03).

Las restricciones `uq_pagos_cobro` y `uq_pagos_clave_idempotencia` de la base de datos respaldan las reglas. Las transacciones se manejan con el puerto `Transactions`, así la capa de aplicación no depende de Spring.

Pruebas: `ChargeStateTest` y `CashPaymentServiceTest` (unitarias), y `PaymentApiTest` (integración con PostgreSQL real, incluidos seis pagos simultáneos).
