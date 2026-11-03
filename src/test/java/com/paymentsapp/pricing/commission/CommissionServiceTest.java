package com.paymentsapp.pricing.commission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paymentsapp.pricing.account.AccountClient;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommissionServiceTest {

    private final AccountClient accountClient = mock(AccountClient.class);
    private final CommissionRuleRepository rules = mock(CommissionRuleRepository.class);
    private final CommissionService service = new CommissionService(accountClient, rules);
    private final UUID userId = UUID.randomUUID();

    private void givenRule(OperationType op, Subscription sub, String percent) {
        when(accountClient.getSubscription(userId)).thenReturn(sub);
        when(rules.findByOperationTypeAndSubscription(op, sub))
                .thenReturn(Optional.of(new CommissionRule(op, sub, new BigDecimal(percent))));
    }

    @Test
    void addsPercentCommissionToAmount() {
        givenRule(OperationType.CARD_TRANSFER, Subscription.NONE, "1.50");

        Commission result = service.calculate(userId, OperationType.CARD_TRANSFER, new BigDecimal("100"));

        assertThat(result.commission()).isEqualByComparingTo("1.50");
        assertThat(result.total()).isEqualByComparingTo("101.50");
        assertThat(result.subscription()).isEqualTo(Subscription.NONE);
    }

    @Test
    void roundsCommissionHalfUpToKopecks() {
        givenRule(OperationType.PHONE_TRANSFER, Subscription.PREMIUM, "0.50");

        // 0.5% от 3.33 = 0.01665 → 0.02
        Commission result = service.calculate(userId, OperationType.PHONE_TRANSFER, new BigDecimal("3.33"));

        assertThat(result.commission()).isEqualByComparingTo("0.02");
        assertThat(result.total()).isEqualByComparingTo("3.35");
    }

    @Test
    void zeroPercentMeansNoCommission() {
        givenRule(OperationType.OWN_TRANSFER, Subscription.NONE, "0.00");

        Commission result = service.calculate(userId, OperationType.OWN_TRANSFER, new BigDecimal("50"));

        assertThat(result.commission()).isEqualByComparingTo("0");
        assertThat(result.total()).isEqualByComparingTo("50");
    }

    @Test
    void failsWhenRuleIsMissing() {
        when(accountClient.getSubscription(userId)).thenReturn(Subscription.PREMIUM_PLUS);
        when(rules.findByOperationTypeAndSubscription(OperationType.PHONE_TRANSFER, Subscription.PREMIUM_PLUS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(userId, OperationType.PHONE_TRANSFER, BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class);
    }
}
