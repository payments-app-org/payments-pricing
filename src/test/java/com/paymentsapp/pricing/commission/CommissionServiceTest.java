package com.paymentsapp.pricing.commission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paymentsapp.pricing.account.AccountClient;
import com.paymentsapp.pricing.quote.QuoteSigner;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class CommissionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");

    private final AccountClient accountClient = mock(AccountClient.class);
    private final CommissionRuleRepository rules = mock(CommissionRuleRepository.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final CommissionService service = new CommissionService(accountClient, rules,
            new QuoteSigner(json, "test-secret"), Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    private void givenRule(OperationType op, Subscription sub, String percent) {
        when(accountClient.getSubscription(userId, accountId)).thenReturn(sub);
        when(rules.findByOperationTypeAndSubscription(op, sub))
                .thenReturn(Optional.of(new CommissionRule(7, op, sub, new BigDecimal(percent))));
    }

    @Test
    void addsPercentCommissionToAmount() {
        givenRule(OperationType.CARD2CARD, Subscription.NONE, "1.50");

        Commission result = service.calculate(userId, accountId, OperationType.CARD2CARD, new BigDecimal("100"));

        assertThat(result.commission()).isEqualByComparingTo("1.50");
        assertThat(result.total()).isEqualByComparingTo("101.50");
        assertThat(result.subscription()).isEqualTo(Subscription.NONE);
        assertThat(result.tariffId()).isEqualTo(7);
    }

    @Test
    void roundsCommissionHalfUpToKopecks() {
        givenRule(OperationType.SBP_PHONE, Subscription.PREMIUM, "0.50");

        // 0.5% от 3.33 = 0.01665 → 0.02
        Commission result = service.calculate(userId, accountId, OperationType.SBP_PHONE, new BigDecimal("3.33"));

        assertThat(result.commission()).isEqualByComparingTo("0.02");
        assertThat(result.total()).isEqualByComparingTo("3.35");
    }

    @Test
    void quoteTokenCarriesTheCalculationAndExpiry() {
        givenRule(OperationType.SBP_PHONE, Subscription.NONE, "1.00");

        Commission result = service.calculate(userId, accountId, OperationType.SBP_PHONE, new BigDecimal("500"));

        String[] parts = result.quoteToken().split("\\.");
        assertThat(parts).hasSize(2);
        JsonNode body = json.readTree(Base64.getUrlDecoder().decode(parts[0]));
        assertThat(body.get("quoteId").asString()).isEqualTo(result.quoteId().toString());
        assertThat(body.get("userId").asString()).isEqualTo(userId.toString());
        assertThat(body.get("sourceAccountId").asString()).isEqualTo(accountId.toString());
        assertThat(body.get("commission").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(CommissionService.QUOTE_TTL));
    }

    @Test
    void failsWhenRuleIsMissing() {
        when(accountClient.getSubscription(userId, accountId)).thenReturn(Subscription.PREMIUM_PLUS);
        when(rules.findByOperationTypeAndSubscription(OperationType.SBP_PHONE, Subscription.PREMIUM_PLUS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(userId, accountId, OperationType.SBP_PHONE, BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class);
    }
}
