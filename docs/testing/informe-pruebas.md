# Informe de pruebas · Parking Spot (HU-23)

| Dato | Valor |
|---|---|
| Fecha de ejecución | 27/11/2026 |
| Rama | `feature/HU-23-pruebas-unitarias-integracion` (desde `develop` en `dd13827`) |
| Entorno | Windows 11, JDK 21.0.12 (Corretto), Maven 3.9.16, Node 24.20 / npm 11.19, Docker Engine 29.5.3, PostgreSQL 16 (Testcontainers 1.21.4) |
| Comandos | `mvn -f backend/pom.xml clean verify` (Docker encendido) y `npm test` en `frontend/` ([cómo ejecutar](ejecucion-pruebas.md)) |
| Evidencia | `backend/target/surefire-reports/`, reporte JSON de Vitest y el [catálogo de resultados](catalogo-resultados.md), con los 416 casos y su estado |

## 1. Resumen

| Suite | Tipo | Necesita Docker | Clases o archivos | Pruebas | Aprobadas | Fallidas | Omitidas |
|---|---|---|---|---|---|---|---|
| Backend · unitarias | Dominio y aplicación, sin Spring ni base de datos (CA-01) | No | 23 | 127 | 127 | 0 | 0 |
| Backend · integración sin Docker | API con MockMvc y seguridad real, H2 en modo PostgreSQL (CA-02, CA-03) | No | 10 | 129 | 129 | 0 | 0 |
| Backend · integración con Testcontainers | API y persistencia sobre PostgreSQL 16 con Flyway V1-V4 (CA-02, CA-03, CA-06) | **Sí** | 9 | 87 | 87 | 0 | 0 |
| **Backend total** | | | **42** | **343** | **343** | **0** | **0** |
| Frontend | Páginas y clientes HTTP con Vitest + jsdom (CA-04) | No | 12 | 73 | 73 | 0 | 0 |
| **Total** | | | **54** | **416** | **416** | **0** | **0** |

Salida de Maven: `Tests run: 343, Failures: 0, Errors: 0, Skipped: 0` · `BUILD SUCCESS` · `Total time: 01:18 min`.
Salida de Vitest: `Test Files 12 passed (12)` · `Tests 73 passed (73)`.

### Aporte de la HU-23

| | Antes | Después | Nuevas |
|---|---|---|---|
| Backend | 236 (173 sin Docker + 63 con Testcontainers) | 343 | **107** en 5 clases: `AcademicScenariosTest` (7), `ParkingConfigurationApiTest` (17), `SecurityMatrixTest` (47), `PlatformApiTest` (28) y `TimeSlotScheduleTest` (8) |
| Frontend | 54 en 8 archivos | 73 en 12 archivos | **19** en 4 archivos: `LoginPage` (6), `ParkingConfigurationPage` (6), `ClassifyVehiclePage` (2) y `EntryLanePage` (5). Además, el soporte `tests/support/testApp.tsx` |

No se modificó código de producción ni dependencias. Tampoco se tocaron los 19 archivos del PR #50 (HU-22).

## 2. Casos de prueba

Tipo: ✔ = escenario positivo (éxito); ✘ = escenario negativo (error o rechazo), según el CA-05. Los identificadores `CP-HU-xx` son los de cada issue; los demás se nombran `HU-xx-CAyy`. La columna **Obtenido** proviene de los reportes de la corrida del 27/11/2026.

