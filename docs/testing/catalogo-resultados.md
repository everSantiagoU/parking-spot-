# Catálogo de resultados de pruebas

Generado a partir de los reportes de la corrida del 27/11/2026: `backend/target/surefire-reports/TEST-*.xml` (`mvn -f backend/pom.xml clean verify`, Docker encendido) y el reporte JSON de Vitest (`npx vitest run --reporter=json`). Cada fila es un caso ejecutado; la columna **Obtenido** es el estado real del reporte.

Leyenda de entorno: **Docker** = PostgreSQL 16 aislado con Testcontainers; **H2** = contexto de Spring con H2 en modo PostgreSQL; **Unidad** = sin Spring ni base de datos; **jsdom** = Vitest en navegador simulado.

## Backend (343 casos: 343 aprobados, 0 fallidos, 0 omitidos)

### AcceptedReadingTest · Unidad · 4 casos

`com.parkingspot.domain.AcceptedReadingTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| manualReadingIsMarkedManualWithItsOperator | ✅ Aprobado | 0.001 s |
| acceptsAutomaticReadingAtOrAboveThreshold | ✅ Aprobado | 0.001 s |
| readingThatRequiresValidationCarriesNothingToRegister | ✅ Aprobado | 0.001 s |
| lowConfidenceReadingCannotBeAccepted | ✅ Aprobado | 0.001 s |

### ArchitectureTest · Unidad · 2 casos

`com.parkingspot.ArchitectureTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| elDominioNoDependeDeFrameworksNiDeOtrasCapas | ✅ Aprobado | 0.048 s |
| laAplicacionNoDependeDeSpringNiDeLosAdaptadores | ✅ Aprobado | 0.013 s |

### CashPaymentServiceTest · Unidad · 10 casos

`com.parkingspot.application.CashPaymentServiceTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| solicitudSinClaveOImporte | ✅ Aprobado | 0.167 s |
| paseVigente | ✅ Aprobado | 0.016 s |
| pagoDuplicado | ✅ Aprobado | 0.014 s |
| idempotencia | ✅ Aprobado | 0.004 s |
| importeIncorrecto | ✅ Aprobado | 0.005 s |
| cobroAnuladoNoSePaga | ✅ Aprobado | 0.002 s |
| cobroInexistente | ✅ Aprobado | 0.004 s |
| claveConOtrosDatos | ✅ Aprobado | 0.003 s |
| trazabilidad | ✅ Aprobado | 0.007 s |
| pagoExitoso | ✅ Aprobado | 0.001 s |

### ChargeServiceTest · Unidad · 10 casos

`com.parkingspot.application.ChargeServiceTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| sinTarifa | ✅ Aprobado | 0.031 s |
| reingresoMismoDia | ✅ Aprobado | 0.005 s |
| consultaSinCobro | ✅ Aprobado | 0.004 s |
| motoExenta | ✅ Aprobado | 0.001 s |
| carroNocturno | ✅ Aprobado | 0.002 s |
| placaInvalida | ✅ Aprobado | 0.004 s |
| carroDiurno | ✅ Aprobado | 0.002 s |
| nuevoDiaOperativo | ✅ Aprobado | 0.002 s |
| solicitudesSimultaneas | ✅ Aprobado | 0.005 s |
| tarifaConservada | ✅ Aprobado | 0.002 s |

### ChargeStateTest · Unidad · 4 casos

`com.parkingspot.domain.ChargeStateTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| pendientePasaAPagado | ✅ Aprobado | 0.001 s |
| otrosEstadosRechazanElPago(ChargeStatus, Reason)[1] | ✅ Aprobado | 0.009 s |
| otrosEstadosRechazanElPago(ChargeStatus, Reason)[2] | ✅ Aprobado | 0.001 s |
| otrosEstadosRechazanElPago(ChargeStatus, Reason)[3] | ✅ Aprobado | 0.000 s |

### ConfidenceTest · Unidad · 12 casos

`com.parkingspot.domain.ConfidenceTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| rejectsMissingValue | ✅ Aprobado | 0.001 s |
| rejectsValuesOutsideRange(double)[1] | ✅ Aprobado | 0.003 s |
| rejectsValuesOutsideRange(double)[2] | ✅ Aprobado | 0.005 s |
| rejectsValuesOutsideRange(double)[3] | ✅ Aprobado | 0.001 s |
| rejectsValuesOutsideRange(double)[4] | ✅ Aprobado | 0.001 s |
| rejectsValuesOutsideRange(double)[5] | ✅ Aprobado | 0.001 s |
| rejectsValuesOutsideRange(double)[6] | ✅ Aprobado | 0.000 s |
| rejectsValuesOutsideRange(double)[7] | ✅ Aprobado | 0.000 s |
| acceptsValuesBetweenZeroAndOneInclusive(double)[1] | ✅ Aprobado | 0.001 s |
| acceptsValuesBetweenZeroAndOneInclusive(double)[2] | ✅ Aprobado | 0.001 s |
| acceptsValuesBetweenZeroAndOneInclusive(double)[3] | ✅ Aprobado | 0.001 s |
| acceptsValuesBetweenZeroAndOneInclusive(double)[4] | ✅ Aprobado | 0.001 s |

### ConfidenceThresholdTest · Unidad · 7 casos

`com.parkingspot.domain.ConfidenceThresholdTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| rejectsThresholdOutsideRange(double)[1] | ✅ Aprobado | 0.002 s |
| rejectsThresholdOutsideRange(double)[2] | ✅ Aprobado | 0.001 s |
| rejectsThresholdOutsideRange(double)[3] | ✅ Aprobado | 0.001 s |
| equalOrHigherConfidenceIsAccepted(double)[1] | ✅ Aprobado | 0.001 s |
| equalOrHigherConfidenceIsAccepted(double)[2] | ✅ Aprobado | 0.000 s |
| equalOrHigherConfidenceIsAccepted(double)[3] | ✅ Aprobado | 0.001 s |
| lowerConfidenceRequiresValidation | ✅ Aprobado | 0.001 s |

### EntryEventStoreMappingTest · Unidad · 2 casos

`com.parkingspot.infrastructure.adapter.out.persistence.EntryEventStoreMappingTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| storesConfidenceAsPercentageWithTwoDecimals | ✅ Aprobado | 0.002 s |
| mapsReadingSourceToSchemaOrigin | ✅ Aprobado | 0.003 s |

