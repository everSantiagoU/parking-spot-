# Patrones de diseño (HU-21)

Parking Spot sigue una **arquitectura hexagonal**:

- **`domain`** contiene los modelos, las reglas, los servicios de dominio y los puertos de salida (`domain/port/out`). No importa Spring, JPA, `slf4j` ni otras capas.
- **`application`** contiene los casos de uso. Depende solo del dominio.
- **`infrastructure`** contiene los adaptadores (REST, JDBC, dispositivos simulados) y la configuración que conecta todo con Spring (`infrastructure/config`).

Sobre esa base se aplican tres patrones obligatorios (**Strategy**, **Adapter** y **Repository**) y uno adicional (**State**).

Todas las rutas son relativas a `backend/src/main/java/com/parkingspot/`.

| Patrón | Problema que resuelve | Dónde está | Prueba |
| --- | --- | --- | --- |
| Strategy | El cobro cambia según el tipo de vehículo | `domain/service/tariff/` | `TariffStrategyTest`, `ChargeServiceTest` |
| Adapter | Los lectores de placas y las barreras son simulados y se deben poder reemplazar | `domain/port/out/PlateReaderPort`, `ExitBarrier` + `infrastructure/adapter/out/plate`, `.../barrier` | `PlateReadingServiceTest`, `ExitBarrierAdapterTest`, `SimulatedPlateReaderTest` |
| Repository | Los casos de uso necesitan datos sin conocer PostgreSQL | `domain/port/out/*Repository` + `infrastructure/adapter/out/persistence/` | `ChargeServiceTest`, `PassServiceTest`, `ExitBarrierAdapterTest` y las pruebas `*ApiTest` con Testcontainers |
| State (adicional) | Un pago se permite o se rechaza según el estado del cobro | `domain/service/state/` | `CashPaymentServiceTest` |
| Hexagonal (CA-04) | El dominio no debe depender de frameworks | Todo el backend | `ArchitectureTest` |

---

## 1. Strategy: reglas de cobro por tipo de vehículo

**Problema.**
- RN-01: una moto con tarifa de $0 no genera obligación de pago.
- Un carro siempre paga, y una tarifa de $0 para carro es un error de configuración.
- Con un `if (tipo == MOTO) … else …` dentro de `ChargeService`, cada tipo nuevo de vehículo obligaría a modificar el servicio.

**Solución y justificación.**
- Cada regla es una clase que implementa `TariffStrategy`.
- `TariffStrategyResolver` elige la estrategia según `VehicleType`.
- `ChargeService` solo conoce la interfaz.
- Agregar un tipo de vehículo nuevo es crear una clase y registrarla en `ChargeConfig`, sin tocar el servicio. Esto cumple el principio abierto/cerrado (OCP) y el de inversión de dependencias (DIP).

**Ubicación.**
- `domain/service/tariff/TariffStrategy.java`: la interfaz (estrategia).
- `domain/service/tariff/CarTariffStrategy.java` y `MotorcycleTariffStrategy.java`: las estrategias concretas.
- `domain/service/tariff/TariffStrategyResolver.java`: el selector.
- `application/service/ChargeService.java`: el contexto que usa la estrategia.
- `infrastructure/config/ChargeConfig.java`: registra las estrategias.

**Fragmentos reales.**

```java
// TariffStrategy.java
public interface TariffStrategy {
    VehicleType vehicleType();
    Optional<BigDecimal> amountToCharge(Tariff tariff);
}

// MotorcycleTariffStrategy.java
public Optional<BigDecimal> amountToCharge(Tariff tariff) {
    if (tariff.value().signum() == 0) {
        return Optional.empty();
    }
    return Optional.of(tariff.value());
}

// ChargeService.java
Optional<BigDecimal> amount = strategies.strategyFor(vehicle.type()).amountToCharge(tariff);

// ChargeConfig.java
return new TariffStrategyResolver(List.of(new CarTariffStrategy(), new MotorcycleTariffStrategy()));
```

**Diagrama de clases (referencia para dibujarlo a mano).**

- `«interface» TariffStrategy`
  - `+ vehicleType(): VehicleType`
  - `+ amountToCharge(tariff: Tariff): Optional<BigDecimal>`
- `CarTariffStrategy` y `MotorcycleTariffStrategy` implementan `TariffStrategy` (flecha punteada con triángulo vacío).
- `TariffStrategyResolver`
  - `- strategies: Map<VehicleType, TariffStrategy>`
  - `+ strategyFor(type: VehicleType): TariffStrategy`
  - Relación: agregación 1 → 1..* con `TariffStrategy`.
- `ChargeService` (implementa `GenerateChargeUseCase`)
  - `- strategies: TariffStrategyResolver`
  - Relación: asociación hacia `TariffStrategyResolver`.
- `Tariff` y `VehicleType`: dependencias (flecha punteada) desde la interfaz.

---

## 2. Adapter: dispositivos simulados (lector de placas y barrera de salida)

**Problema.** El MVP no tiene una cámara ni una barrera reales, pero el sistema debe poder conectarlas después sin cambiar las reglas de negocio.

