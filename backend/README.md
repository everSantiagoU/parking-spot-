# Backend

API Spring Boot 3.5.7 con Java 21 y Maven. El paquete raíz es `com.parkingspot`:

- `domain`: modelos, reglas y puertos del negocio.
- `application`: casos de uso y coordinación de los puertos.
- `infrastructure`: adaptadores y configuración técnica.
- `api`: entrada HTTP y representación de respuestas.

La clasificación de placas se implementa en dominio y aplicación, con un adaptador REST en infraestructura. El endpoint de salud lo proporciona Actuator y Springdoc genera la documentación OpenAPI.

## Ejecutar

Se requieren JDK 21 y Maven. Desde la raíz del repositorio:

```sh
mvn -f backend/pom.xml spring-boot:run
```

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

## Configuración

`backend/.env.example` documenta las variables. Spring Boot lee las variables exportadas en el entorno del proceso; no carga el archivo `.env` por sí solo.

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Puerto HTTP, incluido el endpoint de salud. |

Springdoc publica `/v3/api-docs` y Swagger UI en el mismo puerto. `springdoc.show-actuator=true` incorpora los endpoints de Actuator al esquema; la exposición HTTP de Actuator sigue limitada a `health` en `application.yml` (además de la ruta raíz de enlaces propia de Actuator). No se requieren variables de entorno adicionales para la documentación.

## Clasificación de vehículos

`POST /api/v1/vehicles/classify` recibe una placa y devuelve su valor normalizado y el tipo calculado por el backend:

```sh
curl -X POST http://localhost:8080/api/v1/vehicles/classify \
  -H 'Content-Type: application/json' \
  -d '{"plate":" abc123 "}'
```

Respuesta HTTP 200: `{"plate":"ABC123","type":"CARRO"}`. También se reconoce `ABC12D` como `MOTO`. Una placa con formato inválido devuelve HTTP 400 con `code` igual a `INVALID_PLATE` y un `message` explicativo. No se persisten datos.

Para usar la vista de clasificación durante el desarrollo local, el backend permite solicitudes CORS únicamente desde `FRONTEND_ORIGIN` (por defecto, `http://localhost:5173`). Ajusta esta variable si Vite usa otro origen. No hay conexiones a servicios externos configuradas.

## Lector de placas simulado

`POST /api/v1/plate-readings/simulate` reemplaza la cámara durante el desarrollo. Recibe la placa y la confianza (entre 0 y 1, ambos incluidos) que enviaría un lector, normaliza y clasifica la placa con las mismas reglas de la clasificación de vehículos y responde con la fuente `SIMULADOR`:

```sh
curl -X POST http://localhost:8080/api/v1/plate-readings/simulate \
  -H 'Content-Type: application/json' \
  -d '{"plate":" abc12d ","confidence":0.93}'
```

Respuesta HTTP 200: `{"plate":"ABC12D","vehicleType":"MOTO","confidence":0.93,"source":"SIMULADOR"}`. El endpoint no aplica umbral de confianza ni registra el ingreso; una confianza baja como `0.2` también responde 200.

Los errores responden HTTP 400 con el formato `{"code":"...","message":"..."}`:

| `code` | Causa |
| --- | --- |
| `INVALID_CONFIDENCE` | La confianza falta, es nula o está fuera de [0, 1]. |
| `INVALID_PLATE` | La placa falta o no tiene el formato `ABC123` (carro) o `ABC12D` (moto). |
| `INVALID_REQUEST` | El cuerpo no es un JSON válido o un campo tiene un tipo incorrecto. |

El lector es un puerto de salida (`domain/port/out/PlateReaderPort`) implementado por `SimulatedPlateReader` en `infrastructure/adapter/out/plate`. Para usar otro lector basta con cambiar el bean `plateReader` en `PlateReaderConfig`, sin modificar el dominio ni la aplicación. No se persisten datos.