### EntryEventTest · Unidad · 4 casos

`com.parkingspot.domain.EntryEventTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| manualEventHasNoConfidenceAndKeepsItsUser | ✅ Aprobado | 0.003 s |
| rejectedFullEventKeepsTraceability | ✅ Aprobado | 0.001 s |
| authorizedEventKeepsPlateSiteTimeSourceAndUser | ✅ Aprobado | 0.000 s |
| manualEventCannotCarryReaderConfidence | ✅ Aprobado | 0.001 s |

### EntryReadingConfigTest · Unidad · 3 casos

`com.parkingspot.infrastructure.EntryReadingConfigTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| failsAtStartupWithThresholdOutsideRange | ✅ Aprobado | 0.019 s |
| readsThresholdFromEnvironmentVariable | ✅ Aprobado | 0.007 s |
| usesDefaultThresholdWhenVariableIsAbsent | ✅ Aprobado | 0.006 s |

### EntryReadingValidationServiceTest · Unidad · 7 casos

`com.parkingspot.application.EntryReadingValidationServiceTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| invalidFormatRequiresValidationEvenWithHighConfidence | ✅ Aprobado | 0.026 s |
| unknownOperatorCannotValidateOrConfirm | ✅ Aprobado | 0.005 s |
| lowConfidenceRequiresValidation | ✅ Aprobado | 0.002 s |
| invalidConfidenceIsStillRejected | ✅ Aprobado | 0.002 s |
| manualConfirmationRejectsInvalidPlate | ✅ Aprobado | 0.002 s |
| manualConfirmationIsMarkedManualWithOperator | ✅ Aprobado | 0.027 s |
| acceptsReadingAtOrAboveThreshold | ✅ Aprobado | 0.002 s |

### EntryRegistrationServiceTest · Unidad · 5 casos

`com.parkingspot.application.EntryRegistrationServiceTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| registersManualReadingWithOperatorAndNoConfidence | ✅ Aprobado | 0.218 s |
| readingThatRequiresValidationRegistersNothing | ✅ Aprobado | 0.012 s |
| doesNotSaveEventWhenOccupancyIsNotConfigured | ✅ Aprobado | 0.017 s |
| registersAcceptedAutomaticReadingWithServerTimeAndOccupiesSpot | ✅ Aprobado | 0.014 s |
| fullSiteRejectsEntryKeepsBarrierClosedAndStoresRejectedEvent | ✅ Aprobado | 0.003 s |

### ExitBarrierAdapterTest · Unidad · 2 casos

`com.parkingspot.application.ExitBarrierAdapterTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| abreLaBarreraDePruebaCuandoElCobroEstaPagado | ✅ Aprobado | 0.081 s |
| noAbreLaBarreraDePruebaCuandoElCobroEstaPendiente | ✅ Aprobado | 0.003 s |

### HistoryServiceTest · Unidad · 8 casos

`com.parkingspot.application.HistoryServiceTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| intervaloInvalido | ✅ Aprobado | 0.003 s |
| combinaFechaYEstado | ✅ Aprobado | 0.012 s |
| placaInvalida | ✅ Aprobado | 0.002 s |
| normalizaPlaca | ✅ Aprobado | 0.002 s |
| intervaloDeEventosEnHoraLocal | ✅ Aprobado | 0.006 s |
| valoresPorDefecto | ✅ Aprobado | 0.003 s |
| totalDePaginas | ✅ Aprobado | 0.002 s |
| paginacionInvalida | ✅ Aprobado | 0.004 s |

### OperatingDayCalculatorTest · Unidad · 7 casos

`com.parkingspot.domain.OperatingDayCalculatorTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| calculaElDiaOperativo(String, String)[1] | ✅ Aprobado | 0.004 s |
| calculaElDiaOperativo(String, String)[2] | ✅ Aprobado | 0.001 s |
| calculaElDiaOperativo(String, String)[3] | ✅ Aprobado | 0.002 s |
| calculaElDiaOperativo(String, String)[4] | ✅ Aprobado | 0.001 s |
| calculaElDiaOperativo(String, String)[5] | ✅ Aprobado | 0.002 s |
| usaLaZonaHorariaDeLaSede | ✅ Aprobado | 0.001 s |
| finDelDiaOperativo | ✅ Aprobado | 0.001 s |

### PassServiceTest · Unidad · 6 casos

`com.parkingspot.application.PassServiceTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| paseDeOtraSede | ✅ Aprobado | 0.012 s |
| paseVencido | ✅ Aprobado | 0.003 s |
| paseVigente | ✅ Aprobado | 0.001 s |
| placaInvalida | ✅ Aprobado | 0.002 s |
| normalizaPlaca | ✅ Aprobado | 0.001 s |
| sinPase | ✅ Aprobado | 0.003 s |

### PassTest · Unidad · 5 casos

`com.parkingspot.domain.PassTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| vencidoDesdeLasSeis | ✅ Aprobado | 0.001 s |
| otraSede | ✅ Aprobado | 0.001 s |
| vigenteElMismoDia | ✅ Aprobado | 0.001 s |
| vigenteAntesDeLasSeis | ✅ Aprobado | 0.001 s |
| reingresoSinCobro | ✅ Aprobado | 0.001 s |

### PlateClassifierTest · Unidad · 3 casos

`com.parkingspot.domain.PlateClassifierTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| classifiesMotorcyclePlate | ✅ Aprobado | 0.001 s |
| rejectsInvalidPlates | ✅ Aprobado | 0.001 s |
| classifiesAndNormalizesCarPlate | ✅ Aprobado | 0.001 s |

### PlateReadingServiceTest · Unidad · 3 casos

`com.parkingspot.application.PlateReadingServiceTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| normalizesAndClassifiesWhatTheReaderReturns | ✅ Aprobado | 0.004 s |
| propagatesInvalidPlateFromTheClassifier | ✅ Aprobado | 0.004 s |
| dependsOnlyOnThePortSoAnyReaderCanBePluggedIn | ✅ Aprobado | 0.002 s |

### SimulatedPlateReaderTest · Unidad · 2 casos

`com.parkingspot.infrastructure.SimulatedPlateReaderTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| rejectsOutOfRangeOrMissingConfidence | ✅ Aprobado | 0.002 s |
| returnsReceivedPlateAndConfidenceWithSimulatorSource | ✅ Aprobado | 0.001 s |

