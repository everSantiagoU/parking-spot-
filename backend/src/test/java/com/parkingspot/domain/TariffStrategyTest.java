package com.parkingspot.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkingspot.domain.exception.TariffNotConfiguredException;
import com.parkingspot.domain.model.Tariff;
import com.parkingspot.domain.model.TimeSlot;
import com.parkingspot.domain.model.VehicleType;
import com.parkingspot.domain.service.tariff.CarTariffStrategy;
import com.parkingspot.domain.service.tariff.MotorcycleTariffStrategy;
import com.parkingspot.domain.service.tariff.TariffStrategyResolver;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-14 CA-05: cada tipo de vehículo tiene su estrategia de cobro (patrón Strategy). */
class TariffStrategyTest {
    private final TariffStrategyResolver resolver =
            new TariffStrategyResolver(List.of(new CarTariffStrategy(), new MotorcycleTariffStrategy()));

    @Test
    @DisplayName("CA-01: una moto con tarifa $0 no genera obligación de pago")
    void motoConTarifaCeroNoGeneraCobro() {
        Tariff free = new Tariff(3, VehicleType.MOTO, TimeSlot.DIURNA, BigDecimal.ZERO);

        assertThat(resolver.strategyFor(VehicleType.MOTO).amountToCharge(free)).isEmpty();
    }

    @Test
    @DisplayName("RN-01 es configurable: una moto con tarifa mayor a $0 sí genera cobro")
    void motoConTarifaPositivaGeneraCobro() {
        Tariff paid = new Tariff(3, VehicleType.MOTO, TimeSlot.DIURNA, new BigDecimal("2000"));

        assertThat(resolver.strategyFor(VehicleType.MOTO).amountToCharge(paid)).contains(new BigDecimal("2000"));
    }

    @Test
    @DisplayName("Un carro paga la tarifa configurada")
    void carroPagaLaTarifa() {
        Tariff day = new Tariff(1, VehicleType.CARRO, TimeSlot.DIURNA, new BigDecimal("10000"));

        assertThat(resolver.strategyFor(VehicleType.CARRO).amountToCharge(day)).contains(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("Una tarifa de $0 para carros es una configuración inválida")
    void carroConTarifaCeroEsUnError() {
        Tariff invalid = new Tariff(1, VehicleType.CARRO, TimeSlot.DIURNA, BigDecimal.ZERO);

        assertThatThrownBy(() -> resolver.strategyFor(VehicleType.CARRO).amountToCharge(invalid))
                .isInstanceOf(TariffNotConfiguredException.class);
    }

    @Test
    void elResolvedorDevuelveLaEstrategiaDeCadaTipo() {
        assertThat(resolver.strategyFor(VehicleType.CARRO)).isInstanceOf(CarTariffStrategy.class);
        assertThat(resolver.strategyFor(VehicleType.MOTO)).isInstanceOf(MotorcycleTariffStrategy.class);
    }
}
