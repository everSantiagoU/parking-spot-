# Matriz de trazabilidad HU → CA → pruebas (HU-23)

Los criterios de aceptación salen de las issues de GitHub de cada historia. Cada prueba se nombra como `Clase.método` (backend, `backend/src/test/java/com/parkingspot/…`) o `archivo › caso` (frontend, `frontend/src/tests/…`). Los resultados de la última corrida están en el [catálogo de resultados](catalogo-resultados.md).

Convenciones: 🐳 = necesita Docker (Testcontainers, PostgreSQL real). ★ = prueba agregada en la HU-23. **Manual** = criterio de proceso o infraestructura, verificado fuera de las suites automatizadas.

## Resumen

| HU | CA cubiertos por pruebas | CA con evidencia manual |
|---|---|---|
| HU-01 Repositorio y Gitflow | — | CA-01 a CA-04 |
| HU-02 Backend base | CA-01, CA-02 ★, CA-03, CA-04 | — |
| HU-03 Frontend base | CA-02, CA-03, CA-04 | CA-01 |
| HU-04 PostgreSQL y Compose | — | CA-01 a CA-04 |
| HU-05 Modelo de datos | CA-01 a CA-04 | — |
| HU-06 Autenticación y roles | CA-01 a CA-04 | — |
| HU-07 Interfaz por rol | CA-01 a CA-04 | — |
| HU-08 a HU-11, HU-13 | todos | — |
| HU-12 Tarifas, franjas y cupos | CA-01 a CA-05 ★ | — |
| HU-14 a HU-20 | todos | — |
| HU-21 Patrones | CA-01 a CA-04, CA-07 | CA-05, CA-06 (documentación) |
| HU-24 CI | CA-04 (lo ejecuta el pipeline) | CA-01 a CA-03, CA-05 a CA-07 |
| HU-28 Swagger/OpenAPI | CA-01 a CA-06 ★ | — |

## Detalle por historia

### HU-01 · Repositorio, Gitflow y GitHub Projects
| CA | Pruebas / evidencia |
|---|---|
| CA-01 Repositorio con acceso del docente | Manual: configuración del repositorio `everSantiagoU/parking-spot-` |
| CA-02 Ramas `main` y `develop` | Manual: `git branch -r` |
| CA-03 Tablero con estados, sprint y responsables | Manual: GitHub Project "Parking Spot" |
| CA-04 PR a `develop` con revisión | Manual: reglas de protección de rama; los checks los ejecuta HU-24 |

### HU-02 · Backend Spring Boot y estructura modular
| CA | Pruebas |
|---|---|
| CA-01 Compila con Java 21 | `mvn verify` (job "Backend" de `ci.yml`) |
| CA-02 `/actuator/health` responde | ★ `PlatformApiTest.healthEsPublicoYUp` |
| CA-03 Paquetes por capa | `ArchitectureTest.elDominioNoDependeDeFrameworksNiDeOtrasCapas`, `ArchitectureTest.laAplicacionNoDependeDeSpringNiDeLosAdaptadores` |
| CA-04 Configuración por variables de entorno | `EntryReadingConfigTest.readsThresholdFromEnvironmentVariable`, `EntryReadingConfigTest.usesDefaultThresholdWhenVariableIsAbsent` |

### HU-03 · Frontend React y TypeScript
| CA | Pruebas |
|---|---|
| CA-01 Vite inicia sin errores | Manual: `npm run dev` |
| CA-02 TypeScript valida | `npm run build` (`tsc -b`, job "Frontend" de `ci.yml`) |
| CA-03 Rutas base de operador y admin | `AppRouter.test › recupera la sesión del operador…`, `› recupera la sesión de administrador…`, `› bloquea el acceso directo al panel del otro rol…` |
| CA-04 Cliente HTTP con URL configurable | `httpClient.test › usa la URL base configurada y envía JSON`, `› la URL base no termina en barra` |

