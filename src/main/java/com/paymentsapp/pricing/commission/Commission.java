package com.paymentsapp.pricing.commission;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Расчёт комиссии + котировка: quoteToken фронтенд передаёт в payment, и тот списывает ровно эту комиссию
public record Commission(
        OperationType operationType,
        Subscription subscription,
        BigDecimal amount,
        BigDecimal percent,
        BigDecimal commission,
        BigDecimal total,
        int tariffId,
        UUID quoteId,
        Instant expiresAt,
        String quoteToken
) {
}
