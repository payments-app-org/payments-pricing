package com.paymentsapp.pricing.account;

import static com.paymentsapp.pricing.error.PricingError.*;

import com.paymentsapp.platform.error.DomainException;
import com.paymentsapp.platform.resilience.ResilientCalls;
import com.paymentsapp.pricing.client.AccountApi;
import com.paymentsapp.pricing.client.Backends;
import com.paymentsapp.pricing.commission.Subscription;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

// account: подписка и счета пользователя (из кэша), чья карта получателя, сколько потрачено на вид операции за месяц.
// Сбой account → PricingError.ACCOUNT_UNAVAILABLE.
@Component
public class AccountClient {

    private final AccountDirectory directory;
    private final AccountApi api;
    private final ResilientCalls calls;

    AccountClient(AccountDirectory directory, AccountApi api, ResilientCalls calls) {
        this.directory = directory;
        this.api = api;
        this.calls = calls;
    }

    public AccountUser getUser(UUID userId) {
        return unavailableAsDomain(() -> directory.user(userId));
    }

    // Профиль, в котором точно есть этот счёт: в кэше его может ещё не быть (карту только что привязали)
    public AccountUser getUserWithAccount(UUID userId, UUID accountId) {
        AccountUser cached = getUser(userId);
        if (cached.find(accountId).isPresent()) {
            return cached;
        }
        AccountUser fresh = unavailableAsDomain(() -> directory.fresh(userId));
        fresh.account(accountId);
        return fresh;
    }

    // Карта выпущена нашим банком (есть у нас в account). Номер карты в кэш не кладём.
    public boolean isOurCard(String cardNumber) {
        return unavailableAsDomain(() -> calls.call(Backends.ACCOUNT, () -> {
            try {
                api.recipientByCard(cardNumber);
                return true;
            } catch (HttpClientErrorException.NotFound e) {
                return false;
            }
        }));
    }

    // Сколько клиент потратил на операции этого вида с начала месяца
    public BigDecimal monthUsage(UUID userId, String operationType) {
        AccountApi.OperationUsage usage = unavailableAsDomain(() -> calls.call(Backends.ACCOUNT,
                () -> api.operationUsage(userId, operationType)));
        return usage == null || usage.month() == null ? BigDecimal.ZERO : usage.month();
    }

    private static <T> T unavailableAsDomain(Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw new DomainException(USER_NOT_FOUND);
        } catch (RestClientException e) {
            throw DomainException.withCause(ACCOUNT_UNAVAILABLE, e);
        }
    }

    // Serializable — лежит в кэше Redis
    public record AccountUser(UUID id, Subscription subscription, List<AccountRef> accounts) implements Serializable {

        public AccountRef account(UUID accountId) {
            return find(accountId).orElseThrow(() -> new DomainException(ACCOUNT_NOT_OWNED));
        }

        Optional<AccountRef> find(UUID accountId) {
            return accounts.stream().filter(a -> a.id().equals(accountId)).findFirst();
        }
    }

    public record AccountRef(UUID id, AccountKind type) implements Serializable {

        public boolean isWallet() {
            return type == AccountKind.WALLET;
        }

        // Привязанная карта или свой счёт в другом банке
        public boolean isExternal() {
            return type == AccountKind.EXTERNAL_CARD || isSbpSource();
        }

        // Свой счёт в другом банке: пополнение по СБП (me2me)
        public boolean isSbpSource() {
            return type == AccountKind.EXTERNAL_SBP;
        }
    }

    public enum AccountKind {
        WALLET,
        CARD,
        EXTERNAL_CARD,
        EXTERNAL_SBP
    }
}
