package com.paymentsapp.pricing.commission;

import java.math.BigDecimal;

public record Commission(
        OperationType operationType,
        Subscription subscription,
        BigDecimal amount,
        BigDecimal percent,
        BigDecimal commission,
        BigDecimal total
) {
}
