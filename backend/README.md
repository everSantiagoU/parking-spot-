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

El esquema OpenAPI incluye `POST /api/v1/vehicles/classify`, `POST /api/v1/plate-readings/simulate` y describe `GET /actuator/health` a partir del endpoint real de Actuator. Su estado normal responde HTTP 200; con la configuración predeterminada, `DOWN` y `OUT_OF_SERVICE` responden HTTP 503. Springdoc también incluye `GET /actuator`, la ruta de enlaces de Actuator. El esquema de la respuesta de salud se mantiene genérico porque Actuator puede variar su contenido según la configuración y los indicadores disponibles.

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
| `PLATE_CONFIDENCE_THRESHOLD` | `0.80` | Umbral de confianza (0 a 1) de la HU-11. Una lectura automática con confianza menor requiere validación del operador. |

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
| `POST /api/v1/plate-readings/simulate` | Solo `OPERADOR`, con CSRF. |
| `POST /api/v1/entries/automatic` | Solo `OPERADOR`, con CSRF. |
| `POST /api/v1/entries/manual` | Solo `OPERADOR`, con CSRF. |
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

## Lector de placas simulado (HU-08)

`POST /api/v1/plate-readings/simulate` reemplaza la cámara durante el desarrollo. Recibe la placa y la confianza (entre 0 y 1, ambos incluidos) que enviaría un lector, normaliza y clasifica la placa con las mismas reglas de la clasificación de vehículos y responde con la fuente `SIMULADOR`. Solo lo puede usar el rol `OPERADOR`, con sesión y token CSRF, igual que `POST /api/v1/vehicles/classify`.

Con `curl` (Bash), usando una cuenta de operador creada como se indica arriba. El token CSRF se pide de nuevo después del login porque el inicio de sesión lo renueva:

```sh
J=cookies.txt; API=http://localhost:8080/api/v1
csrf() { curl -s -c $J -b $J $API/auth/csrf | sed -E 's/.*"token":"([^"]+)".*/\1/'; }
curl -s -c $J -b $J -H "X-CSRF-TOKEN: $(csrf)" -d 'username=<operador>&password=<clave>' $API/auth/login
curl -s -b $J -H "X-CSRF-TOKEN: $(csrf)" -H 'Content-Type: application/json' \
  -d '{"plate":" abc12d ","confidence":0.93}' $API/plate-readings/simulate
```

Respuesta HTTP 200: `{"plate":"ABC12D","vehicleType":"MOTO","confidence":0.93,"source":"SIMULADOR"}`. El endpoint no aplica umbral de confianza ni registra el ingreso; una confianza baja como `0.2` también responde 200.

| HTTP | `code` | Causa |
| --- | --- | --- |
| 400 | `INVALID_CONFIDENCE` | La confianza falta, es nula o está fuera de [0, 1]. |
| 400 | `INVALID_PLATE` | La placa falta o no tiene el formato `ABC123` (carro) o `ABC12D` (moto). |
| 400 | `INVALID_REQUEST` | El cuerpo no es un JSON válido o un campo tiene un tipo incorrecto. |
| 401 | — | No hay sesión o la sesión es inválida o expiró. |
| 403 | `ACCESS_DENIED` | La sesión no tiene el rol `OPERADOR` o falta el token CSRF. |

Los errores 400 usan el formato `{"code":"...","message":"..."}`. El lector es un puerto de salida (`domain/port/out/PlateReaderPort`) implementado por `SimulatedPlateReader` en `infrastructure/adapter/out/plate`. Para usar otro lector basta con cambiar el bean `plateReader` en `PlateReaderConfig`, sin modificar el dominio ni la aplicación. No se persisten datos.

## Validar la confianza y lectura manual (HU-11)

Ambas rutas son solo para `OPERADOR`, con sesión y token CSRF. La sede y el usuario salen del operador autenticado (`usuarios.sede_id`), nunca del cuerpo.

