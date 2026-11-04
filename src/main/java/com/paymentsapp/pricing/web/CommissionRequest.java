package com.paymentsapp.pricing.web;

import com.paymentsapp.pricing.commission.OperationType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CommissionRequest(
        @NotNull UUID userId,
        @NotNull UUID sourceAccountId,
        @NotNull OperationType operationType,
        @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount,
        // Для CARD2CARD — номер карты получателя (наша карта или чужая — разные тарифы)
        String recipientCardNumber,
        // Для OWN_TRANSFER — свой счёт зачисления
        UUID destinationAccountId,
        // Для BILL — код поставщика в biller
        String providerCode
) {
}
