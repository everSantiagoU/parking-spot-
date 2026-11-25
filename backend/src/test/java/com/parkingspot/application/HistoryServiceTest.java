package com.parkingspot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkingspot.application.dto.HistoryFilter;
import com.parkingspot.application.service.HistoryService;
import com.parkingspot.domain.exception.InvalidHistoryFilterException;
import com.parkingspot.domain.exception.InvalidPlateException;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.EventType;
import com.parkingspot.domain.model.history.ChargeQuery;
import com.parkingspot.domain.model.history.EventQuery;
import com.parkingspot.domain.model.history.PageResult;
import com.parkingspot.domain.service.PlateClassifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Validación y armado de los filtros del historial (HU-20). Los repositorios solo guardan la consulta recibida. */
class HistoryServiceTest {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private EventQuery lastEventQuery;
    private ChargeQuery lastChargeQuery;

    private final HistoryService service = new HistoryService(new PlateClassifier(),
            query -> {
                lastEventQuery = query;
                return new PageResult<>(List.of(), query.page(), query.size(), 0);
            },
            query -> {
                lastChargeQuery = query;
                return new PageResult<>(List.of(), query.page(), query.size(), 0);
            },
            BOGOTA);

    @Test
    @DisplayName("Sin filtros: página 0 de 20 registros y sin condiciones")
    void valoresPorDefecto() {
        service.findEvents(new HistoryFilter(null, null, null, null, null), null, 1L);

        assertThat(lastEventQuery).isEqualTo(new EventQuery(1L, null, null, null, null, 0, 20));
    }

    @Test
    @DisplayName("CP-HU-20-E01: la placa se normaliza antes de consultar")
    void normalizaPlaca() {
        service.findCharges(new HistoryFilter(" abc123 ", null, null, null, null), null, 1L);

        assertThat(lastChargeQuery.plate()).isEqualTo("ABC123");
    }

    @Test
    @DisplayName("CA-04: el intervalo de eventos va de las 00:00 del día inicial a las 00:00 del día siguiente al final, hora Bogotá")
    void intervaloDeEventosEnHoraLocal() {
        service.findEvents(new HistoryFilter(null, LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 24), 0, 10),
                EventType.INGRESO, null);

        assertThat(lastEventQuery.from()).isEqualTo(LocalDateTime.of(2026, 11, 23, 0, 0).atZone(BOGOTA).toInstant());
        assertThat(lastEventQuery.to()).isEqualTo(LocalDateTime.of(2026, 11, 25, 0, 0).atZone(BOGOTA).toInstant());
        assertThat(lastEventQuery.type()).isEqualTo(EventType.INGRESO);
        assertThat(lastEventQuery.siteId()).isNull();
    }

    @Test
    @DisplayName("CP-HU-20-E02: el filtro de cobros combina fechas de día operativo y estado")
    void combinaFechaYEstado() {
        LocalDate day = LocalDate.of(2026, 11, 23);
        service.findCharges(new HistoryFilter(null, day, day, 1, 5), ChargeStatus.PAGADO, 1L);

        assertThat(lastChargeQuery).isEqualTo(new ChargeQuery(1L, null, ChargeStatus.PAGADO, day, day, 1, 5));
    }

    @Test
    @DisplayName("CP-HU-20-X01: un intervalo con la fecha inicial posterior a la final es inválido")
    void intervaloInvalido() {
        HistoryFilter filter = new HistoryFilter(null, LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 23), null, null);

        assertThatThrownBy(() -> service.findEvents(filter, null, 1L)).isInstanceOf(InvalidHistoryFilterException.class);
        assertThatThrownBy(() -> service.findCharges(filter, null, 1L)).isInstanceOf(InvalidHistoryFilterException.class);
    }

    @Test
    @DisplayName("La paginación rechaza página negativa y tamaños fuera de 1 a 100")
    void paginacionInvalida() {
        assertThatThrownBy(() -> service.findEvents(new HistoryFilter(null, null, null, -1, null), null, 1L))
                .isInstanceOf(InvalidHistoryFilterException.class);
        assertThatThrownBy(() -> service.findEvents(new HistoryFilter(null, null, null, null, 0), null, 1L))
                .isInstanceOf(InvalidHistoryFilterException.class);
        assertThatThrownBy(() -> service.findCharges(new HistoryFilter(null, null, null, null, 101), null, 1L))
                .isInstanceOf(InvalidHistoryFilterException.class);
    }

    @Test
    @DisplayName("Una placa con formato inválido se rechaza")
    void placaInvalida() {
        assertThatThrownBy(() -> service.findCharges(new HistoryFilter("12", null, null, null, null), null, 1L))
                .isInstanceOf(InvalidPlateException.class);
    }

    @Test
    @DisplayName("El total de páginas se redondea hacia arriba")
    void totalDePaginas() {
        assertThat(new PageResult<>(List.of(), 0, 20, 41).totalPages()).isEqualTo(3);
        assertThat(new PageResult<>(List.of(), 0, 20, 0).totalPages()).isZero();
    }
}