### HU-04 · PostgreSQL y Docker Compose
| CA | Pruebas / evidencia |
|---|---|
| CA-01 a CA-04 | Manual: `docker compose up -d`, `scripts/verificar-entorno.sh` y el README raíz. Las suites 🐳 usan la misma versión de PostgreSQL (16) |

### HU-05 · Modelo de datos y migraciones Flyway
| CA | Pruebas |
|---|---|
| CA-01 Tablas creadas | 🐳 `DatabaseMigrationTest.migracionesCreanTodasLasTablas` |
| CA-02 Claves foráneas | 🐳 `DatabaseMigrationTest.cobroConSedeInexistenteEsRechazado`, `.usuarioConRolInexistenteEsRechazado`, `.pagoConCobroInexistenteEsRechazado` |
| CA-03 Unicidad placa + sede + día operativo | 🐳 `DatabaseMigrationTest.cobroDuplicadoEsRechazado`, `.mismaPlacaOtroDiaEsPermitida` |
| CA-04 Migraciones desde BD vacía | 🐳 `DatabaseMigrationTest.flywayRegistraLasMigraciones`, `.migracionesCreanTodasLasTablas` |
| Integridad adicional | 🐳 `.pagoDuplicadoEsRechazado`, `.cobroPagadoSinVigenciaEsRechazado`, `.placaInvalidaEsRechazada`, `.ocupacionMayorQueCapacidadEsRechazada`, `.datosInicialesCumplenLasReglas` |

### HU-06 · Autenticación y roles
| CA | Pruebas |
|---|---|
| CA-01 Login válido autentica | `AuthenticationTest.authenticatesPersistedUsersAndReturnsOnlyUsernameAndRole`, ★ `LoginPage.test › un login válido de operador…`, ★ `› un login válido de administrador…` |
| CA-02 Contraseñas con hash | `AuthenticationTest.newAccountsStoreOnlyBcryptHashes` |
| CA-03 El operador no modifica tarifas | `AuthenticationTest.onlyAdminMayReachTariffHandlerAndOnlyOperatorMayClassify`, ★ 🐳 `ParkingConfigurationApiTest.soloElAdministrador`, ★ `SecurityMatrixTest.rolSinPermiso403` (rutas `/api/v1/admin/**`) |
| CA-04 Rutas protegidas rechazan sesión inválida | `AuthenticationTest.protectedApiRequiresSessionEvenForPostWithoutCsrf`, `.logoutInvalidatesSessionAndReplayIsUnauthorized`, `.csrfIsRequiredForLoginAndAuthenticatedWrites`, ★ `SecurityMatrixTest.sinSesion401` (19 rutas), ★ `.escrituraSinCsrf403` (8), ★ `.rutaNoDeclaradaSeNiega` (4) |
| Negativo: credenciales inválidas | `AuthenticationTest.wrongPasswordUnknownUserAndDisabledAccountGetTheSameError`, ★ `LoginPage.test › credenciales inválidas muestran el error…` |

### HU-07 · Interfaz base de operador y administrador
| CA | Pruebas |
|---|---|
| CA-01 El operador ve el panel de carril | `AppRouter.test › recupera la sesión del operador y solo enlaza sus módulos`, ★ `LoginPage.test › un login válido de operador…` |
| CA-02 El admin ve el menú de configuración | `AppRouter.test › recupera la sesión de administrador y muestra sus módulos` |
| CA-03 Carga y errores | ★ `LoginPage.test › mientras se consulta la sesión…`, ★ `› un fallo de conexión…`, ★ `ParkingConfigurationPage.test › informa el fallo al cargar la configuración`, `AppRouter.test › CP-HU-18-X02 …` |
| CA-04 Navegación por rol | `AppRouter.test › bloquea el acceso directo al panel del otro rol…`, `› bloquea la ruta a un administrador`, ★ `ParkingConfigurationPage.test › un operador no puede abrir la configuración` |

