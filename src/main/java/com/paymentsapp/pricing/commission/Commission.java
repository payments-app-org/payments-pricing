package com.paymentsapp.pricing.commission;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Расчёт комиссии + котировка: quoteToken фронтенд передаёт в payment, и тот списывает ровно эту комиссию
public record Commission(
        OperationType operationType,
        Subscription subscription,
        Party sourceType,
        Party destinationType,
        BigDecimal amount,
        BigDecimal percent,
        BigDecimal minFee,
        BigDecimal fixedFee,
        // Верхняя граница комиссии; null — нет
        BigDecimal maxFee,
        // Бесплатный лимит в месяц и сколько от него осталось до этой операции; null — лимита нет
        BigDecimal freeMonthlyAmount,
        BigDecimal freeMonthlyLeft,
        BigDecimal commission,
        BigDecimal total,
        int tariffId,
        UUID quoteId,
        Instant expiresAt,
        String quoteToken
) {
}