### TariffStrategyTest · Unidad · 5 casos

`com.parkingspot.domain.TariffStrategyTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| carroConTarifaCeroEsUnError | ✅ Aprobado | 0.002 s |
| motoConTarifaPositivaGeneraCobro | ✅ Aprobado | 0.006 s |
| elResolvedorDevuelveLaEstrategiaDeCadaTipo | ✅ Aprobado | 0.001 s |
| motoConTarifaCeroNoGeneraCobro | ✅ Aprobado | 0.001 s |
| carroPagaLaTarifa | ✅ Aprobado | 0.002 s |

### TimeSlotResolverTest · Unidad · 8 casos

`com.parkingspot.domain.TimeSlotResolverTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| resuelveLaFranja(String, TimeSlot)[1] | ✅ Aprobado | 0.004 s |
| resuelveLaFranja(String, TimeSlot)[2] | ✅ Aprobado | 0.001 s |
| resuelveLaFranja(String, TimeSlot)[3] | ✅ Aprobado | 0.000 s |
| resuelveLaFranja(String, TimeSlot)[4] | ✅ Aprobado | 0.001 s |
| resuelveLaFranja(String, TimeSlot)[5] | ✅ Aprobado | 0.002 s |
| resuelveLaFranja(String, TimeSlot)[6] | ✅ Aprobado | 0.001 s |
| resuelveLaFranja(String, TimeSlot)[7] | ✅ Aprobado | 0.001 s |
| sinFranjaConfiguradaEsUnErrorControlado | ✅ Aprobado | 0.001 s |

### TimeSlotScheduleTest · Unidad · 8 casos

`com.parkingspot.domain.TimeSlotScheduleTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| cadaHoraPerteneceAExactamenteUnaFranja(String, boolean, boolean)[1] | ✅ Aprobado | 0.004 s |
| cadaHoraPerteneceAExactamenteUnaFranja(String, boolean, boolean)[2] | ✅ Aprobado | 0.002 s |
| cadaHoraPerteneceAExactamenteUnaFranja(String, boolean, boolean)[3] | ✅ Aprobado | 0.000 s |
| cadaHoraPerteneceAExactamenteUnaFranja(String, boolean, boolean)[4] | ✅ Aprobado | 0.001 s |
| cadaHoraPerteneceAExactamenteUnaFranja(String, boolean, boolean)[5] | ✅ Aprobado | 0.000 s |
| cadaHoraPerteneceAExactamenteUnaFranja(String, boolean, boolean)[6] | ✅ Aprobado | 0.001 s |
| cadaHoraPerteneceAExactamenteUnaFranja(String, boolean, boolean)[7] | ✅ Aprobado | 0.001 s |
| cadaHoraPerteneceAExactamenteUnaFranja(String, boolean, boolean)[8] | ✅ Aprobado | 0.001 s |

### PlateReadingControllerTest · Spring MVC (sin BD) · 17 casos

`com.parkingspot.infrastructure.PlateReadingControllerTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| acceptsConfidenceBoundaries(double)[1] | ✅ Aprobado | 0.008 s |
| acceptsConfidenceBoundaries(double)[2] | ✅ Aprobado | 0.004 s |
| acceptsLowConfidenceBecauseThresholdBelongsToHu11 | ✅ Aprobado | 0.004 s |
| rejectsOperatorWithoutCsrfToken | ✅ Aprobado | 0.003 s |
| returnsNormalizedPlateTypeConfidenceAndSimulatorSource | ✅ Aprobado | 0.005 s |
| rejectsInvalidConfidence(String)[1] | ✅ Aprobado | 0.004 s |
| rejectsInvalidConfidence(String)[2] | ✅ Aprobado | 0.003 s |
| rejectsInvalidConfidence(String)[3] | ✅ Aprobado | 0.004 s |
| rejectsInvalidConfidence(String)[4] | ✅ Aprobado | 0.004 s |
| rejectsInvalidPlateWithSameErrorFormat(String)[1] | ✅ Aprobado | 0.003 s |
| rejectsInvalidPlateWithSameErrorFormat(String)[2] | ✅ Aprobado | 0.005 s |
| rejectsAdminRole | ✅ Aprobado | 0.003 s |
| rejectsMalformedRequestWithSameErrorFormat(String)[1] | ✅ Aprobado | 0.003 s |
| rejectsMalformedRequestWithSameErrorFormat(String)[2] | ✅ Aprobado | 0.003 s |
| rejectsMalformedRequestWithSameErrorFormat(String)[3] | ✅ Aprobado | 0.003 s |
| rejectsMalformedRequestWithSameErrorFormat(String)[4] | ✅ Aprobado | 0.004 s |
| rejectsRequestWithoutSession | ✅ Aprobado | 0.002 s |

### AuthenticationTest · H2 · 10 casos

`com.parkingspot.AuthenticationTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| wrongPasswordUnknownUserAndDisabledAccountGetTheSameError | ✅ Aprobado | 0.603 s |
| newAccountsStoreOnlyBcryptHashes | ✅ Aprobado | 0.133 s |
| logoutInvalidatesSessionAndReplayIsUnauthorized | ✅ Aprobado | 0.212 s |
| protectedApiRequiresSessionEvenForPostWithoutCsrf | ✅ Aprobado | 0.040 s |
| authenticatesPersistedUsersAndReturnsOnlyUsernameAndRole(Role)[1] | ✅ Aprobado | 0.171 s |
| authenticatesPersistedUsersAndReturnsOnlyUsernameAndRole(Role)[2] | ✅ Aprobado | 0.149 s |
| csrfIsRequiredForLoginAndAuthenticatedWrites | ✅ Aprobado | 0.138 s |
| onlyAdminMayReachTariffHandlerAndOnlyOperatorMayClassify(Role)[1] | ✅ Aprobado | 0.177 s |
| onlyAdminMayReachTariffHandlerAndOnlyOperatorMayClassify(Role)[2] | ✅ Aprobado | 0.137 s |
| allowsCredentialedPreflightOnlyFromConfiguredFrontend | ✅ Aprobado | 0.011 s |

### CapacityValidationIntegrationTest · H2 · 4 casos

`com.parkingspot.infrastructure.CapacityValidationIntegrationTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| manualEntryAlsoRespectsCapacity | ✅ Aprobado | 0.122 s |
| freedSpotAllowsEntryAgain | ✅ Aprobado | 0.103 s |
| capacityIsValidatedPerVehicleType | ✅ Aprobado | 0.090 s |
| fullSiteAnswersLlenoAndKeepsBarrierClosed | ✅ Aprobado | 0.087 s |