### HU-08 · Lector de placas simulado
| CA | Pruebas |
|---|---|
| CA-01 Recibe placa y confianza | `PlateReadingControllerTest.returnsNormalizedPlateTypeConfidenceAndSimulatorSource` |
| CA-02 Confianza entre 0 y 1 | `ConfidenceTest` (12), `PlateReadingControllerTest.acceptsConfidenceBoundaries`, `.rejectsInvalidConfidence` |
| CA-03 Lector intercambiable | `PlateReadingServiceTest.dependsOnlyOnThePortSoAnyReaderCanBePluggedIn`, `SimulatedPlateReaderTest` |
| CA-04 Respuesta con placa y fuente | `PlateReadingControllerTest.returnsNormalized…`, `SimulatedPlateReaderTest.returnsReceivedPlateAndConfidenceWithSimulatorSource` |
| Seguridad | `PlateReadingControllerTest.rejectsRequestWithoutSession`, `.rejectsAdminRole`, `.rejectsOperatorWithoutCsrfToken` |

### HU-09 · Clasificar motos y carros
| CA | Pruebas |
|---|---|
| CA-01 AAA123 es carro | `PlateClassifierTest.classifiesAndNormalizesCarPlate`, ★ `PlatformApiTest.clasificaCarro` |
| CA-02 AAA12A es moto | `PlateClassifierTest.classifiesMotorcyclePlate`, ★ `PlatformApiTest.clasificaMoto`, ★ `ClassifyVehiclePage.test › muestra la placa normalizada y el tipo…` |
| CA-03 Normaliza minúsculas y espacios | `PlateClassifierTest.classifiesAndNormalizesCarPlate`, ★ `PlatformApiTest.clasificaCarro` (`" abc123 "`) |
| CA-04 Formato inválido requiere corrección | `PlateClassifierTest.rejectsInvalidPlates`, ★ `PlatformApiTest.formatoInvalido` (4), ★ `ClassifyVehiclePage.test › muestra el mensaje del backend ante un formato inválido` |

### HU-10 · Registrar eventos de ingreso
| CA | Pruebas |
|---|---|
| CA-01 Guarda placa y sede | `EntryRegistrationIntegrationTest.automaticEntryIsStoredWithSiteServerTimeAndSourceAndUpdatesOccupancy`, `EntryEventTest.authorizedEventKeepsPlateSiteTimeSourceAndUser` |
| CA-02 Hora del servidor | `EntryRegistrationIntegrationTest.automaticEntryIsStored…`, `.ignoresClientProvidedTime`, `EntryRegistrationServiceTest.registersAcceptedAutomaticReadingWithServerTimeAndOccupiesSpot` |
| CA-03 Entrada autorizada actualiza ocupación | `EntryRegistrationIntegrationTest.automaticEntryIsStored…`, `.occupancyIsRolledBackWhenEventCannotBeStored`, ★ `EntryLanePage.test › registra la placa digitada…` |
| CA-04 Guarda el origen de la lectura | `EntryRegistrationIntegrationTest.manualEntryIsStoredAsManualWithUserAndNoConfidence`, `EntryEventStoreMappingTest` |

### HU-11 · Validar confianza y lectura manual
| CA | Pruebas |
|---|---|
| CA-01 Umbral configurable | `EntryReadingConfigTest` (3) |
| CA-02 Confianza inferior pide validación | `ConfidenceThresholdTest` (7), `EntryReadingControllerTest.thresholdBoundary`, `.invalidFormatRequiresValidation` |
| CA-03 Placa manual identificada | `AcceptedReadingTest.manualReadingIsMarkedManualWithItsOperator`, `EntryReadingControllerTest.manualConfirmationIsMarkedManualWithOperator` |
| CA-04 Sin confirmación no hay entrada | `EntryReadingControllerTest.noEntryIsRegisteredWithoutRequiredConfirmation`, `EntryRegistrationServiceTest.readingThatRequiresValidationRegistersNothing`, `AcceptedReadingTest.lowConfidenceReadingCannotBeAccepted`, ★ 🐳 `AcademicScenariosTest.escenario5BajaConfianzaConValidacionManual` |

