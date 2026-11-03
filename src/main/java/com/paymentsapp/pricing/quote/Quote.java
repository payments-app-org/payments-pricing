package com.paymentsapp.pricing.quote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Содержимое котировки. Тот же формат читает сервис payment.
public record Quote(
        UUID quoteId,
        UUID userId,
        UUID sourceAccountId,
        String operationType,
        BigDecimal amount,
        BigDecimal commission,
        int tariffId,
        Instant expiresAt
) {
}
