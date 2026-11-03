package com.paymentsapp.pricing.commission;

import com.paymentsapp.pricing.account.AccountClient;
import com.paymentsapp.pricing.quote.Quote;
import com.paymentsapp.pricing.quote.QuoteSigner;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommissionService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    // Сколько живёт котировка: за это время пользователь должен нажать «Перевести»
    static final Duration QUOTE_TTL = Duration.ofMinutes(10);

    private final AccountClient accountClient;
    private final CommissionRuleRepository rules;
    private final QuoteSigner signer;
    private final Clock clock;

    @Autowired
    public CommissionService(AccountClient accountClient, CommissionRuleRepository rules, QuoteSigner signer) {
        this(accountClient, rules, signer, Clock.systemUTC());
    }

    CommissionService(AccountClient accountClient, CommissionRuleRepository rules, QuoteSigner signer, Clock clock) {
        this.accountClient = accountClient;
        this.rules = rules;
        this.signer = signer;
        this.clock = clock;
    }

    public Commission calculate(UUID userId, UUID sourceAccountId, OperationType operationType, BigDecimal amount) {
        Subscription subscription = accountClient.getSubscription(userId, sourceAccountId);
        CommissionRule rule = rules.findByOperationTypeAndSubscription(operationType, subscription)
                .orElseThrow(() -> new IllegalStateException(
                        "Нет правила комиссии для " + operationType + " / " + subscription));

        BigDecimal commission = amount.multiply(rule.getPercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        Quote quote = new Quote(UUID.randomUUID(), userId, sourceAccountId, operationType.name(), amount, commission,
                rule.getId(), Instant.now(clock).plus(QUOTE_TTL));
        return new Commission(operationType, subscription, amount, rule.getPercent(), commission,
                amount.add(commission), rule.getId(), quote.quoteId(), quote.expiresAt(), signer.sign(quote));
    }
}
