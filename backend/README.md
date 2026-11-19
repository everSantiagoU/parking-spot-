# Backend

API Spring Boot 3.5.7 con Java 21 y Maven. El paquete raíz es `com.parkingspot`:

- `domain`: modelos, reglas y puertos del negocio.
- `application`: casos de uso y coordinación de los puertos.
- `infrastructure`: adaptadores y configuración técnica.
- `api`: entrada HTTP y representación de respuestas.

Las carpetas de las capas están preparadas para futuras funcionalidades; aún no hay lógica de negocio ni controladores propios. El endpoint de salud lo proporciona Actuator.

## Ejecutar

Se requieren JDK 21 y Maven. Desde la raíz del repositorio:

```sh
mvn -f backend/pom.xml spring-boot:run
```

Consultar el estado:

```sh
curl http://localhost:8080/actuator/health
```

La respuesta esperada contiene `"status":"UP"`. Para compilar sin ejecutar pruebas:

```sh
mvn -f backend/pom.xml -DskipTests compile
```

## Configuración

`backend/.env.example` documenta las variables. Spring Boot lee las variables exportadas en el entorno del proceso; no carga el archivo `.env` por sí solo.

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Puerto HTTP, incluido el endpoint de salud. |

No hay conexiones a servicios externos configuradas en esta actividad.