### ConcurrentEntryTest · H2 · 5 casos

`com.parkingspot.infrastructure.ConcurrentEntryTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| simultaneousEntriesNeverExceedCapacity()[1] | ✅ Aprobado | 0.212 s |
| simultaneousEntriesNeverExceedCapacity()[2] | ✅ Aprobado | 0.130 s |
| simultaneousEntriesNeverExceedCapacity()[3] | ✅ Aprobado | 0.118 s |
| simultaneousEntriesNeverExceedCapacity()[4] | ✅ Aprobado | 0.097 s |
| simultaneousEntriesNeverExceedCapacity()[5] | ✅ Aprobado | 0.093 s |

### EntryApiDocsTest · H2 · 2 casos

`com.parkingspot.infrastructure.EntryApiDocsTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| documentsEntryEndpoints(String)[1] | ✅ Aprobado | 0.747 s |
| documentsEntryEndpoints(String)[2] | ✅ Aprobado | 0.021 s |

### EntryReadingControllerTest · H2 · 10 casos

`com.parkingspot.infrastructure.EntryReadingControllerTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| acceptedAutomaticReadingContinuesToEntry | ✅ Aprobado | 0.117 s |
| thresholdBoundary | ✅ Aprobado | 0.103 s |
| enforcesOperatorRoleSessionAndCsrf(String)[1] | ✅ Aprobado | 0.089 s |
| enforcesOperatorRoleSessionAndCsrf(String)[2] | ✅ Aprobado | 0.081 s |
| invalidFormatRequiresValidation | ✅ Aprobado | 0.105 s |
| inactiveOperatorIsForbidden | ✅ Aprobado | 0.075 s |
| noEntryIsRegisteredWithoutRequiredConfirmation | ✅ Aprobado | 0.080 s |
| manualConfirmationRejectsInvalidPlate | ✅ Aprobado | 0.073 s |
| invalidConfidenceIsBadRequest | ✅ Aprobado | 0.069 s |
| manualConfirmationIsMarkedManualWithOperator | ✅ Aprobado | 0.075 s |

### EntryRegistrationIntegrationTest · H2 · 5 casos

`com.parkingspot.infrastructure.EntryRegistrationIntegrationTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| manualEntryIsStoredAsManualWithUserAndNoConfidence | ✅ Aprobado | 0.078 s |
| nothingIsStoredWhenOccupancyIsNotConfigured | ✅ Aprobado | 0.070 s |
| ignoresClientProvidedTime | ✅ Aprobado | 0.078 s |
| occupancyIsRolledBackWhenEventCannotBeStored | ✅ Aprobado | 0.085 s |
| automaticEntryIsStoredWithSiteServerTimeAndSourceAndUpdatesOccupancy | ✅ Aprobado | 0.078 s |

### PlateReadingApiDocsTest · H2 · 1 casos

`com.parkingspot.infrastructure.PlateReadingApiDocsTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| documentsSimulatorEndpointInOpenApi | ✅ Aprobado | 0.523 s |

### PlatformApiTest · H2 · 28 casos

`com.parkingspot.infrastructure.PlatformApiTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| clasificaCarro | ✅ Aprobado | 0.007 s |
| formatoInvalido(String)[1] | ✅ Aprobado | 0.011 s |
| formatoInvalido(String)[2] | ✅ Aprobado | 0.009 s |
| formatoInvalido(String)[3] | ✅ Aprobado | 0.006 s |
| formatoInvalido(String)[4] | ✅ Aprobado | 0.006 s |
| cadaEndpointEstaDocumentado(String, String)[1] | ✅ Aprobado | 0.014 s |
| cadaEndpointEstaDocumentado(String, String)[2] | ✅ Aprobado | 0.011 s |
| cadaEndpointEstaDocumentado(String, String)[3] | ✅ Aprobado | 0.013 s |
| cadaEndpointEstaDocumentado(String, String)[4] | ✅ Aprobado | 0.012 s |
| cadaEndpointEstaDocumentado(String, String)[5] | ✅ Aprobado | 0.013 s |
| cadaEndpointEstaDocumentado(String, String)[6] | ✅ Aprobado | 0.011 s |
| cadaEndpointEstaDocumentado(String, String)[7] | ✅ Aprobado | 0.030 s |
| cadaEndpointEstaDocumentado(String, String)[8] | ✅ Aprobado | 0.013 s |
| cadaEndpointEstaDocumentado(String, String)[9] | ✅ Aprobado | 0.011 s |
| cadaEndpointEstaDocumentado(String, String)[10] | ✅ Aprobado | 0.012 s |
| cadaEndpointEstaDocumentado(String, String)[11] | ✅ Aprobado | 0.013 s |
| cadaEndpointEstaDocumentado(String, String)[12] | ✅ Aprobado | 0.012 s |
| cadaEndpointEstaDocumentado(String, String)[13] | ✅ Aprobado | 0.012 s |
| cadaEndpointEstaDocumentado(String, String)[14] | ✅ Aprobado | 0.011 s |
| cadaEndpointEstaDocumentado(String, String)[15] | ✅ Aprobado | 0.009 s |
| cadaEndpointEstaDocumentado(String, String)[16] | ✅ Aprobado | 0.008 s |
| cadaEndpointEstaDocumentado(String, String)[17] | ✅ Aprobado | 0.009 s |
| cadaEndpointEstaDocumentado(String, String)[18] | ✅ Aprobado | 0.007 s |
| cadaEndpointEstaDocumentado(String, String)[19] | ✅ Aprobado | 0.007 s |
| cadaEndpointEstaDocumentado(String, String)[20] | ✅ Aprobado | 0.007 s |
| healthEsPublicoYUp | ✅ Aprobado | 0.037 s |
| clasificaMoto | ✅ Aprobado | 0.005 s |
| swaggerYOpenApiPublicos | ✅ Aprobado | 0.025 s |

### SecurityMatrixTest · H2 · 47 casos

