package com.paymentsapp.pricing.commission;

import static com.paymentsapp.pricing.error.PricingError.*;

import com.paymentsapp.platform.crypto.Hashing;
import com.paymentsapp.platform.error.DomainException;
import com.paymentsapp.pricing.account.AccountClient;
import com.paymentsapp.pricing.account.AccountClient.AccountUser;
import com.paymentsapp.pricing.biller.BillerClient;
import com.paymentsapp.pricing.quote.Quote;
import com.paymentsapp.pricing.quote.QuoteProperties;
import com.paymentsapp.pricing.quote.QuoteSigner;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CommissionService {

    private final AccountClient accountClient;
    private final BillerClient billerClient;
    private final CommissionRuleRepository rules;
    private final QuoteSigner signer;
    private final QuoteProperties quoteProperties;
    private final Clock clock;

    public CommissionService(AccountClient accountClient, BillerClient billerClient, CommissionRuleRepository rules,
                             QuoteSigner signer, QuoteProperties quoteProperties, Clock clock) {
        this.accountClient = accountClient;
        this.billerClient = billerClient;
        this.rules = rules;
        this.signer = signer;
        this.quoteProperties = quoteProperties;
        this.clock = clock;
    }

    // recipientCardNumber — для перевода по карте, destinationAccountId — для перевода между своими счетами
    public Commission calculate(UUID userId, UUID sourceAccountId, OperationType operationType, BigDecimal amount,
                                String recipientCardNumber, UUID destinationAccountId) {
        return calculate(userId, sourceAccountId, operationType, amount, recipientCardNumber, destinationAccountId, null);
    }

    // providerCode — для оплаты поставщику (BILL): тариф по группе поставщика из biller
    public Commission calculate(UUID userId, UUID sourceAccountId, OperationType operationType, BigDecimal amount,
                                String recipientCardNumber, UUID destinationAccountId, String providerCode) {
        AccountUser user = accountClient.getUserWithAccount(userId, sourceAccountId);
        AccountClient.AccountRef sourceAccount = user.account(sourceAccountId);
        Party source = sourceAccount.isWallet() ? Party.WALLET
                : sourceAccount.isSbpSource() ? Party.SBP
                : sourceAccount.isExternal() ? Party.OTHER_BANK_CARD : Party.OUR_CARD;

        String recipientKey = null;
        Party destination = switch (operationType) {
            case SBP_PHONE -> Party.SBP;
            case ABROAD -> Party.ABROAD;
            case QR -> Party.QR;
            case WALLET_TRANSFER -> Party.WALLET;
            case REQUISITES -> Party.LEGAL_ENTITY;
            case REQUISITES_OUR -> Party.OUR_LEGAL_ENTITY;
            case BILL -> {
                if (providerCode == null || providerCode.isBlank()) {
                    throw new DomainException(PROVIDER_REQUIRED);
                }
                recipientKey = providerCode;
                String group = billerClient.providerGroup(providerCode)
                        .orElseThrow(() -> new DomainException(PROVIDER_NOT_FOUND));
                yield billParty(group);
            }
            case CARD2CARD -> {
                if (recipientCardNumber == null) {
                    throw new DomainException(RECIPIENT_CARD_REQUIRED);
                }
                recipientKey = recipientCardNumber;
                yield accountClient.isOurCard(recipientCardNumber) ? Party.OUR_CARD : Party.OTHER_BANK_CARD;
            }
            case OWN_TRANSFER -> {
                if (destinationAccountId == null) {
                    throw new DomainException(DESTINATION_REQUIRED);
                }
                recipientKey = destinationAccountId.toString();
                AccountClient.AccountRef target = accountClient.getUserWithAccount(userId, destinationAccountId)
                        .account(destinationAccountId);
                yield target.isWallet() ? Party.WALLET : target.isExternal() ? Party.OTHER_BANK_CARD : Party.OUR_CARD;
            }
        };

        CommissionRule rule = rules.findByOperationTypeAndSourceTypeAndDestinationTypeAndSubscription(
                        operationType, source, destination, user.subscription())
                .orElseThrow(() -> new DomainException(OPERATION_NOT_AVAILABLE));

        // Бесплатный лимит в месяц (переводы по СБП) — считаем от того, сколько уже переведено
        BigDecimal used = rule.getFreeMonthlyAmount() == null ? BigDecimal.ZERO
                : accountClient.monthUsage(userId, operationType.name());
        BigDecimal commission = rule.commissionFor(amount, used);
        BigDecimal freeLeft = rule.getFreeMonthlyAmount() == null ? null : rule.freeLeft(used);
        Quote quote = new Quote(UUID.randomUUID(), userId, sourceAccountId, operationType.name(), destination.name(),
                recipientKey == null ? null : sha256(recipientKey), amount, commission, rule.getId(),
                Instant.now(clock).plus(quoteProperties.ttl()));
        return new Commission(operationType, user.subscription(), source, destination, amount, rule.getPercent(),
                rule.getMinFee(), rule.getFixedFee(), rule.getMaxFee(), rule.getFreeMonthlyAmount(), freeLeft, commission, amount.add(commission), rule.getId(), quote.quoteId(),
                quote.expiresAt(), signer.sign(quote));
    }

    // Группа поставщика из biller — получатель в тарифной сетке
    private static Party billParty(String group) {
        try {
            return Party.valueOf(group);
        } catch (IllegalArgumentException e) {
            throw new DomainException(OPERATION_NOT_AVAILABLE);
        }
    }

    static String sha256(String value) {
        return Hashing.sha256Hex(value);
    }
}
