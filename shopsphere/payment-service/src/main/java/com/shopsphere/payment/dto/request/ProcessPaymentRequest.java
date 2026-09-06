package com.shopsphere.payment.dto.request;

import com.shopsphere.payment.model.entity.SimulationMode;

public record ProcessPaymentRequest(
    SimulationMode simulationMode,
    String failureReason
) {
    public SimulationMode getEffectiveSimulationMode() {
        return simulationMode != null ? simulationMode : SimulationMode.SUCCESS;
    }
}
