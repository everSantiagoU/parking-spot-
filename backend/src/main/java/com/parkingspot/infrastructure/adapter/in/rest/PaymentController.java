package com.parkingspot.infrastructure.adapter.in.rest;

import com.parkingspot.application.dto.PaymentResult;
import com.parkingspot.application.usecase.FindPaymentUseCase;
import com.parkingspot.application.usecase.RegisterCashPaymentUseCase;
import com.parkingspot.domain.exception.PaymentRejectedException;
import com.parkingspot.domain.exception.PaymentRejectedException.Reason;
import com.parkingspot.domain.model.ChargeStatus;
import com.parkingspot.domain.model.Payment;
import com.parkingspot.domain.model.PaymentMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Pagos", description = "Pago en efectivo idempotente (HU-16)")
public class PaymentController {
    private final RegisterCashPaymentUseCase registerPayment;
    private final FindPaymentUseCase findPayment;

    public PaymentController(RegisterCashPaymentUseCase registerPayment, FindPaymentUseCase findPayment) {
        this.registerPayment = registerPayment;
        this.findPayment = findPayment;
    }

    @PostMapping
    @Operation(summary = "Registra el pago en efectivo de un cobro pendiente", responses = {
            @ApiResponse(responseCode = "201", description = "Pago registrado; el cobro queda PAGADO con pase vigente"),
            @ApiResponse(responseCode = "200", description = "Repetición con la misma clave: se devuelve el pago original"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o importe incorrecto", content = @Content),
            @ApiResponse(responseCode = "404", description = "El cobro no existe", content = @Content),
            @ApiResponse(responseCode = "409", description = "Cobro ya pagado, no pagable o clave usada con otros datos", content = @Content)
    })
    public ResponseEntity<PaymentResponse> pay(@RequestBody PaymentRequest request, Authentication operator) {
        if (request == null || request.chargeId() == null) {
            throw new PaymentRejectedException(Reason.SOLICITUD_INVALIDA, "Debe indicar el cobro a pagar.");
        }
        PaymentResult result = registerPayment.payInCash(request.chargeId(), request.amount(),
                request.idempotencyKey(), operator.getName());
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(PaymentResponse.from(result));
    }

    @GetMapping("/charge/{chargeId}")
    @Operation(summary = "Consulta el pago de un cobro: importe, fecha, método y operador", responses = {
            @ApiResponse(responseCode = "200", description = "Pago encontrado"),
            @ApiResponse(responseCode = "404", description = "El cobro no tiene pagos", content = @Content)
    })
    public PaymentView byCharge(@PathVariable long chargeId) {
        return PaymentView.from(findPayment.findByCharge(chargeId));
    }

    public record PaymentRequest(Long chargeId, BigDecimal amount, String idempotencyKey) {
    }

    public record PaymentView(long paymentId, long chargeId, PaymentMethod method, BigDecimal amount, String operator,
                              OffsetDateTime registeredAt) {
        static PaymentView from(Payment payment) {
            return new PaymentView(payment.id(), payment.chargeId(), payment.method(), payment.amount(),
                    payment.operator(), payment.registeredAt());
        }
    }

    public record PaymentResponse(PaymentView payment, String plate, ChargeStatus chargeStatus,
                                  OffsetDateTime passValidUntil, boolean replayed) {
        static PaymentResponse from(PaymentResult result) {
            return new PaymentResponse(PaymentView.from(result.payment()), result.charge().plate(),
                    result.chargeStatus(), result.passValidUntil(), result.replayed());
        }
    }
}
