package com.paymentsapp.pricing.web;

import com.paymentsapp.pricing.commission.OperationType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CommissionRequest(
        @NotNull UUID userId,
        @NotNull OperationType operationType,
        @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount
) {
}
