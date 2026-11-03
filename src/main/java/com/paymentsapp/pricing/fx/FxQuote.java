package com.paymentsapp.pricing.fx;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Котировка перевода за рубеж: сколько спишем в рублях, сколько получат в валюте, по какому курсу. Комиссии нет.
public record FxQuote(
        BigDecimal amountRub,
        String currency,
        String symbol,
        BigDecimal receiveAmount,
        BigDecimal rate,
        BigDecimal commission,
        BigDecimal total,
        UUID quoteId,
        Instant expiresAt,
        String quoteToken
) {
}