| ID | HU | CA | Tipo | Descripción | Resultado esperado | Obtenido | Estado | Evidencia |
|---|---|---|---|---|---|---|---|---|
| HU-02-CA02 | 02 | CA-02 | ✔ | Consultar la salud sin sesión | 200, `status` UP y `db` UP | 200 UP | ✅ | `PlatformApiTest.healthEsPublicoYUp` |
| HU-02-CA03 | 02 | CA-03 | ✘ | Dominio o aplicación importan Spring o adaptadores | La prueba de arquitectura lo detecta | Sin dependencias prohibidas | ✅ | `ArchitectureTest` (2) |
| HU-05-CA01 | 05 | CA-01, CA-04 | ✔ | Migrar una base vacía | Existen todas las tablas y Flyway V1-V4 queda exitoso | Tablas creadas, V1-V4 exitosas | ✅ | 🐳 `DatabaseMigrationTest.migracionesCreanTodasLasTablas`, `.flywayRegistraLasMigraciones` |
| HU-05-CA02 | 05 | CA-02 | ✘ | Insertar con sede, rol o cobro inexistente | Violación de llave foránea | Rechazado | ✅ | 🐳 `DatabaseMigrationTest` (3 casos) |
| HU-05-CA03 | 05 | CA-03 | ✘ | Dos cobros para la misma placa, sede y día | Violación de unicidad | Rechazado | ✅ | 🐳 `DatabaseMigrationTest.cobroDuplicadoEsRechazado` |
| HU-06-CA01 | 06 | CA-01 | ✔ | Login válido (operador y admin) | 200 con usuario y rol; el frontend abre el panel del rol | Igual | ✅ | `AuthenticationTest.authenticatesPersistedUsers…`, `LoginPage.test` |
| HU-06-CA01-X | 06 | CA-01 | ✘ | Contraseña errada, usuario inexistente o cuenta inactiva | 401 `INVALID_CREDENTIALS` igual en los tres casos; el frontend muestra "Credenciales inválidas." | Igual | ✅ | `AuthenticationTest.wrongPassword…`, `LoginPage.test › credenciales inválidas…` |
| HU-06-CA02 | 06 | CA-02 | ✔ | Crear una cuenta | Se guarda solo el hash BCrypt de 60 caracteres | Igual | ✅ | `AuthenticationTest.newAccountsStoreOnlyBcryptHashes` |
| HU-06-CA03 | 06 | CA-03 | ✘ | El operador intenta editar tarifas o configuración | 403 sin cambios | 403 | ✅ | `AuthenticationTest.onlyAdmin…`, 🐳 `ParkingConfigurationApiTest.soloElAdministrador` |
| HU-06-CA04 | 06 | CA-04 | ✘ | 19 rutas protegidas sin sesión | 401 en todas | 401 | ✅ | `SecurityMatrixTest.sinSesion401` (19) |
| HU-06-CA04-R | 06 | CA-03, CA-04 | ✘ | 16 rutas llamadas con el rol sin permiso | 403 en todas | 403 | ✅ | `SecurityMatrixTest.rolSinPermiso403` (16) |
| HU-06-CSRF | 06 | CA-04 | ✘ | 8 escrituras con el rol correcto pero sin CSRF | 403 en todas | 403 | ✅ | `SecurityMatrixTest.escrituraSinCsrf403` (8) |
| HU-06-DENY | 06 | CA-04 | ✘ | Rutas no declaradas (`/actuator/env`, `DELETE /entries/manual`…) | 401 sin sesión y 403 con cualquier rol | Igual | ✅ | `SecurityMatrixTest.rutaNoDeclaradaSeNiega` (4) |
| HU-07-CA03 | 07 | CA-03 | ✘ | Sesión en consulta, fallo de red y fallo de carga | Botón deshabilitado y mensajes de error visibles | Igual | ✅ | `LoginPage.test` (2), `ParkingConfigurationPage.test › informa el fallo…` |
| HU-07-CA04 | 07 | CA-04 | ✘ | Un rol abre la ruta del otro rol | "Acceso denegado." y sin llamadas al backend del otro rol | Igual | ✅ | `AppRouter.test`, `ParkingConfigurationPage.test › un operador no puede…` |
| HU-08-CA01 | 08 | CA-01, CA-04 | ✔ | Simular `" abc12d "` con confianza 0.93 | 200 `ABC12D`, `MOTO`, `SIMULADOR` | Igual | ✅ | `PlateReadingControllerTest.returnsNormalized…` |
| HU-08-CA02 | 08 | CA-02 | ✘ | Confianza 1.5, -0.1, nula o ausente | 400 `INVALID_CONFIDENCE` | Igual | ✅ | `PlateReadingControllerTest.rejectsInvalidConfidence` (4), `ConfidenceTest` (12) |
| HU-09-CA01 | 09 | CA-01 a CA-03 | ✔ | Clasificar `" abc123 "` y `abc12d` por la API | `ABC123` CARRO y `ABC12D` MOTO | Igual | ✅ | `PlatformApiTest.clasificaCarro`, `.clasificaMoto` |
| HU-09-CA04 | 09 | CA-04 | ✘ | `AB123`, `ABC1234`, `ABC 123` y vacío | 400 `INVALID_PLATE`; el frontend muestra el mensaje | Igual | ✅ | `PlatformApiTest.formatoInvalido` (4), `ClassifyVehiclePage.test` |
| HU-10-CA01 | 10 | CA-01, CA-02, CA-04 | ✔ | Ingreso automático aceptado | Fila en `eventos` con placa, sede, `AUTOMATICA`, confianza 93.00 y hora del servidor | Igual | ✅ | `EntryRegistrationIntegrationTest.automaticEntryIsStored…` |
| HU-10-CA02-X | 10 | CA-02 | ✘ | El cliente envía `ocurridoEn` | Se ignora y queda la hora del servidor | Igual | ✅ | `EntryRegistrationIntegrationTest.ignoresClientProvidedTime` |
| HU-10-CA03-X | 10 | CA-03 | ✘ | Falla el INSERT del evento después de ocupar el cupo | Rollback: ocupación sin cambio y sin evento | Igual | ✅ | `EntryRegistrationIntegrationTest.occupancyIsRolledBackWhenEventCannotBeStored` |
| HU-11-CA02 | 11 | CA-02 | ✘ | Confianza 0.79 con umbral 0.80 | 200 `REQUIERE_VALIDACION`, `CONFIANZA_BAJA` | Igual | ✅ | `EntryReadingControllerTest.thresholdBoundary` |
| HU-11-CA04 | 11 | CA-04 | ✘ | Lecturas de baja confianza o con formato inválido sin confirmar | 0 eventos y ocupación sin cambio | Igual | ✅ | `EntryReadingControllerTest.noEntryIsRegisteredWithoutRequiredConfirmation` |
| HU-12-CA01 | 12 | CA-01 | ✔ | Modificar tarifas de carro y moto | 204 y valores guardados en `tarifas` | Igual | ✅ | 🐳 `ParkingConfigurationApiTest.modificaTarifasPorTipoYFranja` |
| HU-12-CA02 | 12 | CA-02 | ✘ | Nocturna igual, mayor o en cero (carro y moto) | 422 `CONFIGURACION_INVALIDA` sin cambios | Igual (5 casos) | ✅ | 🐳 `ParkingConfigurationApiTest.rechazaNocturnaMayorOIgual` |
| HU-12-CA02-E | 12 | CA-02 | ✔ | Moto exenta con $0 diurna y nocturna | 204 | 204 | ✅ | 🐳 `ParkingConfigurationApiTest.motoExentaEsValida` |
| HU-12-CA03 | 12 | CA-03 | ✔ | Diurna desde las 11:00 y reloj a las 10:00 | 204 y el cobro siguiente usa la franja NOCTURNA ($6.000) | Igual | ✅ | 🐳 `ParkingConfigurationApiTest.franjasConfigurables` |
| HU-12-CA03-X | 12 | CA-03 | ✘ | Dos franjas que empiezan a la misma hora | 422 "Las franjas deben empezar a horas distintas." | Igual | ✅ | 🐳 `ParkingConfigurationApiTest.franjasIgualesSeRechazan` |
| HU-12-CA04 | 12 | CA-04 | ✘ | Capacidad -1 y -50 | 422 y capacidad sin cambio | Igual | ✅ | 🐳 `ParkingConfigurationApiTest.cuposNegativosSeRechazan` |
| HU-12-ATOM | 12 | CA-03 (HU-23) | ✘ | Carro válido y moto inválida en la misma petición | 422 y no se guarda ninguna parte | Igual | ✅ | 🐳 `ParkingConfigurationApiTest.guardarEsAtomico` |
| HU-13-CA01 | 13 | CA-01 | ✔ | Carros en capacidad 0 e ingreso de una moto | Carro 409 LLENO y moto 201 | Igual | ✅ | `CapacityValidationIntegrationTest.capacityIsValidatedPerVehicleType` |
| HU-13-CA04 | 13 | CA-04 | ✘ | 20 ingresos simultáneos con 5 cupos (5 repeticiones) | Exactamente 5 AUTORIZADO, 15 LLENO y `ocupados` = 5 | Igual | ✅ | `ConcurrentEntryTest` (×5) |
| CP-HU-14-E01 | 14 | CA-02 | ✔ | Carro en franja diurna | Cobro PENDIENTE de $10.000 | Igual | ✅ | `ChargeServiceTest.carroDiurno`, 🐳 `ChargeApiTest.carroDiurnoGeneraCobro` |
| CP-HU-14-E02 | 14 | CA-01 | ✔ | Moto con tarifa $0 | EXENTA, sin cobro | Igual | ✅ | `ChargeServiceTest.motoExenta`, 🐳 `ChargeApiTest.motoExenta` |
| CP-HU-14-X01 | 14 | CA-06 | ✘ | Calcular sin tarifa configurada | 422 controlado y sin registros | Igual | ✅ | `ChargeServiceTest.sinTarifa`, 🐳 `ChargeApiTest.sinTarifa` |
| CP-HU-15-E01 | 15 | CA-01, CA-02 | ✔ | Eventos a las 05:59 y a las 06:00 | Días operativos distintos | Igual | ✅ | `OperatingDayCalculatorTest` (7) |
| CP-HU-15-E02 | 15 | CA-04 | ✔ | La misma placa en otro día operativo | Cobro independiente | Igual | ✅ | `ChargeServiceTest.nuevoDiaOperativo`, 🐳 `DatabaseMigrationTest.mismaPlacaOtroDiaEsPermitida` |
| CP-HU-15-X01 | 15 | CA-05, CA-06 | ✘ | Dos cobros para la misma placa, sede y día (incluso simultáneos) | Solo existe un cobro | Igual | ✅ | 🐳 `ChargeApiTest.concurrencia`, 🐳 `DatabaseMigrationTest.cobroDuplicadoEsRechazado` |
| CP-HU-16-E01 | 16 | CA-01, CA-08 | ✔ | Pagar el importe correcto | Pago registrado, cobro PAGADO y pase vigente | Igual | ✅ | `CashPaymentServiceTest.pagoExitoso`, 🐳 `PaymentApiTest.pagoExitoso` |
| CP-HU-16-E02 | 16 | CA-02 | ✔ | Repetir la misma solicitud | Un único pago (respuesta repetida) | Igual | ✅ | 🐳 `PaymentApiTest.idempotencia`, `CashierPage.test` |
| CP-HU-16-X01 | 16 | CA-04, CA-05 | ✘ | Cobro inexistente o importe incorrecto | 404 / 400 y nada cambia | Igual | ✅ | 🐳 `PaymentApiTest.erroresControlados` |
| CP-HU-16-X02 | 16 | CA-02 | ✘ | Reutilizar la clave con otro importe | 409 sin alterar el pago original | Igual | ✅ | 🐳 `PaymentApiTest.claveConOtrosDatos` |
| HU-16-CONC | 16 | CA-06 | ✘ | Seis pagos simultáneos con claves distintas | Un solo pago | Igual | ✅ | 🐳 `PaymentApiTest.concurrencia` |
| CP-HU-17-E01 | 17 | CA-01, CA-04 | ✔ | Pagar, salir y reingresar el mismo día | Sin otro cobro | Igual | ✅ | 🐳 `PassApiTest.reingresoSinCobro`, 🐳 `AcademicScenariosTest.escenario4…` |
| CP-HU-17-X01 | 17 | CA-03 | ✘ | Consultar un pase de un día anterior | VENCIDO | Igual | ✅ | `PassTest.vencidoDesdeLasSeis`, 🐳 `PassApiTest.paseVencido` |
| CP-HU-17-X02 | 17 | CA-05 | ✘ | Usar un pase de otra sede | OTRA_SEDE, sin exención | Igual | ✅ | `PassTest.otraSede`, 🐳 `PassApiTest.paseDeOtraSede` |
| CP-HU-18-E01 | 18 | CA-01 | ✔ | Salida con cobro pagado | Barrera abierta, `PAGO_CONFIRMADO` | Igual | ✅ | 🐳 `ExitApiTest.paidExitReleases…` |
| CP-HU-18-E02 | 18 | CA-02 | ✔ | Salida de una moto sin cobro | Autorizada, `EXENCION_COMPROBADA` | Igual | ✅ | 🐳 `ExitApiTest.cpE02ExemptionSurvivesTariffChange` |
| CP-HU-18-X01 | 18 | CA-03 | ✘ | Salida con cobro pendiente | Barrera cerrada, `PAGO_PENDIENTE` y auditoría | Igual | ✅ | 🐳 `ExitApiTest.cpX01PendingChargeDeniesAndAudits` |
| CP-HU-18-X02 | 18 | CA-07 | ✘ | Backend no disponible | No se indica apertura | Igual | ✅ | `AppRouter.test › CP-HU-18-X02…`, 🐳 `ExitApiTest.chargeLookupFailureIsAuditedAndReturns503` |
| CP-HU-19-E01 | 19 | CA-01 | ✔ | Salida autorizada con 10 vehículos | Ocupación 9 | Igual | ✅ | 🐳 `ExitApiTest.paidExitReleasesExactlyOneOfTen…` |
| CP-HU-19-X01 | 19 | CA-03 | ✘ | Repetir una salida | Ocupación sin cambio | Igual | ✅ | 🐳 `ExitApiTest.repeatedExitDoesNotReleaseAnotherSpot…` |
| CP-HU-19-X02 | 19 | CA-06 | ✘ | Fallo transaccional durante la salida | Sin cambios parciales | Igual | ✅ | 🐳 `ExitApiTest.auditFailureAfterRelease…` (3 casos) |
| CP-HU-20-E01 | 20 | CA-03 | ✔ | Registros de una placa | Solo esa placa | Igual | ✅ | 🐳 `HistoryApiTest.filtroPorPlaca` |
| CP-HU-20-E02 | 20 | CA-04, CA-05 | ✔ | Fecha y estado combinados | Cumplen ambos filtros | Igual | ✅ | 🐳 `HistoryApiTest.filtroPorFechaYEstado` |
| CP-HU-20-X01 | 20 | — | ✘ | Intervalo de fechas inválido | 400 | Igual | ✅ | 🐳 `HistoryApiTest.filtrosInvalidos`, `HistoryServiceTest.intervaloInvalido` |
| CP-HU-20-X02 | 20 | CA-07 | ✘ | Consultar sin autorización | 401 / 403 | Igual | ✅ | 🐳 `HistoryApiTest.controlDeAcceso` |
| CP-HU-21-E01 | 21 | CA-01 | ✔ | Estrategias de tarifa | Cada estrategia calcula lo suyo | Igual | ✅ | `TariffStrategyTest` (5) |
| CP-HU-21-E02 | 21 | CA-02 | ✔ | Sustituir el adaptador de barrera | El caso de uso funciona sin cambios | Igual | ✅ | `ExitBarrierAdapterTest` (2) |
| CP-HU-21-X01 | 21 | CA-04 | ✘ | Dependencia del dominio hacia infraestructura | Detectada | Sin violaciones | ✅ | `ArchitectureTest` (2) |
| HU-28-CA03 | 28 | CA-02, CA-03, CA-06 | ✔ | Swagger UI, `/v3/api-docs` y los 20 endpoints REST | Públicos y documentados con respuestas | Igual | ✅ | `PlatformApiTest.swaggerYOpenApiPublicos`, `.cadaEndpointEstaDocumentado` (20) |
| FE-HU12 | 12 | CA-02 | ✘ | El backend rechaza una nocturna mayor | La página muestra el mensaje del 422 y no confirma el guardado | Igual | ✅ | `ParkingConfigurationPage.test` |
| FE-HU13 | 13 | CA-02, CA-03 | ✘ | El backend responde 409 LLENO | "No hay cupo disponible…" sin "Ingreso autorizado" | Igual | ✅ | `EntryLanePage.test` |
| CP-HU-23-E01 | 23 | — | ✔ | Ejecutar las suites completas | Todas aprobadas | 416/416 aprobadas | ✅ | Sección 1 |
| CP-HU-23-X01 | 23 | — | ✘ | Introducir un error temporal | Una prueba lo detecta | 3 pruebas del backend y 1 del frontend fallaron | ✅ | Sección 4 |

