package com.paymentsapp.pricing.commission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paymentsapp.platform.error.DomainException;
import com.paymentsapp.pricing.account.AccountClient;
import com.paymentsapp.pricing.account.AccountClient.AccountKind;
import com.paymentsapp.pricing.account.AccountClient.AccountRef;
import com.paymentsapp.pricing.account.AccountClient.AccountUser;
import com.paymentsapp.pricing.biller.BillerClient;
import com.paymentsapp.pricing.error.PricingError;
import com.paymentsapp.pricing.quote.QuoteProperties;
import com.paymentsapp.pricing.quote.QuoteSigner;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
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
    private static final Duration QUOTE_TTL = Duration.ofMinutes(10);
    private static final String OUR_CARD = "2200300456781002";
    private static final String FOREIGN_CARD = "4111111111111111";
    private static final String MOBILE_PROVIDER = "yota";

    private final AccountClient accountClient = mock(AccountClient.class);
    private final BillerClient billerClient = mock(BillerClient.class);
    private final CommissionRuleRepository rules = mock(CommissionRuleRepository.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final QuoteProperties quoteProperties = new QuoteProperties("test-secret", QUOTE_TTL);
    private final CommissionService service = new CommissionService(accountClient, billerClient, rules,
            new QuoteSigner(json, quoteProperties), quoteProperties, Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID userId = UUID.randomUUID();
    private final UUID walletId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();
    private final UUID linkedCardId = UUID.randomUUID();

    private void givenUser(Subscription subscription) {
        AccountUser user = new AccountUser(userId, subscription, List.of(new AccountRef(walletId, AccountKind.WALLET),
                new AccountRef(cardId, AccountKind.CARD), new AccountRef(linkedCardId, AccountKind.EXTERNAL_CARD)));
        when(accountClient.getUserWithAccount(any(), any())).thenReturn(user);
        when(accountClient.isOurCard(OUR_CARD)).thenReturn(true);
        when(accountClient.isOurCard(FOREIGN_CARD)).thenReturn(false);
    }

    private void givenRule(int id, OperationType op, Party from, Party to, Subscription sub, String percent, String min,
                           String fixed) {
        when(rules.findByOperationTypeAndSourceTypeAndDestinationTypeAndSubscription(op, from, to, sub))
                .thenReturn(Optional.of(new CommissionRule(id, op, from, to, sub, new BigDecimal(percent),
                        new BigDecimal(min), new BigDecimal(fixed))));
    }

    private static PricingError codeOf(Throwable e) {
        return (PricingError) ((DomainException) e).getCode();
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
    void linkedCardIsOtherBankSource() {
        givenUser(Subscription.NONE);
        givenRule(7, OperationType.OWN_TRANSFER, Party.OTHER_BANK_CARD, Party.WALLET, Subscription.NONE, "0", "0", "0");

        Commission result = service.calculate(userId, linkedCardId, OperationType.OWN_TRANSFER, new BigDecimal("500"), null, walletId);

        assertThat(result.sourceType()).isEqualTo(Party.OTHER_BANK_CARD);
        assertThat(result.destinationType()).isEqualTo(Party.WALLET);
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
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(QUOTE_TTL));
    }

    @Test
    void billTariffComesFromProviderGroup() {
        givenUser(Subscription.NONE);
        when(billerClient.providerGroup(MOBILE_PROVIDER)).thenReturn(Optional.of(Party.MOBILE.name()));
        givenRule(8, OperationType.BILL, Party.OUR_CARD, Party.MOBILE, Subscription.NONE, "0", "0", "0");

        Commission result = service.calculate(userId, cardId, OperationType.BILL, new BigDecimal("300"), null, null,
                MOBILE_PROVIDER);

        assertThat(result.destinationType()).isEqualTo(Party.MOBILE);
        assertThat(result.commission()).isEqualByComparingTo("0");
    }

    @Test
    void billRejectsMissingOrUnknownProvider() {
        givenUser(Subscription.NONE);
        when(billerClient.providerGroup("nope")).thenReturn(Optional.empty());
        when(billerClient.providerGroup("odd")).thenReturn(Optional.of("NOT_A_TARIFF_GROUP"));

        assertThatThrownBy(() -> service.calculate(userId, cardId, OperationType.BILL, BigDecimal.TEN, null, null, " "))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(PricingError.PROVIDER_REQUIRED));
        assertThatThrownBy(() -> service.calculate(userId, cardId, OperationType.BILL, BigDecimal.TEN, null, null, "nope"))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(PricingError.PROVIDER_NOT_FOUND));
        assertThatThrownBy(() -> service.calculate(userId, cardId, OperationType.BILL, BigDecimal.TEN, null, null, "odd"))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(PricingError.OPERATION_NOT_AVAILABLE));
    }

    @Test
    void unsupportedRouteIsRejected() {
        givenUser(Subscription.NONE);
        when(rules.findByOperationTypeAndSourceTypeAndDestinationTypeAndSubscription(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(userId, cardId, OperationType.SBP_PHONE, BigDecimal.TEN, null, null))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(PricingError.OPERATION_NOT_AVAILABLE));
    }

    @Test
    void sbpIsFreeWithinMonthlyLimitAndChargedOnlyOnExcess() {
        givenUser(Subscription.NONE);
        when(rules.findByOperationTypeAndSourceTypeAndDestinationTypeAndSubscription(
                OperationType.SBP_PHONE, Party.OUR_CARD, Party.SBP, Subscription.NONE))
                .thenReturn(Optional.of(new CommissionRule(6, OperationType.SBP_PHONE, Party.OUR_CARD, Party.SBP,
                        Subscription.NONE, new BigDecimal("0.50"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("1500"), new BigDecimal("100000"))));

        // Перевели 90 000: ещё 10 000 бесплатно, 0,5% — с 20 000 превышения
        when(accountClient.monthUsage(userId, "SBP_PHONE")).thenReturn(new BigDecimal("90000"));
        Commission partly = service.calculate(userId, cardId, OperationType.SBP_PHONE, new BigDecimal("30000"), null, null);
        assertThat(partly.commission()).isEqualByComparingTo("100.00");
        assertThat(partly.freeMonthlyLeft()).isEqualByComparingTo("10000");

        // Лимит исчерпан: 0,5% от 1 000 000 = 5 000, но не больше 1 500
        when(accountClient.monthUsage(userId, "SBP_PHONE")).thenReturn(new BigDecimal("100000"));
        Commission capped = service.calculate(userId, cardId, OperationType.SBP_PHONE, new BigDecimal("1000000"), null, null);
        assertThat(capped.commission()).isEqualByComparingTo("1500.00");

        // В пределах лимита — бесплатно
        when(accountClient.monthUsage(userId, "SBP_PHONE")).thenReturn(BigDecimal.ZERO);
        Commission free = service.calculate(userId, cardId, OperationType.SBP_PHONE, new BigDecimal("50000"), null, null);
        assertThat(free.commission()).isEqualByComparingTo("0");
    }
}