### HU-12 · Administrar tarifas, franjas y cupos
| CA | Pruebas |
|---|---|
| CA-01 Crear y modificar tarifas por tipo y franja | ★ 🐳 `ParkingConfigurationApiTest.modificaTarifasPorTipoYFranja`, `.leeLaConfiguracionInicial`, 🐳 `AdministrationApiTest.onlyAdminCanSaveAndOnlyFutureChargesUseTheNewTariff`, ★ `ParkingConfigurationPage.test › guarda la nueva tarifa…`, `› carga la sede, las franjas…` |
| CA-02 Nocturna menor que diurna | ★ 🐳 `ParkingConfigurationApiTest.rechazaNocturnaMayorOIgual` (5 casos), `.motoExentaEsValida`, ★ 🐳 `AcademicScenariosTest.escenario7TarifaNocturnaMayorOIgualSeRechaza`, ★ `ParkingConfigurationPage.test › muestra el rechazo del backend cuando la nocturna…` |
| CA-03 Franjas con horarios configurables | ★ 🐳 `ParkingConfigurationApiTest.franjasConfigurables`, `.franjasIgualesSeRechazan`, ★ `TimeSlotScheduleTest` (8), `TimeSlotResolverTest` (8) |
| CA-04 Cupos no negativos | ★ 🐳 `ParkingConfigurationApiTest.cuposNegativosSeRechazan` (2), `.cupoEnCeroEsValido`, `.importesInvalidos`, 🐳 `AdministrationApiTest.onlyAdminCanSave…` (capacidad menor que la ocupación), ★ `ParkingConfigurationPage.test › muestra el rechazo de un cupo inválido` |
| CA-05 El operador no edita la configuración | ★ 🐳 `ParkingConfigurationApiTest.soloElAdministrador`, ★ `SecurityMatrixTest.rolSinPermiso403`, ★ `ParkingConfigurationPage.test › un operador no puede abrir la configuración` |
| Consistencia transaccional | ★ 🐳 `ParkingConfigurationApiTest.guardarEsAtomico`, `.sedeInexistente` |

### HU-13 · Validar cupos antes de abrir la entrada
| CA | Pruebas |
|---|---|
| CA-01 Cupo por tipo | `CapacityValidationIntegrationTest.capacityIsValidatedPerVehicleType` |
| CA-02 Sin cupo muestra LLENO | `CapacityValidationIntegrationTest.fullSiteAnswersLlenoAndKeepsBarrierClosed`, ★ `EntryLanePage.test › HU-13: sin cupo muestra LLENO…`, ★ 🐳 `AcademicScenariosTest.escenario6ParqueaderoLlenoRechazaIngreso` |
| CA-03 Sin cupo la barrera sigue cerrada | Las mismas, y `EntryRegistrationServiceTest.fullSiteRejectsEntryKeepsBarrierClosedAndStoresRejectedEvent` |
| CA-04 Ingresos simultáneos no superan la capacidad | `ConcurrentEntryTest.simultaneousEntriesNeverExceedCapacity` (×5), 🐳 `ExitApiTest.simultaneousEntriesOccupyOneSpotUntilEffectiveExit`, 🐳 `DatabaseMigrationTest.ocupacionMayorQueCapacidadEsRechazada` |

### HU-14 · Motor de cálculo de cobros
| CA | Pruebas |
|---|---|
| CA-01 Tarifa de motos | `ChargeServiceTest.motoExenta`, `TariffStrategyTest.motoConTarifaCeroNoGeneraCobro`, 🐳 `ChargeApiTest.motoExenta` |
| CA-02 Tarifa diurna | `ChargeServiceTest.carroDiurno`, 🐳 `ChargeApiTest.carroDiurnoGeneraCobro`, ★ 🐳 `AcademicScenariosTest.escenario2CarroDiurnoGeneraCobroPendiente` |
| CA-03 Tarifa nocturna | `ChargeServiceTest.carroNocturno`, ★ 🐳 `ParkingConfigurationApiTest.franjasConfigurables` |
| CA-04 Conservación de la tarifa | `ChargeServiceTest.tarifaConservada`, 🐳 `ChargeApiTest.tarifaConservada`, 🐳 `AdministrationApiTest.onlyAdminCanSave…` |
| CA-05 Strategy | `TariffStrategyTest` (5) |
| CA-06 Tarifa inexistente | `ChargeServiceTest.sinTarifa`, 🐳 `ChargeApiTest.sinTarifa`, `TimeSlotResolverTest.sinFranjaConfiguradaEsUnErrorControlado` |
| CA-07 Integración con el frontend | `ChargePage.test` (4), `chargesApi.test` (3) |

