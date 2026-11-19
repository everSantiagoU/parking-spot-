# Backend

API Spring Boot 3.5.7 con Java 21 y Maven. El paquete raíz es `com.parkingspot`:

- `domain`: modelos, reglas y puertos del negocio.
- `application`: casos de uso y coordinación de los puertos.
- `infrastructure`: adaptadores y configuración técnica.
- `api`: entrada HTTP y representación de respuestas.

Las carpetas de las capas están preparadas para futuras funcionalidades; aún no hay lógica de negocio ni controladores propios. El endpoint de salud lo proporciona Actuator y Springdoc genera su documentación OpenAPI.

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

El esquema OpenAPI describe `GET /actuator/health` a partir del endpoint real de Actuator. Su estado normal responde HTTP 200; con la configuración predeterminada, `DOWN` y `OUT_OF_SERVICE` responden HTTP 503. Springdoc también incluye `GET /actuator`, la ruta de enlaces de Actuator. No hay operaciones de negocio. El esquema de la respuesta de salud se mantiene genérico porque Actuator puede variar su contenido según la configuración y los indicadores disponibles.

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

No hay conexiones a servicios externos configuradas en esta actividad.
