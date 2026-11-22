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

## Clasificación de vehículos

`POST /api/v1/vehicles/classify` recibe una placa y devuelve su valor normalizado y el tipo calculado por el backend:

```sh
curl -X POST http://localhost:8080/api/v1/vehicles/classify \
  -H 'Content-Type: application/json' \
  -d '{"plate":" abc123 "}'
```

Respuesta HTTP 200: `{"plate":"ABC123","type":"CARRO"}`. También se reconoce `ABC12D` como `MOTO`. Una placa con formato inválido devuelve HTTP 400 con `code` igual a `INVALID_PLATE` y un `message` explicativo. No se persisten datos.

Para usar la vista de clasificación durante el desarrollo local, el backend permite solicitudes CORS únicamente desde `FRONTEND_ORIGIN` (por defecto, `http://localhost:5173`). Ajusta esta variable si Vite usa otro origen. No hay conexiones a servicios externos configuradas.