### HU-15 · Día operativo y unicidad de cobros
| CA | Pruebas |
|---|---|
| CA-01 y CA-02 Inicio del día y madrugada | `OperatingDayCalculatorTest` (7: 05:59 / 06:00, zona Bogotá, fin del día) |
| CA-03 Reingreso | `ChargeServiceTest.reingresoMismoDia`, 🐳 `ChargeApiTest.reingresoNoDuplica` |
| CA-04 Nuevo día operativo | `ChargeServiceTest.nuevoDiaOperativo` |
| CA-05 Unicidad en PostgreSQL | 🐳 `DatabaseMigrationTest.cobroDuplicadoEsRechazado` |
| CA-06 Concurrencia | `ChargeServiceTest.solicitudesSimultaneas`, 🐳 `ChargeApiTest.concurrencia` |

### HU-16 · Pago en efectivo idempotente
| CA | Pruebas |
|---|---|
| CA-01 Pago exitoso | `CashPaymentServiceTest.pagoExitoso`, 🐳 `PaymentApiTest.pagoExitoso`, `CashierPage.test › registra el pago…` |
| CA-02 Idempotencia | `CashPaymentServiceTest.idempotencia`, 🐳 `PaymentApiTest.idempotencia`, `CashierPage.test › reutiliza la misma clave de idempotencia…` |
| CA-03 Trazabilidad | `CashPaymentServiceTest.trazabilidad`, 🐳 `PaymentApiTest.trazabilidad` |
| CA-04 Cobro inexistente | `CashPaymentServiceTest.cobroInexistente`, 🐳 `PaymentApiTest.erroresControlados` |
| CA-05 Importe incorrecto | `CashPaymentServiceTest.importeIncorrecto`, 🐳 `PaymentApiTest.erroresControlados` |
| CA-06 Pago duplicado | `CashPaymentServiceTest.pagoDuplicado`, 🐳 `PaymentApiTest.pagoDuplicado`, 🐳 `DatabaseMigrationTest.pagoDuplicadoEsRechazado`, `CashierPage.test › para un cobro ya pagado…` |
| CA-07 Autorización | 🐳 `PaymentApiTest.autorizacion`, ★ `SecurityMatrixTest` (`/api/v1/payments`, `/api/v1/payments/charge/1`) |
| CA-08 Pase vigente | `CashPaymentServiceTest.paseVigente`, 🐳 `PaymentApiTest.pagoExitoso` |
| Concurrencia | 🐳 `PaymentApiTest.concurrencia`, `CashPaymentServiceTest.claveConOtrosDatos` (CP-HU-16-X02) |

### HU-17 · Pase vigente
| CA | Pruebas |
|---|---|
| CA-01 Generación del pase | `PassServiceTest.paseVigente`, 🐳 `PassApiTest.reingresoSinCobro` |
| CA-02 Vigencia | `PassTest.vigenteElMismoDia`, `.vigenteAntesDeLasSeis` |
| CA-03 Vencimiento | `PassTest.vencidoDesdeLasSeis`, `PassServiceTest.paseVencido`, 🐳 `PassApiTest.paseVencido`, `PassPage.test › un pase vencido…` |
| CA-04 Reingreso sin cobro | `PassTest.reingresoSinCobro`, 🐳 `PassApiTest.reingresoSinCobro`, ★ 🐳 `AcademicScenariosTest.escenario4PagoSalidaYReingresoSinNuevoCobro` |
| CA-05 Validación de sede | `PassTest.otraSede`, `PassServiceTest.paseDeOtraSede`, 🐳 `PassApiTest.paseDeOtraSede`, `PassPage.test › un pase de otra sede…` |
| CA-06 Consulta en el frontend | `PassPage.test` (5) |