**Solución y justificación.**
- El dominio define lo que necesita como **puertos**: `PlateReaderPort` y `ExitBarrier`.
- En infraestructura, cada dispositivo tiene un **adaptador** que traduce entre el puerto y el dispositivo:
  - `SimulatedPlateReader` convierte la captura que envía el operador en un `RawPlateReading` con fuente `SIMULADOR`.
  - `SimulatedExitBarrier` convierte la orden de apertura en un registro de log.
- Los adaptadores se crean en un solo lugar: `PlateReaderConfig` y `ExitConfig`.
- Cambiar de dispositivo es cambiar un `@Bean`. `PlateReadingService` y `ExitValidationService` no se modifican (DIP).
- Ajuste hecho en esta historia: antes, `SimulatedExitBarrier` se registraba con `@Component`. Ahora se crea en `ExitConfig`, igual que el lector de placas, para que los dos adaptadores se reemplacen de la misma forma.

**Ubicación.**
- Puertos (target): `domain/port/out/PlateReaderPort.java` y `domain/port/out/ExitBarrier.java`.
- Adaptadores: `infrastructure/adapter/out/plate/SimulatedPlateReader.java` e `infrastructure/adapter/out/barrier/SimulatedExitBarrier.java`.
- Clientes: `application/service/PlateReadingService.java` y `application/service/ExitValidationService.java`.
- Conexión: `infrastructure/config/PlateReaderConfig.java` y `infrastructure/config/ExitConfig.java`.

**Fragmentos reales.**

```java
// PlateReaderPort.java (dominio)
public interface PlateReaderPort {
    RawPlateReading read(PlateCapture capture);
}

// SimulatedPlateReader.java (infraestructura)
public RawPlateReading read(PlateCapture capture) {
    return new RawPlateReading(capture.plateText(), Confidence.of(capture.confidence()), ReadingSource.SIMULADOR);
}

// ExitConfig.java: el único lugar que cambia al conectar una barrera física
@Bean
ExitBarrier exitBarrier() {
    return new SimulatedExitBarrier();
}
```

**Evidencia CP-HU-21-E02.** Estas pruebas sustituyen el adaptador por uno de prueba sin modificar el caso de uso:
- `PlateReadingServiceTest.dependsOnlyOnThePortSoAnyReaderCanBePluggedIn` reemplaza el lector por una lambda.
- `ExitBarrierAdapterTest` usa una `RecordingBarrier` que guarda las aperturas en lugar de mover una barrera.

**Diagrama de clases (referencia para dibujarlo a mano).** Se dibuja un esquema por dispositivo.

Lector de placas:
- `«interface» PlateReaderPort` (paquete `domain.port.out`)
  - `+ read(capture: PlateCapture): RawPlateReading`
- `SimulatedPlateReader` (paquete `infrastructure.adapter.out.plate`) implementa `PlateReaderPort`.
- `PlateReadingService` (implementa `ReadPlateUseCase`)
  - `- reader: PlateReaderPort`
  - `- classifier: PlateClassifier`
  - Relación: asociación hacia `PlateReaderPort`.
- Clase opcional para mostrar la extensión: `«futuro» CameraPlateReader`, que también implementa `PlateReaderPort`.

Barrera de salida:
- `«interface» ExitBarrier`
  - `+ open(siteId: long, plate: String): void`
- `SimulatedExitBarrier` implementa `ExitBarrier`.
- `ExitValidationService`
  - `- barrier: ExitBarrier`
  - Relación: asociación hacia `ExitBarrier`.
- `ExitConfig` crea `SimulatedExitBarrier` (dependencia «create»).

---

## 3. Repository: persistencia independiente de PostgreSQL

**Problema.** Los casos de uso necesitan consultar y guardar cobros, pagos, pases, tarifas, franjas, eventos y operadores. Si usaran SQL o `JdbcClient` directamente, quedarían atados a PostgreSQL y a Spring, y no se podrían probar sin una base de datos.

**Solución y justificación.**
- Cada agregado tiene una interfaz de repositorio en `domain/port/out`. La interfaz se expresa con objetos del dominio (`Charge`, `Pass`, `Tariff`…), nunca con filas ni con SQL.
- La implementación en infraestructura usa `JdbcClient` con SQL parametrizado.
- Los servicios reciben la interfaz por constructor. En las pruebas unitarias se pasa una versión en memoria, y en las de integración (Testcontainers) se usa la versión JDBC real.
- Decisión técnica: se usa JDBC en lugar de Spring Data JPA porque varias operaciones necesitan SQL específico, como el `INSERT … ON CONFLICT` atómico de `saveIfAbsent` o el pase más reciente por sede. El patrón Repository no depende de esa elección.

**Ubicación.** Interfaces en `domain/port/out/`; implementaciones en `infrastructure/adapter/out/persistence/`.