El detalle de los 416 casos ejecutados, con su estado y su tiempo, está en el [catálogo de resultados](catalogo-resultados.md). La relación completa HU → CA → pruebas está en la [matriz de trazabilidad](matriz-trazabilidad.md).

## 3. Escenarios académicos de demostración

Los ocho escenarios obligatorios están en la issue #37 (HU-27, sección "Escenarios obligatorios de demostración"). Los escenarios 1 a 7 se ejecutan de punta a punta por la API REST, con la seguridad real, las migraciones Flyway y PostgreSQL 16 aislado, en `backend/src/test/java/com/parkingspot/infrastructure/AcademicScenariosTest.java`, con el reloj fijo el 23/11/2026 a las 10:00 de Bogotá.

| # | Escenario | Pasos automatizados | Verificaciones | Resultado | Evidencia |
|---|---|---|---|---|---|
| 1 | Motocicleta que ingresa y sale sin cobro | `POST /entries/manual` `ABC12D` → `POST /exits/validate` | 201 MOTO, 0 cobros, barrera abierta con `EXENCION_COMPROBADA`, ocupación de motos 1 → 0 | ✅ Aprobado | `escenario1MotoIngresaYSaleSinCobro` |
| 2 | Carro que ingresa de día y genera un cobro pendiente | `POST /entries/manual` `ABC123` → `GET /charges/today` | Franja DIURNA, PENDIENTE, $10.000, día operativo 2026-11-23, un cobro | ✅ Aprobado | `escenario2CarroDiurnoGeneraCobroPendiente` |
| 3 | Carro que intenta salir sin pagar y encuentra la barrera cerrada | Ingreso → `POST /exits/validate` | `barrierOpen=false`, `PAGO_PENDIENTE`, ocupación sin cambio, evento de validación RECHAZADO | ✅ Aprobado | `escenario3SalidaSinPagoMantieneLaBarreraCerrada` |
| 4 | Pago, salida y reingreso sin un nuevo cobro | Ingreso → `POST /payments` → salida → reingreso | Cobro PAGADO con pase, barrera abierta con `PAGO_CONFIRMADO`, reingreso 201 y el mismo cobro (uno solo) | ✅ Aprobado | `escenario4PagoSalidaYReingresoSinNuevoCobro` |
| 5 | Lectura de baja confianza con validación manual | `POST /entries/automatic` con confianza 0.42 → `POST /entries/manual` | `REQUIERE_VALIDACION` sin eventos ni cobros; después 201 MANUAL, sin confianza y con `usuario_id` | ✅ Aprobado | `escenario5BajaConfianzaConValidacionManual` |
| 6 | Parqueadero lleno que rechaza un nuevo ingreso | Capacidad de carros 1 → dos ingresos | 201 y luego 409 LLENO con barrera cerrada, ocupación 1, sin cobro, evento RECHAZADO/LLENO | ✅ Aprobado | `escenario6ParqueaderoLlenoRechazaIngreso` |
| 7 | Rechazo de tarifa nocturna mayor o igual a la diurna | `PUT /admin/sites/{id}/configuration` con nocturna 10.000 y 12.000 | 422 `CONFIGURACION_INVALIDA` en ambos casos y nocturna guardada sin cambio ($6.000) | ✅ Aprobado | `escenario7TarifaNocturnaMayorOIgualSeRechaza` |
| 8 | Ejecución de la suite de pruebas automatizadas | `mvn -f backend/pom.xml clean verify` y `npm test` | 343/343 y 73/73 aprobadas | ✅ Aprobado | Sección 1 y [catálogo](catalogo-resultados.md) |

