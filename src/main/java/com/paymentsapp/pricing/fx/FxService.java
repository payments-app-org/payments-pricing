package com.paymentsapp.pricing.fx;

import com.paymentsapp.pricing.account.AccountClient;
import com.paymentsapp.pricing.account.AccountClient.AccountRef;
import com.paymentsapp.pricing.account.AccountClient.AccountUser;
import com.paymentsapp.pricing.commission.CommissionRule;
import com.paymentsapp.pricing.commission.CommissionRuleRepository;
import com.paymentsapp.pricing.commission.OperationNotAvailableException;
import com.paymentsapp.pricing.commission.OperationType;
import com.paymentsapp.pricing.commission.Party;
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

// Пересчёт для перевода за рубеж в обе стороны. Округляем в пользу банка: получатель — вниз, списание — вверх.
@Service
public class FxService {

    static final Duration QUOTE_TTL = Duration.ofMinutes(10);

    private final AccountClient accountClient;
    private final CommissionRuleRepository rules;
    private final ExchangeRateRepository rates;
    private final QuoteSigner signer;
    private final Clock clock;

    @Autowired
    public FxService(AccountClient accountClient, CommissionRuleRepository rules, ExchangeRateRepository rates,
                     QuoteSigner signer) {
        this(accountClient, rules, rates, signer, Clock.systemUTC());
    }

    FxService(AccountClient accountClient, CommissionRuleRepository rules, ExchangeRateRepository rates,
              QuoteSigner signer, Clock clock) {
        this.accountClient = accountClient;
        this.rules = rules;
        this.rates = rates;
        this.signer = signer;
        this.clock = clock;
    }

    // Задаётся ровно одно: amountRub (сколько списать) или receiveAmount (сколько получить)
    public FxQuote quote(UUID userId, UUID sourceAccountId, String currency, BigDecimal amountRub, BigDecimal receiveAmount) {
        if ((amountRub == null) == (receiveAmount == null)) {
            throw new IllegalArgumentException("Укажите сумму списания или сумму к получению");
        }
        ExchangeRate rate = rates.findById(currency)
                .orElseThrow(() -> new IllegalArgumentException("Валюта " + currency + " не поддерживается"));
        AccountUser user = accountClient.getUser(userId);
        AccountRef source = user.account(sourceAccountId);
        Party sourceParty = source.isWallet() ? Party.WALLET : source.isExternal() ? Party.OTHER_BANK_CARD : Party.OUR_CARD;
        // Тариф «за рубеж» — 0%, но его id попадает в платёж, как у остальных операций
        CommissionRule rule = rules.findByOperationTypeAndSourceTypeAndDestinationTypeAndSubscription(
                        OperationType.ABROAD, sourceParty, Party.ABROAD, user.subscription())
                .orElseThrow(() -> new OperationNotAvailableException("С этого счёта перевод за рубеж недоступен"));

        BigDecimal r = rate.getSellRate();
        BigDecimal rub = amountRub != null ? amountRub.setScale(2, RoundingMode.HALF_UP)
                : receiveAmount.multiply(r).setScale(2, RoundingMode.CEILING);
        BigDecimal receive = receiveAmount != null ? receiveAmount.setScale(2, RoundingMode.HALF_UP)
                : rub.divide(r, 2, RoundingMode.FLOOR);
        if (rub.signum() <= 0 || receive.signum() <= 0) {
            throw new IllegalArgumentException("Слишком маленькая сумма");
        }
        BigDecimal commission = rule.commissionFor(rub);

        Quote quote = new Quote(UUID.randomUUID(), userId, sourceAccountId, OperationType.ABROAD.name(),
                Party.ABROAD.name(), null, rub, commission, rule.getId(), Instant.now(clock).plus(QUOTE_TTL),
                currency, receive, r);
        return new FxQuote(rub, currency, rate.getSymbol(), receive, r, commission, rub.add(commission),
                quote.quoteId(), quote.expiresAt(), signer.sign(quote));
    }
}
