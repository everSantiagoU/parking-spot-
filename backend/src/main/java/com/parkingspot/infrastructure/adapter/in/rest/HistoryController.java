package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.application.dto.HistoryFilter;
import com.parkingspot.application.usecase.QueryChargesUseCase;
import com.parkingspot.application.usecase.QueryEventsUseCase;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.EventType;
import com.parkingspot.domain.model.history.ChargeRecord;
import com.parkingspot.domain.model.history.EventRecord;
import com.parkingspot.domain.model.history.PageResult;
import com.parkingspot.domain.port.out.OperatorSiteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
@Tag(name = "Historial", description = "Consulta de eventos y cobros con filtros y paginación (HU-20)")
public class HistoryController {
    private final QueryEventsUseCase queryEvents;
    private final QueryChargesUseCase queryCharges;
    private final OperatorSiteRepository operatorSites;

    public HistoryController(QueryEventsUseCase queryEvents, QueryChargesUseCase queryCharges,
                             OperatorSiteRepository operatorSites) {
        this.queryEvents = queryEvents;
        this.queryCharges = queryCharges;
        this.operatorSites = operatorSites;
    }

    @GetMapping("/events")
    @Operation(summary = "Lista ingresos, validaciones y salidas. El operador ve su sede; el administrador, todas.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Página de eventos; puede venir vacía"),
                    @ApiResponse(responseCode = "400", description = "Filtro inválido (intervalo, placa o paginación)", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Sin sesión", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Sin rol de operador o administrador", content = @Content)
            })
    public PageResponse<EventRecord> events(
            @Parameter(description = "Placa exacta, por ejemplo ABC123") @RequestParam(required = false) String plate,
            @Parameter(description = "Fecha inicial (yyyy-MM-dd), inclusive")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fecha final (yyyy-MM-dd), inclusive")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "INGRESO, VALIDACION o SALIDA") @RequestParam(required = false) EventType type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication user) {
        HistoryFilter filter = new HistoryFilter(plate, from, to, page, size);
        return PageResponse.from(queryEvents.findEvents(filter, type, siteOf(user)));
    }

    @GetMapping("/charges")
    @Operation(summary = "Lista cobros con importe, fecha, placa y estado. El operador ve su sede; el administrador, todas.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Página de cobros; puede venir vacía"),
                    @ApiResponse(responseCode = "400", description = "Filtro inválido (intervalo, placa, estado o paginación)", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Sin sesión", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Sin rol de operador o administrador", content = @Content)
            })
    public PageResponse<ChargeRecord> charges(
            @Parameter(description = "Placa exacta, por ejemplo ABC123") @RequestParam(required = false) String plate,
            @Parameter(description = "Día operativo inicial (yyyy-MM-dd), inclusive")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Día operativo final (yyyy-MM-dd), inclusive")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "PENDIENTE, PAGADO, VENCIDO o ANULADO") @RequestParam(required = false) ChargeStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication user) {
        HistoryFilter filter = new HistoryFilter(plate, from, to, page, size);
        return PageResponse.from(queryCharges.findCharges(filter, status, siteOf(user)));
    }

    /** El administrador consulta todas las sedes (null); el operador solo la suya. */
    private Long siteOf(Authentication user) {
        boolean admin = user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (admin) {
            return null;
        }
        return operatorSites.findSiteOf(user.getName())
                .orElseThrow(ChargeController.OperatorWithoutSiteException::new);
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
        static <T> PageResponse<T> from(PageResult<T> result) {
            return new PageResponse<>(result.items(), result.page(), result.size(), result.totalItems(),
                    result.totalPages());
        }
    }
}
