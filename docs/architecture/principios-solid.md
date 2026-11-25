# Principios SOLID (HU-22)

Este documento muestra cómo se aplica cada principio SOLID en el backend de Parking Spot, con fragmentos reales del código y el problema que evita. Para los cinco principios se revisaron las clases, los puertos y los adaptadores existentes. Las violaciones que se encontraron se corrigieron en esta historia y aparecen marcadas como **Refactor HU-22**.

Todas las rutas son relativas a `backend/src/main/java/com/parkingspot/`. Los patrones que apoyan estos principios (Strategy, Adapter, Repository y State) se documentan en [`patrones-diseno.md`](patrones-diseno.md).

| Principio | Dónde se evidencia | Refactor HU-22 | Prueba |
| --- | --- | --- | --- |
| SRP | Controladores que solo traducen HTTP; servicios con un caso de uso cada uno | `LatestEntryController` y `ParkingConfigurationStore` | `LatestEntryServiceTest`, `AdministrationApiTest`, `ExitApiTest` |
| OCP | Estrategias de tarifa | — | `TariffStrategyTest` |
| LSP | Implementaciones de `PlateReaderPort`, `ExitBarrier`, `ChargeRepository` y `ChargeState` | — | `PlateReadingServiceTest`, `ExitBarrierAdapterTest`, `CashPaymentServiceTest` |
| ISP | Puertos pequeños por cliente | `OccupancyRepository` dividido en tres interfaces | `EntryRegistrationServiceTest`, `ExitBarrierAdapterTest`, `ExitApiTest` |
| DIP | Casos de uso que dependen de puertos, y configuración que conecta los adaptadores | `LatestEntryController` | `ArchitectureTest` |

---

## S — Responsabilidad única (SRP)

> Una clase debe tener una sola razón para cambiar.

**Cómo se cumple en el proyecto.**
- Los controladores REST solo convierten HTTP en llamadas a un caso de uso.
- Cada servicio de aplicación implementa un único caso de uso: `ChargeService` genera cobros, `CashPaymentService` registra pagos y `PassService` consulta pases.
- Los adaptadores de persistencia solo ejecutan SQL.

**Refactor HU-22 — `LatestEntryController`.** El controlador hacía tres cosas a la vez:
- recibía la petición;
- buscaba la sede del operador;
- ejecutaba SQL con `JdbcClient`.

Por eso cualquier cambio en la tabla `eventos` obligaba a modificar un controlador web. Se separó así:

```java
// Antes (infrastructure/adapter/in/rest/LatestEntryController.java)
public record LatestEntryController(JdbcClient jdbc, OperatorRepository operators) {
    public LatestEntry latest(Principal principal) {
        long site = operators.findActiveOperator(principal.getName()).orElseThrow(...).siteId();
        return new LatestEntry(jdbc.sql("SELECT placa FROM eventos WHERE ...").param("site", site)...);
    }
}

// Después: el controlador solo traduce HTTP
public record LatestEntryController(FindLatestEntryUseCase latestEntries) {
    public LatestEntry latest(Principal principal) {
        return new LatestEntry(latestEntries.findLatestPlate(principal.getName()).orElse(null));
    }
}
// application/service/LatestEntryService.java          → regla: sede del operador + último ingreso
// infrastructure/adapter/out/persistence/JdbcLatestEntryRepository.java → SQL
```

**Refactor HU-22 — `ParkingConfigurationStore`.** El adaptador de persistencia lanzaba `ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, …)`, es decir, decidía códigos HTTP. Ahora:
- lanza excepciones del dominio, `InvalidParkingConfigurationException` y `SiteNotAvailableException`;
- el controlador las traduce a 422 y 404.

Las respuestas para el cliente no cambiaron: los mismos códigos de estado y el mismo `CONFIGURACION_INVALIDA`.

```java
// ParkingConfigurationStore.java
throw new SiteNotAvailableException();                       // antes: new ResponseStatusException(NOT_FOUND, ...)
private InvalidParkingConfigurationException invalid(String message) { ... }

// ParkingConfigurationController.java
@ExceptionHandler(InvalidParkingConfigurationException.class)  // → 422
@ExceptionHandler(SiteNotAvailableException.class)             // → 404
```

**Problema que evita.** Que un cambio en SQL, en reglas o en HTTP obligue a tocar la misma clase, y que sea imposible probar la regla sin levantar un servidor web y una base de datos.

---

## O — Abierto/cerrado (OCP)

> El software debe estar abierto a la extensión y cerrado a la modificación.

**Cómo se cumple.** El cobro depende del tipo de vehículo. En lugar de un `if (tipo == MOTO)` dentro del motor de cobros, cada regla es una `TariffStrategy`, y `ChargeService` solo pide la estrategia al resolvedor.

```java
// domain/service/tariff/TariffStrategy.java
public interface TariffStrategy {
    VehicleType vehicleType();
    Optional<BigDecimal> amountToCharge(Tariff tariff);
}

// application/service/ChargeService.java: no cambia al agregar tipos
Optional<BigDecimal> amount = strategies.strategyFor(vehicle.type()).amountToCharge(tariff);

// infrastructure/config/ChargeConfig.java: único punto que se extiende
return new TariffStrategyResolver(List.of(new CarTariffStrategy(), new MotorcycleTariffStrategy()));
```

**Problema que evita.** Para agregar, por ejemplo, bicicletas basta con crear `BicycleTariffStrategy` y registrarla. `ChargeService` no se modifica ni hay que volver a probarlo.

---

## L — Sustitución de Liskov (LSP)