En la interfaz, los escenarios 6 y 7 tienen además evidencia en `EntryLanePage.test` (mensaje LLENO) y `ParkingConfigurationPage.test` (mensaje del 422).

## 4. CP-HU-23-X01 · Las pruebas detectan fallos

En los dos casos se introdujo un error **temporal** en el código de producción, se ejecutaron las pruebas, se guardó la salida y se revirtió con `git checkout --`. **Ningún cambio se incluyó en un commit.** Después de revertir, las mismas pruebas volvieron a pasar (24/24 y 73/73).

### 4.1 Backend: regla de la HU-12 CA-02 (escenario 7)

Cambio introducido en `backend/src/main/java/com/parkingspot/infrastructure/adapter/out/persistence/ParkingConfigurationStore.java`. Con él, una tarifa nocturna **igual** a la diurna deja de rechazarse:

```diff
-        if (!exempt && (rates.night().compareTo(rates.day()) >= 0
+        if (!exempt && (rates.night().compareTo(rates.day()) > 0
```

Comando: `mvn -f backend/pom.xml test -Dtest=ParkingConfigurationApiTest,AcademicScenariosTest`

```text
[ERROR] Tests run: 7, Failures: 1, Errors: 0, Skipped: 0 <<< FAILURE! -- in com.parkingspot.infrastructure.AcademicScenariosTest
[ERROR] com.parkingspot.infrastructure.AcademicScenariosTest.escenario7TarifaNocturnaMayorOIgualSeRechaza <<< FAILURE!
java.lang.AssertionError: Status expected:<422> but was:<204>
[ERROR] Tests run: 17, Failures: 2, Errors: 0, Skipped: 0 <<< FAILURE! -- in com.parkingspot.infrastructure.ParkingConfigurationApiTest
[ERROR] com.parkingspot.infrastructure.ParkingConfigurationApiTest.rechazaNocturnaMayorOIgual(String, int, int)[1] <<< FAILURE!
java.lang.AssertionError: Status expected:<422> but was:<204>
[ERROR] com.parkingspot.infrastructure.ParkingConfigurationApiTest.rechazaNocturnaMayorOIgual(String, int, int)[4] <<< FAILURE!
java.lang.AssertionError: Status expected:<422> but was:<204>
[ERROR]   AcademicScenariosTest.escenario7TarifaNocturnaMayorOIgualSeRechaza:188 Status expected:<422> but was:<204>
[ERROR]   ParkingConfigurationApiTest.rechazaNocturnaMayorOIgual:144 Status expected:<422> but was:<204>
[ERROR]   ParkingConfigurationApiTest.rechazaNocturnaMayorOIgual:144 Status expected:<422> but was:<204>
[ERROR] Tests run: 24, Failures: 3, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

Fallaron justo los casos con nocturna **igual** a la diurna: `[1]` carro 10.000/10.000 y `[4]` moto 2.000/2.000. Los casos con nocturna mayor siguieron pasando, así que las pruebas distinguen el valor límite. El backend respondió 204 y guardó una configuración inválida.

### 4.2 Frontend: el carril de entrada exige la autorización explícita de la barrera

Cambio introducido en `frontend/src/features/entries/entriesApi.ts`. Con él, la interfaz confirmaría un ingreso aunque el backend no abriera la barrera:

```diff
-  if (entry?.resultado !== 'AUTORIZADO' || entry.barreraAbierta !== true || !entry.plate) {
+  if (entry?.resultado !== 'AUTORIZADO' || !entry.plate) {
```

Comando: `npm test` en `frontend/`

```text
 ❯ src/tests/EntryLanePage.test.tsx (5 tests | 1 failed)
     × no confirma el ingreso si el backend no autoriza explícitamente la barrera
 FAIL  src/tests/EntryLanePage.test.tsx > carril de entrada > no confirma el ingreso si el backend no autoriza explícitamente la barrera
AssertionError: expected [] to deeply equal [ Array(1) ]
 Test Files  1 failed | 11 passed (12)
      Tests  1 failed | 72 passed (73)
```

La alerta "El servidor no confirmó el registro del ingreso." no apareció: sin la prueba, el operador habría visto "Ingreso autorizado" con la barrera cerrada.

## 5. Hallazgos

**Bugs encontrados: ninguno.** Las 126 pruebas nuevas pasaron contra el código de `develop`. No hubo que marcar ninguna prueba con `@Disabled`. En particular, las reglas de la HU-12 (nocturna menor que diurna, franjas distintas, cupos no negativos y guardado atómico) se comportan como dicen sus CA.

Observaciones (no son fallos):

1. **La validación de la HU-12 vive en el adaptador de persistencia** (`ParkingConfigurationStore`), no en el dominio, y usa SQL propio de PostgreSQL (`ON CONFLICT`). Por eso solo se puede probar por la API con Testcontainers y no con pruebas unitarias del dominio. El PR #50 (HU-22) mueve los errores a excepciones de dominio y conserva el contrato HTTP (422 `CONFIGURACION_INVALIDA` y 404), así que estas pruebas siguen siendo válidas después de ese PR.
2. **Docker apagado.** Sin Docker, las 9 suites de Testcontainers (87 casos) se omiten y el build sigue en verde. Se documentó en la [guía de ejecución](ejecucion-pruebas.md) cómo verificar que el resumen diga `Skipped: 0`. El total sin Docker (`Tests run: 336, Skipped: 80`) se calculó a partir de la corrida base, que se hizo con Docker apagado, más los métodos agregados.
3. **Vitest en Windows.** Si la carpeta se abre con la letra de unidad en minúscula (`c:\`), fallan todos los archivos con "Vitest failed to find the runner". Se documentó cómo evitarlo.
4. **El frontend no usa React Testing Library.** Las pruebas siguen el estilo del equipo (Vitest + jsdom + `act`), con el helper `tests/support/testApp.tsx`.