- `POST /api/v1/entries/automatic` recibe la lectura del lector, `{"plate":"ABC123","confidence":0.93}`. Si el formato es inválido (`motivo` `FORMATO_INVALIDO`, aunque la confianza sea alta) o la confianza es menor que `PLATE_CONFIDENCE_THRESHOLD` (`motivo` `CONFIANZA_BAJA`), responde `resultado` `REQUIERE_VALIDACION` y no registra la entrada. Una confianza igual o mayor al umbral responde `ACEPTADA`.
- `POST /api/v1/entries/manual` recibe la placa que el operador digita o confirma, `{"plate":"ABC123"}`. La lectura queda con `source` `MANUAL`, sin confianza y con el usuario del operador (`operador`).

En el dominio, solo una `AcceptedReading` puede continuar al ingreso, y únicamente se crea con una lectura automática que alcanza el umbral o con una confirmación manual. Si el operador está inactivo o no tiene sede, la API responde 403 con `code` `OPERATOR_NOT_AVAILABLE`.

## Registrar eventos de ingreso (HU-10)

Cuando la lectura se acepta (automática sobre el umbral o confirmada manualmente), las mismas rutas registran el ingreso y responden **201** con `resultado` `AUTORIZADO`, `barreraAbierta` `true`, `eventoId`, `sedeId` y `ocurridoEn`. Una lectura que requiere validación responde **200** con `resultado` `REQUIERE_VALIDACION`, `barreraAbierta` `false` y no guarda nada.

En una sola transacción se inserta la fila en `eventos` y se suma 1 a `ocupacion.ocupados` de la sede y el tipo de vehículo:

| Columna de `eventos` | Valor |
| --- | --- |
| `sede_id`, `usuario_id` | Sede y usuario del operador autenticado. |
| `placa`, `tipo_vehiculo` | Placa normalizada y tipo calculado (HU-09). |
| `tipo_evento` | `INGRESO`. |
| `origen_lectura` | `AUTOMATICA` para el simulador (`SIMULADOR` en la API) y `MANUAL` para la placa digitada. |
| `confianza` | Confianza de 0 a 1 multiplicada por 100 (`0.93` → `93.00`); `NULL` si es manual. |
| `resultado`, `motivo` | `AUTORIZADO` y `NULL`. |
| `ocurrido_en` | Hora del servidor (`java.time.Clock`), nunca de la petición. |

Si la sede no tiene una fila en `ocupacion` para ese tipo de vehículo, la API responde 409 con `code` `OCCUPANCY_NOT_CONFIGURED` y no guarda nada.

## Validar cupos antes de abrir la entrada (HU-13)

Antes de registrar un ingreso aceptado, el backend valida el cupo del tipo de vehículo en la sede del operador con los datos de `ocupacion` (`capacidad`, que administra la HU-12, y `ocupados`). Sin cupo, las rutas de ingreso responden **409** con `resultado` `LLENO`, `barreraAbierta` `false` y el `eventoId` del intento, que queda guardado en `eventos` con `resultado` `RECHAZADO` y `motivo` `LLENO`. Los carros llenos no bloquean a las motos, ni al revés.

La verificación y el incremento son una sola sentencia atómica, dentro de la misma transacción que guarda el evento:

```sql
UPDATE ocupacion SET ocupados = ocupados + 1
WHERE sede_id = ? AND tipo_vehiculo = ? AND ocupados < capacidad;
-- 1 fila → AUTORIZADO; 0 filas → LLENO
```

La base de datos bloquea la fila mientras la actualiza. Si dos ingresos compiten por el último cupo, el segundo espera y evalúa `ocupados < capacidad` sobre el valor ya confirmado, así que afecta 0 filas. El `CHECK (ocupados <= capacidad)` de la HU-05 es una segunda barrera. `ConcurrentEntryTest` lanza 20 ingresos simultáneos contra 5 cupos y comprueba que siempre se autorizan exactamente 5.