> Cualquier implementación de un contrato debe poder reemplazar a otra sin romper a quien lo usa.

**Cómo se cumple.** Los casos de uso funcionan igual con cualquier implementación de sus puertos:

| Contrato | Implementaciones intercambiables |
| --- | --- |
| `PlateReaderPort` | `SimulatedPlateReader` y el lector falso de `PlateReadingServiceTest` |
| `ExitBarrier` | `SimulatedExitBarrier` y `RecordingBarrier` de `ExitBarrierAdapterTest` |
| `ChargeRepository` | `JdbcChargeRepository` e `InMemoryCharges` de `ChargeServiceTest` y `ExitBarrierAdapterTest` |
| `ChargeState` | `PendingChargeState`, `PaidChargeState` y `ClosedChargeState`; todas cumplen el mismo contrato `pay(charge)`: devuelven el estado siguiente o rechazan el pago |

```java
// ExitBarrierAdapterTest.java: la barrera real se sustituye y el servicio se comporta igual
private static class RecordingBarrier implements ExitBarrier {
    public void open(long siteId, String plate) { openings.add(siteId + ":" + plate); }
}
assertThat(barrier.openings).containsExactly("1:ABC123");
```

**Problema que evita.** Que un adaptador "compatible" rompa el sistema al conectarlo, por ejemplo, una cámara real en vez del simulador. Las pruebas con implementaciones sustitutas demuestran que el contrato es suficiente.

---

## I — Segregación de interfaces (ISP)

> Ningún cliente debe depender de métodos que no usa.

**Cómo se cumple.** La mayoría de los puertos tienen uno o dos métodos (`PassRepository`, `ExitBarrier`, `PlateReaderPort`, `TariffRepository`…).

**Refactor HU-22 — `OccupancyRepository`.** El puerto tenía cuatro operaciones y cada cliente usaba solo una parte:

| Cliente | Usaba | No necesitaba |
| --- | --- | --- |
| `EntryRegistrationService` (ingreso) | `lockForEntry`, `occupySpotIfAvailable` | `releaseOccupiedSpot`, `findBySite` |
| `ExitValidationService` (salida) | `releaseOccupiedSpot` | `lockForEntry`, `occupySpotIfAvailable`, `findBySite` |
| `OccupancyController` (consulta) | `findBySite` | las tres operaciones de escritura |

Se dividió en tres interfaces pequeñas. `OccupancyRepository` las reúne para que un solo adaptador (`OccupancyStore`) las implemente:

```java
// domain/port/out/
public interface SpotReservation { void lockForEntry(...); boolean occupySpotIfAvailable(...); }
public interface SpotRelease     { boolean releaseOccupiedSpot(...); }
public interface OccupancyQuery  { List<Occupancy> findBySite(long siteId); }
public interface OccupancyRepository extends SpotReservation, SpotRelease, OccupancyQuery { }

// Cada cliente pide solo lo que usa
public EntryRegistrationService(..., SpotReservation occupancy, ...)
public ExitValidationService(..., SpotRelease occupancy, ...)
public OccupancyController(OccupancyQuery occupancy, ...)
```

**Problema que evita.**
- Que la pantalla de consulta pueda modificar la ocupación por error.
- Que la salida pueda ocupar cupos.
- Que un cambio en la firma de una operación obligue a recompilar y revisar clientes que no la usan.

Además, los dobles de prueba solo tienen que implementar la parte que el caso de uso necesita.

---

## D — Inversión de dependencias (DIP)

> Los módulos de alto nivel no deben depender de los de bajo nivel; ambos dependen de abstracciones.

**Cómo se cumple.**
- Los servicios de `application` reciben puertos de `domain/port/out` por constructor y nunca crean adaptadores.
- Las clases de `infrastructure/config` deciden qué implementación concreta se usa.

```java
// application/service/PlateReadingService.java: conoce el puerto, no el simulador
public PlateReadingService(PlateReaderPort reader, PlateClassifier classifier) { ... }

// infrastructure/config/PlateReaderConfig.java: único lugar que conoce la clase concreta
@Bean PlateReaderPort plateReader() { return new SimulatedPlateReader(); }

// infrastructure/config/EntryRegistrationConfig.java (Refactor HU-22)
@Bean FindLatestEntryUseCase findLatestEntryUseCase(OperatorRepository operators, LatestEntryRepository entries) {
    return new LatestEntryService(operators, entries);
}
```

**Verificación automática.** `ArchitectureTest` (HU-21) falla si `domain` o `application` importan Spring, JPA o clases de `infrastructure`. Esa prueba detectó la violación de `ParkingConfigurationService`, que se corrigió en la HU-21.

**Problema que evita.** Que las reglas de negocio dependan de PostgreSQL, Spring o un dispositivo concreto, que no se puedan probar sin infraestructura, y que cambiar de proveedor obligue a reescribir los casos de uso.

---

## Pendiente identificado

`infrastructure/adapter/in/rest/AdminHistoryController.java` también ejecuta SQL desde un controlador, lo que viola SRP y DIP. No se refactorizó en esta historia porque la HU-20 (PR #45) ya ofrece la misma consulta con arquitectura hexagonal (`/api/v1/history/**`, `HistoryService` y repositorios `*HistoryRepository`). Se recomienda retirar este controlador cuando la HU-20 esté integrada en `develop`.

## Regresión (CA-07)

El comportamiento externo no cambió: mismas URL, mismos códigos HTTP y mismas respuestas. La regresión completa se ejecuta con `mvn -B -f backend/pom.xml verify` y en el CI de GitHub Actions (HU-24).