`com.parkingspot.infrastructure.SecurityMatrixTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| rolSinPermiso403(HttpMethod, String, String)[1] | ✅ Aprobado | 0.006 s |
| rolSinPermiso403(HttpMethod, String, String)[2] | ✅ Aprobado | 0.004 s |
| rolSinPermiso403(HttpMethod, String, String)[3] | ✅ Aprobado | 0.004 s |
| rolSinPermiso403(HttpMethod, String, String)[4] | ✅ Aprobado | 0.004 s |
| rolSinPermiso403(HttpMethod, String, String)[5] | ✅ Aprobado | 0.004 s |
| rolSinPermiso403(HttpMethod, String, String)[6] | ✅ Aprobado | 0.003 s |
| rolSinPermiso403(HttpMethod, String, String)[7] | ✅ Aprobado | 0.005 s |
| rolSinPermiso403(HttpMethod, String, String)[8] | ✅ Aprobado | 0.004 s |
| rolSinPermiso403(HttpMethod, String, String)[9] | ✅ Aprobado | 0.003 s |
| rolSinPermiso403(HttpMethod, String, String)[10] | ✅ Aprobado | 0.003 s |
| rolSinPermiso403(HttpMethod, String, String)[11] | ✅ Aprobado | 0.005 s |
| rolSinPermiso403(HttpMethod, String, String)[12] | ✅ Aprobado | 0.005 s |
| rolSinPermiso403(HttpMethod, String, String)[13] | ✅ Aprobado | 0.004 s |
| rolSinPermiso403(HttpMethod, String, String)[14] | ✅ Aprobado | 0.005 s |
| rolSinPermiso403(HttpMethod, String, String)[15] | ✅ Aprobado | 0.004 s |
| rolSinPermiso403(HttpMethod, String, String)[16] | ✅ Aprobado | 0.004 s |
| rutaNoDeclaradaSeNiega(HttpMethod, String)[1] | ✅ Aprobado | 0.007 s |
| rutaNoDeclaradaSeNiega(HttpMethod, String)[2] | ✅ Aprobado | 0.009 s |
| rutaNoDeclaradaSeNiega(HttpMethod, String)[3] | ✅ Aprobado | 0.010 s |
| rutaNoDeclaradaSeNiega(HttpMethod, String)[4] | ✅ Aprobado | 0.008 s |
| sinSesion401(HttpMethod, String, String)[1] | ✅ Aprobado | 0.005 s |
| sinSesion401(HttpMethod, String, String)[2] | ✅ Aprobado | 0.005 s |
| sinSesion401(HttpMethod, String, String)[3] | ✅ Aprobado | 0.009 s |
| sinSesion401(HttpMethod, String, String)[4] | ✅ Aprobado | 0.006 s |
| sinSesion401(HttpMethod, String, String)[5] | ✅ Aprobado | 0.005 s |
| sinSesion401(HttpMethod, String, String)[6] | ✅ Aprobado | 0.006 s |
| sinSesion401(HttpMethod, String, String)[7] | ✅ Aprobado | 0.005 s |
| sinSesion401(HttpMethod, String, String)[8] | ✅ Aprobado | 0.006 s |
| sinSesion401(HttpMethod, String, String)[9] | ✅ Aprobado | 0.005 s |
| sinSesion401(HttpMethod, String, String)[10] | ✅ Aprobado | 0.011 s |
| sinSesion401(HttpMethod, String, String)[11] | ✅ Aprobado | 0.006 s |
| sinSesion401(HttpMethod, String, String)[12] | ✅ Aprobado | 0.005 s |
| sinSesion401(HttpMethod, String, String)[13] | ✅ Aprobado | 0.007 s |
| sinSesion401(HttpMethod, String, String)[14] | ✅ Aprobado | 0.005 s |
| sinSesion401(HttpMethod, String, String)[15] | ✅ Aprobado | 0.003 s |
| sinSesion401(HttpMethod, String, String)[16] | ✅ Aprobado | 0.004 s |
| sinSesion401(HttpMethod, String, String)[17] | ✅ Aprobado | 0.003 s |
| sinSesion401(HttpMethod, String, String)[18] | ✅ Aprobado | 0.002 s |
| sinSesion401(HttpMethod, String, String)[19] | ✅ Aprobado | 0.003 s |
| escrituraSinCsrf403(HttpMethod, String, String)[1] | ✅ Aprobado | 0.005 s |
| escrituraSinCsrf403(HttpMethod, String, String)[2] | ✅ Aprobado | 0.004 s |
| escrituraSinCsrf403(HttpMethod, String, String)[3] | ✅ Aprobado | 0.004 s |
| escrituraSinCsrf403(HttpMethod, String, String)[4] | ✅ Aprobado | 0.004 s |
| escrituraSinCsrf403(HttpMethod, String, String)[5] | ✅ Aprobado | 0.005 s |
| escrituraSinCsrf403(HttpMethod, String, String)[6] | ✅ Aprobado | 0.003 s |
| escrituraSinCsrf403(HttpMethod, String, String)[7] | ✅ Aprobado | 0.003 s |
| escrituraSinCsrf403(HttpMethod, String, String)[8] | ✅ Aprobado | 0.004 s |

### AcademicScenariosTest · Docker · 7 casos

`com.parkingspot.infrastructure.AcademicScenariosTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| escenario3SalidaSinPagoMantieneLaBarreraCerrada | ✅ Aprobado | 0.268 s |
| escenario1MotoIngresaYSaleSinCobro | ✅ Aprobado | 0.061 s |
| escenario4PagoSalidaYReingresoSinNuevoCobro | ✅ Aprobado | 0.170 s |
| escenario5BajaConfianzaConValidacionManual | ✅ Aprobado | 0.049 s |
| escenario7TarifaNocturnaMayorOIgualSeRechaza | ✅ Aprobado | 0.072 s |
| escenario2CarroDiurnoGeneraCobroPendiente | ✅ Aprobado | 0.045 s |
| escenario6ParqueaderoLlenoRechazaIngreso | ✅ Aprobado | 0.048 s |

### AdministrationApiTest · Docker · 2 casos