| Puerto (dominio) | Implementación (infraestructura) |
| --- | --- |
| `ChargeRepository` | `JdbcChargeRepository` |
| `PayableChargeRepository` | `JdbcPayableChargeRepository` |
| `PaymentRepository` | `JdbcPaymentRepository` |
| `PassRepository` | `JdbcPassRepository` |
| `TariffRepository` | `JdbcTariffRepository` |
| `TimeSlotRepository` | `JdbcTimeSlotRepository` |
| `OperatorRepository` | `OperatorStore` |
| `OperatorSiteRepository` | `JdbcOperatorSiteRepository` |
| `OccupancyRepository` | `OccupancyStore` |
| `EntryEventRepository` | `EntryEventStore` |
| `ExitEventRepository` | `ExitEventStore` |
| — (configuración HU-12, acceso directo) | `ParkingConfigurationStore` |

**Fragmentos reales.**

```java
// ChargeRepository.java (dominio)
public interface ChargeRepository {
    Optional<Charge> findByPlateAndSiteAndDay(String plate, long siteId, LocalDate operatingDay);
    SavedCharge saveIfAbsent(Charge charge);
}

// JdbcChargeRepository.java (infraestructura)
@Repository
public class JdbcChargeRepository implements ChargeRepository {
    private final JdbcClient jdbc;
    ...
}

// ChargeServiceTest.java: la misma interfaz, en memoria
private static class InMemoryCharges implements ChargeRepository { ... }
```

**Diagrama de clases (referencia para dibujarlo a mano).** Se toma `ChargeRepository` como ejemplo representativo.

- `«interface» ChargeRepository`
  - `+ findByPlateAndSiteAndDay(plate: String, siteId: long, day: LocalDate): Optional<Charge>`
  - `+ saveIfAbsent(charge: Charge): SavedCharge`
- `JdbcChargeRepository` implementa `ChargeRepository`.
  - `- jdbc: JdbcClient`
- `InMemoryCharges` (de prueba) implementa `ChargeRepository`.
- `ChargeService` y `ExitValidationService` tienen una asociación hacia `ChargeRepository`.
- `Charge` es el record del dominio que maneja el repositorio (dependencia).
- La frontera hexagonal se dibuja como una línea entre los paquetes `domain` e `infrastructure`: las flechas siempre apuntan hacia el dominio.

---

## 4. State (adicional): pago según el estado del cobro

**Problema.** Solo un cobro `PENDIENTE` se puede pagar. Uno `PAGADO` se rechaza por "ya pagado", y uno `VENCIDO` o `ANULADO` se rechaza por "no pagable". Con un `switch` dentro de `CashPaymentService`, esas reglas quedarían mezcladas con el registro del pago.

**Solución.**
- Cada estado es una clase que implementa `ChargeState.pay(charge)`.
- `ChargeStates` devuelve el objeto que corresponde a cada `ChargeStatus`.
- `CashPaymentService` llama a `states.of(charge.status()).pay(charge)`.

**Ubicación.**
- `domain/service/state/ChargeState.java`
- `PendingChargeState`, `PaidChargeState`, `ClosedChargeState` y `ChargeStates`, en el mismo paquete.
- Lo usa `application/service/CashPaymentService.java`.

**Diagrama de clases (referencia).**
- `«interface» ChargeState` con `+ pay(charge: Charge): ChargeStatus`.
- `PendingChargeState`, `PaidChargeState` y `ClosedChargeState` implementan la interfaz.
- `ChargeStates` tiene `- states: Map<ChargeStatus, ChargeState>` y `+ of(status): ChargeState`.
- `CashPaymentService` tiene una asociación hacia `ChargeStates`.

---

## 5. Verificación de la arquitectura (CA-04, CA-07, CP-HU-21-X01)

`ArchitectureTest` lee los `import` del código fuente y falla con el archivo y la línea exactos en estos casos:
- `domain` importa `org.springframework`, `jakarta.persistence`, `org.slf4j`, `application` o `infrastructure`.
- `application` importa Spring, JPA o `infrastructure`.

**Dependencia real detectada y corregida (CP-HU-21-X01).** Al integrar `develop` con esta historia, `ArchitectureTest` encontró que `application/service/ParkingConfigurationService.java` (configuración de la HU-12) importaba `org.springframework.*` y usaba `JdbcClient`. Es decir, la capa de aplicación dependía de Spring y de PostgreSQL. Se corrigió moviendo la clase a `infrastructure/adapter/out/persistence/ParkingConfigurationStore.java`, que es la capa que le corresponde porque es un adaptador de persistencia, y actualizando `ParkingConfigurationController`. El comportamiento y las URL no cambiaron.

Además, para comprobar que la prueba funciona, se agregó a propósito `import org.springframework.stereotype.Component;` en `domain/model/Pass.java`. La prueba falló con `domain/model/Pass.java: import org.springframework.stereotype.Component;` y, después de quitar el import, volvió a pasar.

La regresión completa se ejecuta con `mvn -B -f backend/pom.xml verify` y también en el CI de GitHub Actions (HU-24).

> **CA-06:** los diagramas de clases de cada patrón los dibuja el equipo a mano, tomando como referencia las descripciones de este documento, y se adjuntan al documento académico.
