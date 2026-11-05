package com.paymentsapp.pricing.account;

import com.paymentsapp.platform.resilience.ResilientCalls;
import com.paymentsapp.pricing.account.AccountClient.AccountUser;
import com.paymentsapp.pricing.client.AccountApi;
import com.paymentsapp.pricing.client.Backends;
import java.util.UUID;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

// Профиль пользователя из account (подписка и счета) — в кэше Redis на короткое время:
// котировка считается на каждое изменение суммы в форме. fresh — мимо кэша (например, после привязки новой карты).
@Component
class AccountDirectory {

    private final AccountApi api;
    private final ResilientCalls calls;

    AccountDirectory(AccountApi api, ResilientCalls calls) {
        this.api = api;
        this.calls = calls;
    }

    @Cacheable(value = Backends.USER_CACHE, unless = "#result == null")
    public AccountUser user(UUID userId) {
        return load(userId);
    }

    @CachePut(value = Backends.USER_CACHE, unless = "#result == null")
    public AccountUser fresh(UUID userId) {
        return load(userId);
    }

    private AccountUser load(UUID userId) {
        return calls.call(Backends.ACCOUNT, () -> api.user(userId));
    }
}