`com.parkingspot.infrastructure.AdministrationApiTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| historyCombinesInclusiveDatesPlateAndChargeStatusForAdministrators | ✅ Aprobado | 0.161 s |
| onlyAdminCanSaveAndOnlyFutureChargesUseTheNewTariff | ✅ Aprobado | 0.177 s |

### ChargeApiTest · Docker · 8 casos

`com.parkingspot.infrastructure.ChargeApiTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| sinTarifa | ✅ Aprobado | 0.050 s |
| concurrencia | ✅ Aprobado | 0.042 s |
| carroDiurnoGeneraCobro | ✅ Aprobado | 0.028 s |
| motoExenta | ✅ Aprobado | 0.031 s |
| placaInvalida | ✅ Aprobado | 0.026 s |
| soloOperador | ✅ Aprobado | 0.019 s |
| tarifaConservada | ✅ Aprobado | 0.038 s |
| reingresoNoDuplica | ✅ Aprobado | 0.034 s |

### DatabaseMigrationTest · Docker · 12 casos

`com.parkingspot.infrastructure.persistence.DatabaseMigrationTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| migracionesCreanTodasLasTablas | ✅ Aprobado | 0.026 s |
| cobroDuplicadoEsRechazado | ✅ Aprobado | 0.013 s |
| mismaPlacaOtroDiaEsPermitida | ✅ Aprobado | 0.013 s |
| datosInicialesCumplenLasReglas | ✅ Aprobado | 0.012 s |
| ocupacionMayorQueCapacidadEsRechazada | ✅ Aprobado | 0.012 s |
| cobroConSedeInexistenteEsRechazado | ✅ Aprobado | 0.011 s |
| cobroPagadoSinVigenciaEsRechazado | ✅ Aprobado | 0.011 s |
| placaInvalidaEsRechazada | ✅ Aprobado | 0.010 s |
| pagoDuplicadoEsRechazado | ✅ Aprobado | 0.017 s |
| pagoConCobroInexistenteEsRechazado | ✅ Aprobado | 0.017 s |
| usuarioConRolInexistenteEsRechazado | ✅ Aprobado | 0.015 s |
| flywayRegistraLasMigraciones | ✅ Aprobado | 0.009 s |

### ExitApiTest · Docker · 20 casos

`com.parkingspot.infrastructure.ExitApiTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| auditFailureAfterReleaseRollsBackTheExitAndTheOccupancy | ✅ Aprobado | 0.126 s |
| cpE02ExemptionSurvivesTariffChange | ✅ Aprobado | 0.057 s |
| chargeLookupFailureIsAuditedAndReturns503 | ✅ Aprobado | 0.041 s |
| missingOrEmptyOccupancyRollsBackTheExitAndKeepsTheEntry(boolean)[1] | ✅ Aprobado | 0.065 s |
| missingOrEmptyOccupancyRollsBackTheExitAndKeepsTheEntry(boolean)[2] | ✅ Aprobado | 0.053 s |
| closedChargeDoesNotSatisfyTheObligation(String)[1] | ✅ Aprobado | 0.043 s |
| closedChargeDoesNotSatisfyTheObligation(String)[2] | ✅ Aprobado | 0.046 s |
| requiresSessionOperatorAndCsrf | ✅ Aprobado | 0.014 s |
| cpX01PendingChargeDeniesAndAudits | ✅ Aprobado | 0.053 s |
| simultaneousRequestsForTheSameEntryCanCommitOnlyOneExit | ✅ Aprobado | 0.093 s |
| paidExitReleasesExactlyOneOfTenAndAuditsTheDecision | ✅ Aprobado | 0.317 s |
| documentsTheContract | ✅ Aprobado | 0.404 s |
| latestPlateIgnoresARejectedEntry | ✅ Aprobado | 0.049 s |
| missingEntryDeniesEvenWithPaidCharge | ✅ Aprobado | 0.033 s |
| occupancyOnlyIncludesTheSessionOperatorsSite | ✅ Aprobado | 0.037 s |
| repeatedExitDoesNotReleaseAnotherSpotAndThePlateCanReenter | ✅ Aprobado | 0.103 s |
| failureInsertingTheEffectiveExitIsAuditedAndKeepsTheSpotOccupied | ✅ Aprobado | 0.052 s |
| entryInAnotherSiteDoesNotAuthorize | ✅ Aprobado | 0.023 s |
| simultaneousEntriesOccupyOneSpotUntilEffectiveExit | ✅ Aprobado | 0.071 s |
| exemptionIsNotInferredFromNewTariffOrAnotherOperatingDay | ✅ Aprobado | 0.116 s |

### HistoryApiTest · Docker · 8 casos

`com.parkingspot.infrastructure.HistoryApiTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| filtroPorFechaYEstado | ✅ Aprobado | 0.074 s |
| filtroPorPlaca | ✅ Aprobado | 0.061 s |
| filtrosInvalidos | ✅ Aprobado | 0.067 s |
| listadoDeEventos | ✅ Aprobado | 0.040 s |
| consultaSinResultados | ✅ Aprobado | 0.032 s |
| listadoDeCobros | ✅ Aprobado | 0.028 s |
| paginacion | ✅ Aprobado | 0.027 s |
| controlDeAcceso | ✅ Aprobado | 0.022 s |

### ParkingConfigurationApiTest · Docker · 17 casos

`com.parkingspot.infrastructure.ParkingConfigurationApiTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| importesInvalidos | ✅ Aprobado | 0.060 s |
| franjasConfigurables | ✅ Aprobado | 0.077 s |
| guardarEsAtomico | ✅ Aprobado | 0.047 s |
| motoExentaEsValida | ✅ Aprobado | 0.044 s |
| sedeInexistente | ✅ Aprobado | 0.023 s |
| cuposNegativosSeRechazan(int)[1] | ✅ Aprobado | 0.030 s |
| cuposNegativosSeRechazan(int)[2] | ✅ Aprobado | 0.029 s |
| soloElAdministrador | ✅ Aprobado | 0.029 s |
| cupoEnCeroEsValido | ✅ Aprobado | 0.036 s |
| leeLaConfiguracionInicial | ✅ Aprobado | 0.028 s |
| modificaTarifasPorTipoYFranja | ✅ Aprobado | 0.039 s |
| rechazaNocturnaMayorOIgual(String, int, int)[1] | ✅ Aprobado | 0.031 s |
| rechazaNocturnaMayorOIgual(String, int, int)[2] | ✅ Aprobado | 0.029 s |
| rechazaNocturnaMayorOIgual(String, int, int)[3] | ✅ Aprobado | 0.045 s |
| rechazaNocturnaMayorOIgual(String, int, int)[4] | ✅ Aprobado | 0.043 s |
| rechazaNocturnaMayorOIgual(String, int, int)[5] | ✅ Aprobado | 0.031 s |
| franjasIgualesSeRechazan | ✅ Aprobado | 0.026 s |

