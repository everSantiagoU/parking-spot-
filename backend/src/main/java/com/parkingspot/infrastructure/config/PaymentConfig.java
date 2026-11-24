package com.parkingspot.infrastructure.config;

import com.parkingspot.application.port.Transactions;
import com.parkingspot.application.service.CashPaymentService;
import com.parkingspot.domain.port.out.PayableChargeRepository;
import com.parkingspot.domain.port.out.PaymentRepository;
import com.parkingspot.domain.service.OperatingDayCalculator;
import com.parkingspot.domain.service.state.ChargeStates;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Arma el pago en efectivo (HU-16). */
@Configuration
public class PaymentConfig {

    @Bean
    CashPaymentService cashPaymentService(PayableChargeRepository charges, PaymentRepository payments,
                                          OperatingDayCalculator operatingDays, Transactions transactions,
                                          Clock parkingClock) {
        return new CashPaymentService(charges, payments, new ChargeStates(), operatingDays, transactions, parkingClock);
    }
}