### HU-18 · Validación de salida y barrera simulada
| CA | Pruebas |
|---|---|
| CA-01 Salida con pago | 🐳 `ExitApiTest.paidExitReleasesExactlyOneOfTenAndAuditsTheDecision`, ★ 🐳 `AcademicScenariosTest.escenario4…` |
| CA-02 Salida sin obligación de pago | 🐳 `ExitApiTest.cpE02ExemptionSurvivesTariffChange`, ★ 🐳 `AcademicScenariosTest.escenario1MotoIngresaYSaleSinCobro` |
| CA-03 Pago pendiente | 🐳 `ExitApiTest.cpX01PendingChargeDeniesAndAudits`, `.closedChargeDoesNotSatisfyTheObligation`, ★ 🐳 `AcademicScenariosTest.escenario3SalidaSinPagoMantieneLaBarreraCerrada` |
| CA-04 Motivo de la decisión | Las anteriores verifican `code` (`PAGO_CONFIRMADO`, `EXENCION_COMPROBADA`, `PAGO_PENDIENTE`) |
| CA-05 Registro del evento | 🐳 `ExitApiTest.paidExitReleases…`, `.cpX01PendingChargeDeniesAndAudits` |
| CA-06 Validación de ingreso | 🐳 `ExitApiTest.missingEntryDeniesEvenWithPaidCharge`, `.entryInAnotherSiteDoesNotAuthorize`, `AppRouter.test › enlaza al carril de entrada…` |
| CA-07 Fallo del sistema | 🐳 `ExitApiTest.chargeLookupFailureIsAuditedAndReturns503`, `AppRouter.test › CP-HU-18-X02…`, `› cierra la barrera si el backend responde con un error de auditoría`, `› exige una autorización explícita del backend` |
| Adapter de barrera | `ExitBarrierAdapterTest` (2) |

### HU-19 · Ocupación al registrar salidas
| CA | Pruebas |
|---|---|
| CA-01 Liberación de cupo | 🐳 `ExitApiTest.paidExitReleasesExactlyOneOfTenAndAuditsTheDecision` |
| CA-02 Salida rechazada | 🐳 `ExitApiTest.cpX01PendingChargeDeniesAndAudits`, `AppRouter.test › conserva la disponibilidad ante una salida rechazada` |
| CA-03 Salida duplicada | 🐳 `ExitApiTest.repeatedExitDoesNotReleaseAnotherSpotAndThePlateCanReenter`, `.simultaneousRequestsForTheSameEntryCanCommitOnlyOneExit` |
| CA-04 Consistencia de ocupación | 🐳 `ExitApiTest.occupancyOnlyIncludesTheSessionOperatorsSite`, `.simultaneousEntriesOccupyOneSpotUntilEffectiveExit` |
| CA-05 Actualización del frontend | `AppRouter.test › muestra los cupos persistidos y los consulta otra vez…`, `› conserva el resultado de la salida si falla la consulta posterior…` |
| CA-06 Integridad transaccional | 🐳 `ExitApiTest.missingOrEmptyOccupancyRollsBackTheExitAndKeepsTheEntry`, `.failureInsertingTheEffectiveExitIsAuditedAndKeepsTheSpotOccupied`, `.auditFailureAfterReleaseRollsBackTheExitAndTheOccupancy` |

