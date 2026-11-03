package com.paymentsapp.pricing.commission;

import com.paymentsapp.pricing.account.AccountClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CommissionService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final AccountClient accountClient;
    private final CommissionRuleRepository rules;

    public CommissionService(AccountClient accountClient, CommissionRuleRepository rules) {
        this.accountClient = accountClient;
        this.rules = rules;
    }

    public Commission calculate(UUID userId, OperationType operationType, BigDecimal amount) {
        Subscription subscription = accountClient.getSubscription(userId);
        BigDecimal percent = rules.findByOperationTypeAndSubscription(operationType, subscription)
                .map(CommissionRule::getPercent)
                .orElseThrow(() -> new IllegalStateException(
                        "Нет правила комиссии для " + operationType + " / " + subscription));

        BigDecimal commission = amount.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return new Commission(operationType, subscription, amount, percent, commission, amount.add(commission));
    }
}
