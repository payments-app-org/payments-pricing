package com.paymentsapp.pricing.commission;

import com.paymentsapp.pricing.account.AccountClient;
import com.paymentsapp.pricing.account.AccountClient.AccountUser;
import com.paymentsapp.pricing.quote.Quote;
import com.paymentsapp.pricing.quote.QuoteSigner;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommissionService {

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

    // recipientCardNumber — для перевода по карте, destinationAccountId — для перевода между своими счетами
    public Commission calculate(UUID userId, UUID sourceAccountId, OperationType operationType, BigDecimal amount,
                                String recipientCardNumber, UUID destinationAccountId) {
        AccountUser user = accountClient.getUser(userId);
        AccountClient.AccountRef sourceAccount = user.account(sourceAccountId);
        Party source = sourceAccount.isWallet() ? Party.WALLET
                : sourceAccount.isExternal() ? Party.OTHER_BANK_CARD : Party.OUR_CARD;

        String recipientKey = null;
        Party destination = switch (operationType) {
            case SBP_PHONE -> Party.SBP;
            case ABROAD -> Party.ABROAD;
            case QR -> Party.QR;
            case WALLET_TRANSFER -> Party.WALLET;
            case REQUISITES -> Party.LEGAL_ENTITY;
            case REQUISITES_OUR -> Party.OUR_LEGAL_ENTITY;
            case UIN_PAYMENT -> Party.GOV;
            case CARD2CARD -> {
                if (recipientCardNumber == null) {
                    throw new IllegalArgumentException("Не указан номер карты получателя");
                }
                recipientKey = recipientCardNumber;
                yield accountClient.isOurCard(recipientCardNumber) ? Party.OUR_CARD : Party.OTHER_BANK_CARD;
            }
            case OWN_TRANSFER -> {
                if (destinationAccountId == null) {
                    throw new IllegalArgumentException("Не выбран счёт зачисления");
                }
                recipientKey = destinationAccountId.toString();
                AccountClient.AccountRef target = user.account(destinationAccountId);
                yield target.isWallet() ? Party.WALLET : target.isExternal() ? Party.OTHER_BANK_CARD : Party.OUR_CARD;
            }
        };

        CommissionRule rule = rules.findByOperationTypeAndSourceTypeAndDestinationTypeAndSubscription(
                        operationType, source, destination, user.subscription())
                .orElseThrow(() -> new OperationNotAvailableException("Такой перевод недоступен"));

        BigDecimal commission = rule.commissionFor(amount);
        Quote quote = new Quote(UUID.randomUUID(), userId, sourceAccountId, operationType.name(), destination.name(),
                recipientKey == null ? null : sha256(recipientKey), amount, commission, rule.getId(),
                Instant.now(clock).plus(QUOTE_TTL));
        return new Commission(operationType, user.subscription(), source, destination, amount, rule.getPercent(),
                rule.getMinFee(), rule.getFixedFee(), commission, amount.add(commission), rule.getId(), quote.quoteId(),
                quote.expiresAt(), signer.sign(quote));
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