### HU-20 · Consultar eventos y cobros con filtros
| CA | Pruebas |
|---|---|
| CA-01 Listado de eventos | 🐳 `HistoryApiTest.listadoDeEventos`, `HistoryPage.test › lista los eventos filtrados por placa` |
| CA-02 Listado de cobros | 🐳 `HistoryApiTest.listadoDeCobros`, `HistoryPage.test › combina fecha y estado…` |
| CA-03 Filtro por placa | 🐳 `HistoryApiTest.filtroPorPlaca`, `HistoryServiceTest.normalizaPlaca` |
| CA-04 Filtro por fecha | 🐳 `HistoryApiTest.filtroPorFechaYEstado`, `HistoryServiceTest.intervaloDeEventosEnHoraLocal` |
| CA-05 Filtro por estado | 🐳 `HistoryApiTest.filtroPorFechaYEstado`, `HistoryServiceTest.combinaFechaYEstado` |
| CA-06 Sin resultados | 🐳 `HistoryApiTest.consultaSinResultados`, `HistoryPage.test › muestra un estado vacío sin errores` |
| CA-07 Control de acceso | 🐳 `HistoryApiTest.controlDeAcceso`, 🐳 `AdministrationApiTest.historyCombinesInclusiveDates…`, ★ `SecurityMatrixTest` |
| Negativos | 🐳 `HistoryApiTest.filtrosInvalidos`, `HistoryServiceTest.intervaloInvalido`, `.paginacionInvalida`, `HistoryPage.test › muestra el mensaje del backend ante un filtro inválido` |

### HU-21 · Patrones de diseño
| CA | Pruebas / evidencia |
|---|---|
| CA-01 Strategy | `TariffStrategyTest` (5) |
| CA-02 Adapter | `ExitBarrierAdapterTest`, `PlateReadingServiceTest.dependsOnlyOnThePort…` |
| CA-03 Repository | Las suites de persistencia 🐳 y H2 usan los repositorios reales (`JdbcClient`) |
| CA-04 Arquitectura hexagonal | `ArchitectureTest` (2) |
| CA-05 y CA-06 Justificación y evidencia gráfica | Manual: `docs/architecture/patrones-diseno.md` |
| CA-07 Verificación | Las pruebas anteriores |

### HU-24 · CI con GitHub Actions
| CA | Pruebas / evidencia |
|---|---|
| CA-01 a CA-07 | Manual: `.github/workflows/ci.yml` y los checks de cada PR. El pipeline ejecuta todas las suites de esta matriz (CA-04) |

### HU-28 · Swagger/OpenAPI
| CA | Pruebas |
|---|---|
| CA-01 Configuración | ★ `PlatformApiTest.swaggerYOpenApiPublicos` (título "Parking Spot API") |
| CA-02 Acceso a Swagger UI | ★ `PlatformApiTest.swaggerYOpenApiPublicos` |
| CA-03 Documentación de endpoints | ★ `PlatformApiTest.cadaEndpointEstaDocumentado` (20 rutas) |
| CA-04 Documentación de errores | `PlateReadingApiDocsTest`, `EntryApiDocsTest`, 🐳 `ExitApiTest.documentsTheContract` |
| CA-05 Documentación de seguridad | `PlateReadingApiDocsTest`, `EntryApiDocsTest` (401 y 403) |
| CA-06 Especificación OpenAPI | ★ `PlatformApiTest.swaggerYOpenApiPublicos` (`/v3/api-docs`) |

### HU-23 · Pruebas unitarias y de integración
| CA | Evidencia |
|---|---|
| CA-01 Unitarias del dominio | 23 clases y 127 pruebas sin Spring ni base de datos ([informe](informe-pruebas.md#1-resumen)) |
| CA-02 API | 19 clases de integración, incluidas `SecurityMatrixTest` y `PlatformApiTest` |
| CA-03 Persistencia | `DatabaseMigrationTest` y las suites 🐳 y H2 de integridad, unicidad y rollback |
| CA-04 Frontend | 12 archivos y 73 pruebas con Vitest |
| CA-05 Positivos y negativos | Columna "Tipo" del [informe](informe-pruebas.md#2-casos-de-prueba) |
| CA-06 Escenarios académicos | `AcademicScenariosTest` ([informe §3](informe-pruebas.md#3-escenarios-académicos-de-demostración)) |
| CA-07 Informe | [informe-pruebas.md](informe-pruebas.md) y [catalogo-resultados.md](catalogo-resultados.md) |
| CA-08 Ejecución reproducible | [ejecucion-pruebas.md](ejecucion-pruebas.md) |
