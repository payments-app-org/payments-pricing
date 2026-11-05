package com.paymentsapp.pricing.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paymentsapp.platform.error.DomainException;
import com.paymentsapp.pricing.account.AccountClient;
import com.paymentsapp.pricing.account.AccountClient.AccountKind;
import com.paymentsapp.pricing.account.AccountClient.AccountRef;
import com.paymentsapp.pricing.account.AccountClient.AccountUser;
import com.paymentsapp.pricing.commission.CommissionRule;
import com.paymentsapp.pricing.commission.CommissionRuleRepository;
import com.paymentsapp.pricing.commission.OperationType;
import com.paymentsapp.pricing.commission.Party;
import com.paymentsapp.pricing.commission.Subscription;
import com.paymentsapp.pricing.error.PricingError;
import com.paymentsapp.pricing.quote.QuoteProperties;
import com.paymentsapp.pricing.quote.QuoteSigner;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class FxServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
    private static final Duration QUOTE_TTL = Duration.ofMinutes(10);
    private static final String USD = "USD";

    private final AccountClient accountClient = mock(AccountClient.class);
    private final CommissionRuleRepository rules = mock(CommissionRuleRepository.class);
    private final ExchangeRateRepository rates = mock(ExchangeRateRepository.class);
    private final QuoteProperties quoteProperties = new QuoteProperties("test-secret", QUOTE_TTL);
    private final FxService service = new FxService(accountClient, rules, rates,
            new QuoteSigner(JsonMapper.builder().build(), quoteProperties), quoteProperties, Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID userId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(accountClient.getUserWithAccount(userId, cardId)).thenReturn(new AccountUser(userId, Subscription.NONE,
                List.of(new AccountRef(cardId, AccountKind.CARD))));
        when(rates.findById(USD)).thenReturn(Optional.of(new ExchangeRate(USD, "$", new BigDecimal("95.37"))));
        when(rules.findByOperationTypeAndSourceTypeAndDestinationTypeAndSubscription(
                OperationType.ABROAD, Party.OUR_CARD, Party.ABROAD, Subscription.NONE))
                .thenReturn(Optional.of(new CommissionRule(40, OperationType.ABROAD, Party.OUR_CARD, Party.ABROAD,
                        Subscription.NONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
    }

    @Test
    void roundsInBankFavourBothWays() {
        // 1000 ₽ / 95,37 = 10,4854… → получатель получит 10,48 (вниз)
        FxQuote byRub = service.quote(userId, cardId, USD, new BigDecimal("1000"), null);
        assertThat(byRub.receiveAmount()).isEqualByComparingTo("10.48");
        assertThat(byRub.amountRub()).isEqualByComparingTo("1000.00");

        // 10,49 $ × 95,37 = 1000,4313 → спишем 1000,44 (вверх)
        FxQuote byReceive = service.quote(userId, cardId, USD, null, new BigDecimal("10.49"));
        assertThat(byReceive.amountRub()).isEqualByComparingTo("1000.44");
        assertThat(byReceive.expiresAt()).isEqualTo(NOW.plus(QUOTE_TTL));
    }

    @Test
    void exactlyOneAmountAndKnownCurrency() {
        assertThatThrownBy(() -> service.quote(userId, cardId, USD, BigDecimal.ONE, BigDecimal.ONE))
                .satisfies(e -> assertThat(((DomainException) e).getCode()).isEqualTo(PricingError.FX_AMOUNT_ONE_OF));
        assertThatThrownBy(() -> service.quote(userId, cardId, "XYZ", BigDecimal.TEN, null))
                .hasMessage("Валюта XYZ не поддерживается");
        assertThatThrownBy(() -> service.quote(userId, cardId, USD, new BigDecimal("0.001"), null))
                .satisfies(e -> assertThat(((DomainException) e).getCode()).isEqualTo(PricingError.AMOUNT_TOO_SMALL));
    }
}
