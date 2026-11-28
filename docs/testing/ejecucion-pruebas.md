# Cómo ejecutar las pruebas desde cero (HU-23, CA-08)

Esta guía sirve para que cualquier integrante, con el repositorio recién clonado, ejecute las suites del backend y del frontend y obtenga los mismos resultados que el [informe de pruebas](informe-pruebas.md).

## 1. Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| JDK | 21 | Compilar y probar el backend. `JAVA_HOME` debe apuntar al JDK 21 y `java -version` debe decir 21. |
| Maven | 3.9 o superior | Ejecutar `mvn verify`. Sirve el Maven que trae IntelliJ IDEA (`plugins/maven-plugin/lib/maven3/bin`). |
| Node.js y npm | Node 20.19+ o 22.12+ (probado con 24.20) y npm 10+ | Ejecutar las pruebas del frontend con Vitest. |
| Docker | Docker Desktop o Docker Engine encendido (probado con Engine 29.5.3) | Solo para las suites con Testcontainers (sección 3). |

No hace falta crear una base de datos ni configurar variables de entorno: cada suite levanta su propia base (H2 en memoria o un PostgreSQL 16 desechable).

## 2. Backend

Desde la raíz del repositorio:

```sh
mvn -f backend/pom.xml verify
```

`verify` compila, ejecuta todas las pruebas y falla si alguna falla. Los reportes quedan en `backend/target/surefire-reports/` (un `.txt` y un `TEST-*.xml` por clase). El reporte de cobertura de JaCoCo queda en `backend/target/site/jacoco/index.html`; es solo informativo y no tiene umbral. Para repetir desde cero, usa `mvn -f backend/pom.xml clean verify`.

Para ejecutar una sola clase o un solo método:

```sh
mvn -f backend/pom.xml test -Dtest=AcademicScenariosTest
mvn -f backend/pom.xml test -Dtest='ParkingConfigurationApiTest#rechazaNocturnaMayorOIgual'
```

En Windows, si `java` en el `PATH` no es la versión 21, define `JAVA_HOME` solo para esa terminal antes de ejecutar Maven (PowerShell):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Amazon Corretto\jdk21.0.12_9'   # ruta de tu JDK 21
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## 3. Qué suites necesitan Docker

| Grupo | Necesita Docker | Clases | Pruebas | Base de datos |
|---|---|---|---|---|
| Unitarias (dominio y aplicación) | No | 23 | 127 | Ninguna |
| Integración sin Docker | No | 10 | 129 | H2 en modo PostgreSQL (9 clases) o ninguna (`PlateReadingControllerTest`, `@WebMvcTest`) |
| **Integración con Testcontainers** | **Sí** | **9** | **87** | **PostgreSQL 16 real, con las migraciones Flyway V1-V4** |
| **Total backend** | | **42** | **343** | |

Las 9 clases que necesitan Docker son `AcademicScenariosTest`, `AdministrationApiTest`, `ChargeApiTest`, `DatabaseMigrationTest`, `ExitApiTest`, `HistoryApiTest`, `ParkingConfigurationApiTest`, `PassApiTest` y `PaymentApiTest`.

**Si Docker está apagado**, esas suites **se omiten** (`@Testcontainers(disabledWithoutDocker = true)`) y el build sigue en verde. Como Maven cuenta cada método parametrizado omitido como una sola prueba, el resumen dice `Tests run: 336, …, Skipped: 80`, no 87. Se ejecutan las 256 pruebas sin Docker y se omiten 80 métodos, que equivalen a 87 casos. Para la evidencia completa hay que **encender Docker antes** de `mvn verify` y comprobar que el resumen final diga `Tests run: 343` y `Skipped: 0`.

Comprobación rápida de Docker:

```sh
docker version --format '{{.Server.Version}}'
```

Si responde con un número de versión, está listo. La primera ejecución descarga la imagen `postgres:16-alpine` (unos 100 MB). Testcontainers 1.21.4 (definido en `backend/pom.xml`) es compatible con Docker Engine 29.

## 4. Frontend

Desde `frontend/`:

```sh
npm ci
npm test
```

`npm test` ejecuta `vitest run`: 12 archivos y 73 pruebas en `src/tests/`, con jsdom y un backend simulado (no necesita el backend ni Docker). Para verificar además que el código y las pruebas compilan con TypeScript, usa `npm run build`.

Para obtener el reporte en JSON, como el que se usó para el catálogo:

```sh
npx vitest run --reporter=json --outputFile=vitest-results.json
```

**Problema conocido en Windows:** si la terminal abre la carpeta con la letra de unidad en minúscula (`c:\...` en lugar de `C:\...`, como pasa a veces en VS Code o Git Bash), Vitest puede fallar en **todos** los archivos con `Vitest failed to find the runner`. No es un fallo de las pruebas: entra a la carpeta con la letra en mayúscula (`cd C:\Users\...\frontend`) y vuelve a ejecutar `npm test`.

## 5. Integración continua

El workflow `.github/workflows/ci.yml` (HU-24) ejecuta `mvn -B -ntp verify` y las pruebas del frontend en cada Pull Request hacia `develop` o `main`. El runner `ubuntu-latest` trae Docker, así que en CI corren también las 87 pruebas con Testcontainers. Los reportes de surefire quedan como artefacto `backend-test-reports` y el de cobertura como `backend-coverage-report`.

## 6. Resultado esperado

| Suite | Comando | Resultado esperado |
|---|---|---|
| Backend con Docker | `mvn -f backend/pom.xml clean verify` | `Tests run: 343, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS` |
| Backend sin Docker | igual | `Tests run: 336, Failures: 0, Errors: 0, Skipped: 80` y `BUILD SUCCESS` (las suites con Testcontainers se omiten) |
| Frontend | `npm test` (en `frontend/`) | `Test Files 12 passed (12)` y `Tests 73 passed (73)` |
