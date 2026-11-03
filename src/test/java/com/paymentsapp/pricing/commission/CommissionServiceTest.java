package com.paymentsapp.pricing.commission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paymentsapp.pricing.account.AccountClient;
import com.paymentsapp.pricing.account.AccountClient.AccountRef;
import com.paymentsapp.pricing.account.AccountClient.AccountUser;
import com.paymentsapp.pricing.quote.QuoteSigner;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class CommissionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
    private static final String OUR_CARD = "2200300456781002";
    private static final String FOREIGN_CARD = "4111111111111111";

    private final AccountClient accountClient = mock(AccountClient.class);
    private final CommissionRuleRepository rules = mock(CommissionRuleRepository.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final CommissionService service = new CommissionService(accountClient, rules,
            new QuoteSigner(json, "test-secret"), Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID userId = UUID.randomUUID();
    private final UUID walletId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();

    private void givenUser(Subscription subscription) {
        when(accountClient.getUser(userId)).thenReturn(new AccountUser(userId, subscription,
                List.of(new AccountRef(walletId, "WALLET"), new AccountRef(cardId, "CARD"))));
        when(accountClient.isOurCard(OUR_CARD)).thenReturn(true);
        when(accountClient.isOurCard(FOREIGN_CARD)).thenReturn(false);
    }

    private void givenRule(int id, OperationType op, Party from, Party to, Subscription sub, String percent, String min,
                           String fixed) {
        when(rules.findByOperationTypeAndSourceTypeAndDestinationTypeAndSubscription(op, from, to, sub))
                .thenReturn(Optional.of(new CommissionRule(id, op, from, to, sub, new BigDecimal(percent),
                        new BigDecimal(min), new BigDecimal(fixed))));
    }

    @Test
    void minimumFeeAppliesToSmallAmounts() {
        givenUser(Subscription.NONE);
        givenRule(1, OperationType.SBP_PHONE, Party.WALLET, Party.SBP, Subscription.NONE, "3.00", "50", "0");

        // 3% от 100 = 3 ₽, но минимум 50 ₽
        Commission result = service.calculate(userId, walletId, OperationType.SBP_PHONE, new BigDecimal("100"), null, null);

        assertThat(result.commission()).isEqualByComparingTo("50.00");
        assertThat(result.total()).isEqualByComparingTo("150.00");
        assertThat(result.sourceType()).isEqualTo(Party.WALLET);
    }

    @Test
    void percentAboveMinimumAndFixedFee() {
        givenUser(Subscription.NONE);
        givenRule(2, OperationType.QR, Party.WALLET, Party.QR, Subscription.NONE, "1.50", "0", "10");

        // 1,5% от 1000 = 15 ₽ + 10 ₽ фиксированно
        Commission result = service.calculate(userId, walletId, OperationType.QR, new BigDecimal("1000"), null, null);

        assertThat(result.commission()).isEqualByComparingTo("25.00");
    }

    @Test
    void cardTransferTariffDependsOnWhetherCardIsOurs() {
        givenUser(Subscription.PREMIUM);
        givenRule(3, OperationType.CARD2CARD, Party.OUR_CARD, Party.OUR_CARD, Subscription.PREMIUM, "0.50", "0", "0");
        givenRule(4, OperationType.CARD2CARD, Party.OUR_CARD, Party.OTHER_BANK_CARD, Subscription.PREMIUM, "1.50", "30", "0");

        Commission ours = service.calculate(userId, cardId, OperationType.CARD2CARD, new BigDecimal("10000"), OUR_CARD, null);
        Commission foreign = service.calculate(userId, cardId, OperationType.CARD2CARD, new BigDecimal("10000"), FOREIGN_CARD, null);

        assertThat(ours.destinationType()).isEqualTo(Party.OUR_CARD);
        assertThat(ours.commission()).isEqualByComparingTo("50.00");
        assertThat(foreign.destinationType()).isEqualTo(Party.OTHER_BANK_CARD);
        assertThat(foreign.commission()).isEqualByComparingTo("150.00");
    }

    @Test
    void quoteTokenBindsRecipientAndExpiry() {
        givenUser(Subscription.NONE);
        givenRule(5, OperationType.CARD2CARD, Party.OUR_CARD, Party.OTHER_BANK_CARD, Subscription.NONE, "2.00", "50", "0");

        Commission result = service.calculate(userId, cardId, OperationType.CARD2CARD, new BigDecimal("5000"), FOREIGN_CARD, null);

        JsonNode body = json.readTree(Base64.getUrlDecoder().decode(result.quoteToken().split("\\.")[0]));
        assertThat(body.get("recipientHash").asString()).isEqualTo(CommissionService.sha256(FOREIGN_CARD));
        assertThat(body.get("destinationType").asString()).isEqualTo("OTHER_BANK_CARD");
        assertThat(body.get("commission").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(CommissionService.QUOTE_TTL));
    }

    @Test
    void unsupportedRouteIsRejected() {
        givenUser(Subscription.NONE);
        when(rules.findByOperationTypeAndSourceTypeAndDestinationTypeAndSubscription(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(userId, cardId, OperationType.SBP_PHONE, BigDecimal.TEN, null, null))
                .isInstanceOf(OperationNotAvailableException.class);
    }
}
