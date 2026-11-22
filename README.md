# Parking Spot

Monorepo del sistema académico Parking Spot. El repositorio contiene un monolito modular con backend Java/Spring Boot, frontend React/TypeScript y base de datos PostgreSQL, orquestados con Docker Compose.

## Estructura

- `backend/`: aplicación Spring Boot organizada por capas hexagonales.
- `frontend/`: aplicación React con Vite.
- `docker-compose.yml`: entorno local completo (base de datos, backend y frontend).
- `infrastructure/`: espacio reservado para artefactos de infraestructura.
- `docs/`: documentación académica y técnica.
- `scripts/`: automatizaciones locales (`verificar-entorno.sh`).

Consulta los README de `backend/` y `frontend/` para trabajar en cada parte por separado.

## Arranque desde cero con Docker Compose

El MVP se ejecuta completo en el equipo local, sin servicios en la nube.

### Requisitos

- Docker Desktop (Windows o macOS) o Docker Engine con el plugin Compose v2 (Linux).
- Git.
- Puertos libres: `5432` (PostgreSQL), `8080` (backend) y `5173` (frontend). Se pueden cambiar en `.env`.

### Pasos

1. Clonar el repositorio y entrar a la carpeta:

   ```sh
   git clone https://github.com/everSantiagoU/parking-spot-.git
   cd parking-spot-
   ```

2. Crear el archivo de variables a partir de la plantilla y **cambiar la contraseña**:

   ```sh
   cp .env.example .env        # En PowerShell: Copy-Item .env.example .env
   ```

   `.env` está en `.gitignore`: las credenciales nunca se suben al repositorio. Si falta `POSTGRES_DB`, `POSTGRES_USER` o `POSTGRES_PASSWORD`, Compose se detiene con un mensaje que indica cuál falta.

3. Construir y levantar los servicios:

   ```sh
   docker compose up -d --build
   ```

   La primera vez tarda unos minutos porque descarga las imágenes y compila el backend y el frontend. El backend espera a que PostgreSQL esté sano (`healthcheck`) antes de arrancar.

4. Verificar:

   ```sh
   docker compose ps
   sh scripts/verificar-entorno.sh
   ```

   | Servicio | URL |
   | --- | --- |
   | Frontend | http://localhost:5173 |
   | API (salud) | http://localhost:8080/actuator/health |
   | Swagger UI | http://localhost:8080/swagger-ui/index.html |
   | PostgreSQL | `localhost:5432` (usuario y contraseña de `.env`) |

   La respuesta de salud debe mostrar `"status":"UP"` y el componente `"db"` también en `UP`.

### Variables de entorno (`.env`)

| Variable | Valor de ejemplo | Uso |
| --- | --- | --- |
| `POSTGRES_DB` | `parkingspot` | Nombre de la base de datos. |
| `POSTGRES_USER` | `parkingspot` | Usuario de PostgreSQL. |
| `POSTGRES_PASSWORD` | *(definir)* | Contraseña de PostgreSQL. El backend la recibe como `DB_PASSWORD`. |
| `DB_PORT` | `5432` | Puerto publicado de PostgreSQL en el equipo. |
| `SERVER_PORT` | `8080` | Puerto publicado del backend. |
| `FRONTEND_PORT` | `5173` | Puerto publicado del frontend. |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | Origen permitido por CORS en el backend. |
| `VITE_API_BASE_URL` | `http://localhost:8080` | URL del backend que usa el navegador. Se fija al construir la imagen del frontend. |
| `TZ` | `America/Bogota` | Zona horaria de los contenedores. El día operativo (06:00–05:59) depende de ella. |

### Persistencia de datos

Los datos de PostgreSQL viven en el volumen con nombre `parking-spot_parking-spot-data`, así que sobreviven a reinicios y a `docker compose down`.

```sh
docker compose down          # Detiene los servicios y conserva los datos
docker compose up -d         # Los datos siguen ahí
docker compose down -v       # ⚠️ Borra también el volumen (base de datos vacía)
```

Para comprobarlo, se puede crear una tabla de prueba, reiniciar y consultarla:

```sh
docker compose exec db psql -U parkingspot -d parkingspot -c "CREATE TABLE prueba(id int); INSERT INTO prueba VALUES (1);"
docker compose down && docker compose up -d
docker compose exec db psql -U parkingspot -d parkingspot -c "SELECT * FROM prueba;"
docker compose exec db psql -U parkingspot -d parkingspot -c "DROP TABLE prueba;"
```

### Comandos útiles

```sh
docker compose logs -f backend               # Ver logs del backend
docker compose up -d --build backend         # Recompilar solo el backend tras un cambio
docker compose exec db psql -U parkingspot -d parkingspot   # Consola SQL
```

### Desarrollo sin contenedores para backend o frontend

Se puede levantar solo la base de datos y ejecutar el backend y el frontend desde el IDE:

```sh
docker compose up -d db
mvn -f backend/pom.xml spring-boot:run      # Exportar antes DB_PASSWORD (ver backend/.env.example)
cd frontend && npm install && npm run dev
```

### Problemas comunes

- **`port is already allocated`**: otro programa usa el puerto. Cambie `DB_PORT`, `SERVER_PORT` o `FRONTEND_PORT` en `.env`. Si cambia `SERVER_PORT`, actualice también `VITE_API_BASE_URL`; si cambia `FRONTEND_PORT`, actualice `FRONTEND_ORIGIN`.
- **El backend se reinicia con `password authentication failed`**: la contraseña de `.env` cambió después de crear el volumen. PostgreSQL solo usa las credenciales al crear la base: vuelva a la contraseña anterior o borre el volumen con `docker compose down -v`.
- **El frontend no conecta con la API**: revise `VITE_API_BASE_URL` y reconstruya con `docker compose up -d --build frontend`.