### PassApiTest · Docker · 5 casos

`com.parkingspot.infrastructure.PassApiTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| paseDeOtraSede | ✅ Aprobado | 0.079 s |
| autorizacion | ✅ Aprobado | 0.024 s |
| paseVencido | ✅ Aprobado | 0.022 s |
| reingresoSinCobro | ✅ Aprobado | 0.063 s |
| sinPase | ✅ Aprobado | 0.044 s |

### PaymentApiTest · Docker · 8 casos

`com.parkingspot.infrastructure.PaymentApiTest`

| Caso | Obtenido | Tiempo |
|---|---|---|
| concurrencia | ✅ Aprobado | 0.146 s |
| autorizacion | ✅ Aprobado | 0.032 s |
| pagoDuplicado | ✅ Aprobado | 0.047 s |
| idempotencia | ✅ Aprobado | 0.040 s |
| erroresControlados | ✅ Aprobado | 0.035 s |
| claveConOtrosDatos | ✅ Aprobado | 0.036 s |
| trazabilidad | ✅ Aprobado | 0.035 s |
| pagoExitoso | ✅ Aprobado | 0.028 s |

## Frontend (73 casos: 73 aprobados, 0 fallidos)

### AppRouter.test.tsx · jsdom · 26 casos

| Caso | Obtenido | Tiempo |
|---|---|---|
| rutas y sesión › muestra la bienvenida con el enlace de inicio de sesión | ✅ Aprobado | 0.088 s |
| rutas y sesión › envía a iniciar sesión desde /operator ante un 401 al recuperar la sesión | ✅ Aprobado | 0.037 s |
| rutas y sesión › envía a iniciar sesión desde /admin ante un 401 al recuperar la sesión | ✅ Aprobado | 0.026 s |
| rutas y sesión › envía a iniciar sesión desde /vehicles/classify ante un 401 al recuperar la sesión | ✅ Aprobado | 0.018 s |
| rutas y sesión › envía a iniciar sesión desde /classify ante un 401 al recuperar la sesión | ✅ Aprobado | 0.029 s |
| rutas y sesión › recupera la sesión del operador y solo enlaza sus módulos | ✅ Aprobado | 0.075 s |
| rutas y sesión › recupera la sesión de administrador y muestra sus módulos | ✅ Aprobado | 0.020 s |
| rutas y sesión › bloquea el acceso directo al panel del otro rol y enlaza al panel propio | ✅ Aprobado | 0.013 s |
| rutas y sesión › gestiona HTTP 401 al clasificar con una sesión previamente válida | ✅ Aprobado | 0.046 s |
| rutas y sesión › gestiona HTTP 403 al clasificar con una sesión previamente válida | ✅ Aprobado | 0.020 s |
| rutas y sesión › abre el panel de { username: 'operador', role: 'OPERADOR' } al iniciar sesión y vuelve a login al salir | ✅ Aprobado | 0.079 s |
| rutas y sesión › abre el panel de { username: 'administrador', role: 'ADMIN' } al iniciar sesión y vuelve a login al salir | ✅ Aprobado | 0.038 s |
| carril de salida › CP-HU-18-E01 muestra el estado y el motivo devuelto al solicitar salida | ✅ Aprobado | 0.072 s |
| carril de salida › CP-HU-18-E02 muestra el estado y el motivo devuelto al solicitar salida | ✅ Aprobado | 0.056 s |
| carril de salida › CP-HU-18-X01 muestra el estado y el motivo devuelto al solicitar salida | ✅ Aprobado | 0.073 s |
| carril de salida › CP-HU-18-X02 limpia la apertura al reintentar, evita duplicados y muestra el fallo de conexión | ✅ Aprobado | 0.082 s |
| carril de salida › cierra la barrera si el backend responde con un error de auditoría | ✅ Aprobado | 0.039 s |
| carril de salida › exige una autorización explícita del backend | ✅ Aprobado | 0.049 s |
| carril de salida › muestra los cupos persistidos y los consulta otra vez tras una salida autorizada | ✅ Aprobado | 0.083 s |
| carril de salida › conserva la disponibilidad ante una salida rechazada | ✅ Aprobado | 0.052 s |
| carril de salida › conserva el resultado de la salida si falla la consulta posterior de cupos y permite reintentar | ✅ Aprobado | 0.044 s |
| carril de salida › bloquea la ruta a un administrador | ✅ Aprobado | 0.005 s |
| registro manual de ingreso › permite registrar el ingreso desde el panel y continuar a cobros | ✅ Aprobado | 0.030 s |
| registro manual de ingreso › mantiene la última placa autorizada después de un rechazo y al recargar el panel | ✅ Aprobado | 0.032 s |
| registro manual de ingreso › bloquea el acceso al ingreso para un administrador | ✅ Aprobado | 0.005 s |
| registro manual de ingreso › enlaza al carril de entrada cuando la validación de salida no encuentra un ingreso | ✅ Aprobado | 0.033 s |

### CashierPage.test.tsx · jsdom · 4 casos

| Caso | Obtenido | Tiempo |
|---|---|---|
| caja: pago en efectivo (HU-16) › registra el pago del cobro pendiente y muestra el comprobante con el pase | ✅ Aprobado | 0.156 s |
| caja: pago en efectivo (HU-16) › reutiliza la misma clave de idempotencia al reintentar tras un error | ✅ Aprobado | 0.068 s |
| caja: pago en efectivo (HU-16) › para un cobro ya pagado muestra el pago registrado y no ofrece pagar otra vez (CA-03 y CA-06) | ✅ Aprobado | 0.070 s |
| caja: pago en efectivo (HU-16) › el panel de operador enlaza a la caja | ✅ Aprobado | 0.042 s |

### ChargePage.test.tsx · jsdom · 4 casos

| Caso | Obtenido | Tiempo |
|---|---|---|
| pantalla de cobro del día › el panel de operador enlaza a la pantalla de cobros | ✅ Aprobado | 0.128 s |
| pantalla de cobro del día › muestra el valor y el estado reales del cobro calculado por el backend (CA-07) | ✅ Aprobado | 0.096 s |
| pantalla de cobro del día › informa cuando una moto queda exenta | ✅ Aprobado | 0.056 s |
| pantalla de cobro del día › muestra el error controlado cuando no hay tarifa | ✅ Aprobado | 0.037 s |

### ClassifyVehiclePage.test.tsx · jsdom · 2 casos

| Caso | Obtenido | Tiempo |
|---|---|---|
| clasificar vehículo › muestra la placa normalizada y el tipo que calculó el backend, con el enlace al ingreso | ✅ Aprobado | 0.150 s |
| clasificar vehículo › muestra el mensaje del backend ante un formato inválido | ✅ Aprobado | 0.034 s |

### EntryLanePage.test.tsx · jsdom · 5 casos

| Caso | Obtenido | Tiempo |
|---|---|---|
| carril de entrada › registra la placa digitada (origen MANUAL) y confirma el ingreso autorizado | ✅ Aprobado | 0.153 s |
| carril de entrada › HU-13: sin cupo muestra LLENO y no confirma el ingreso | ✅ Aprobado | 0.042 s |
| carril de entrada › una placa con ingreso activo se rechaza con su motivo | ✅ Aprobado | 0.047 s |
| carril de entrada › no confirma el ingreso si el backend no autoriza explícitamente la barrera | ✅ Aprobado | 0.048 s |
| carril de entrada › una placa inválida muestra el mensaje del backend | ✅ Aprobado | 0.037 s |

### HistoryPage.test.tsx · jsdom · 6 casos

| Caso | Obtenido | Tiempo |
|---|---|---|
| historial de eventos y cobros (HU-20) › lista los eventos filtrados por placa (CA-01, CA-03) | ✅ Aprobado | 0.163 s |
| historial de eventos y cobros (HU-20) › combina fecha y estado en los cobros y muestra importe y estado (CA-02, CA-04, CA-05) | ✅ Aprobado | 0.126 s |
| historial de eventos y cobros (HU-20) › pagina hacia la siguiente página con los mismos filtros | ✅ Aprobado | 0.101 s |
| historial de eventos y cobros (HU-20) › muestra un estado vacío sin errores (CA-06) | ✅ Aprobado | 0.032 s |
| historial de eventos y cobros (HU-20) › muestra el mensaje del backend ante un filtro inválido (CP-HU-20-X01) | ✅ Aprobado | 0.032 s |
| historial de eventos y cobros (HU-20) › el administrador también tiene la consulta en su panel | ✅ Aprobado | 0.018 s |

### LoginPage.test.tsx · jsdom · 6 casos

| Caso | Obtenido | Tiempo |
|---|---|---|
| inicio de sesión › un login válido de operador envía las credenciales con CSRF y abre el panel de operador | ✅ Aprobado | 0.157 s |
| inicio de sesión › un login válido de administrador abre el panel de administración | ✅ Aprobado | 0.059 s |
| inicio de sesión › credenciales inválidas muestran el error, no entran al panel y limpian la contraseña | ✅ Aprobado | 0.039 s |
| inicio de sesión › un fallo de conexión muestra un mensaje para reintentar | ✅ Aprobado | 0.042 s |
| inicio de sesión › mientras se consulta la sesión el botón está deshabilitado y lo indica | ✅ Aprobado | 0.023 s |
| inicio de sesión › un usuario con sesión activa que abre /login va directo a su panel | ✅ Aprobado | 0.039 s |

### ParkingConfigurationPage.test.tsx · jsdom · 6 casos

| Caso | Obtenido | Tiempo |
|---|---|---|
| configuración del parqueadero › carga la sede, las franjas, las tarifas y los cupos guardados (CA-01, CA-03) | ✅ Aprobado | 0.070 s |
| configuración del parqueadero › guarda la nueva tarifa y confirma el resultado (CA-01) | ✅ Aprobado | 0.048 s |
| configuración del parqueadero › muestra el rechazo del backend cuando la nocturna es mayor o igual a la diurna (CA-02, escenario 7) | ✅ Aprobado | 0.043 s |
| configuración del parqueadero › muestra el rechazo de un cupo inválido (CA-04) | ✅ Aprobado | 0.034 s |
| configuración del parqueadero › informa el fallo al cargar la configuración | ✅ Aprobado | 0.016 s |
| configuración del parqueadero › un operador no puede abrir la configuración (CA-05) | ✅ Aprobado | 0.008 s |

### PassPage.test.tsx · jsdom · 5 casos

| Caso | Obtenido | Tiempo |
|---|---|---|
| consulta de pase (HU-17) › muestra el estado, la vigencia y que el reingreso no tiene cobro (CA-06) | ✅ Aprobado | 0.098 s |
| consulta de pase (HU-17) › un pase vencido indica que el reingreso genera cobro (CA-03) | ✅ Aprobado | 0.022 s |
| consulta de pase (HU-17) › un pase de otra sede no habilita la exención (CA-05) | ✅ Aprobado | 0.027 s |
| consulta de pase (HU-17) › muestra el mensaje del backend cuando la placa no tiene pase | ✅ Aprobado | 0.022 s |
| consulta de pase (HU-17) › el panel de operador enlaza a la consulta de pases | ✅ Aprobado | 0.020 s |

### chargesApi.test.ts · jsdom · 3 casos

| Caso | Obtenido | Tiempo |
|---|---|---|
| cliente de cobros › registra el cobro del ingreso enviando la placa con token CSRF | ✅ Aprobado | 0.008 s |
| cliente de cobros › consulta el cobro de hoy con la placa en la URL | ✅ Aprobado | 0.004 s |
| cliente de cobros › propaga el mensaje de error del backend | ✅ Aprobado | 0.003 s |

### classificationApi.test.ts · jsdom · 2 casos

| Caso | Obtenido | Tiempo |
|---|---|---|
| cliente de clasificación › envía la placa y devuelve la clasificación del backend | ✅ Aprobado | 0.012 s |
| cliente de clasificación › muestra el error informado por el backend | ✅ Aprobado | 0.004 s |

### httpClient.test.ts · jsdom · 4 casos

| Caso | Obtenido | Tiempo |
|---|---|---|
| cliente HTTP › usa la URL base configurada y envía JSON | ✅ Aprobado | 0.012 s |
| cliente HTTP › la URL base no termina en barra | ✅ Aprobado | 0.001 s |
| cliente HTTP › convierte la respuesta de error del backend en ApiError | ✅ Aprobado | 0.002 s |
| cliente HTTP › usa un mensaje por defecto si el error no trae cuerpo JSON | ✅ Aprobado | 0.003 s |
