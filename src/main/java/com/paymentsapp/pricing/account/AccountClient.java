package com.paymentsapp.pricing.account;

import com.paymentsapp.pricing.commission.Subscription;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// REST-клиент сервиса account: подписка пользователя и проверка, что счёт списания — его
@Component
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(RestClient.Builder builder, @Value("${account.url}") String accountUrl) {
        this.restClient = builder.baseUrl(accountUrl).build();
    }

    public Subscription getSubscription(UUID userId, UUID sourceAccountId) {
        AccountUser user = restClient.get()
                .uri("/api/users/{userId}", userId)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                    throw new UserNotFoundException(userId);
                })
                .body(AccountUser.class);
        if (user.accounts().stream().noneMatch(a -> a.id().equals(sourceAccountId))) {
            throw new AccountNotOwnedException(sourceAccountId);
        }
        return user.subscription();
    }

    record AccountUser(UUID id, Subscription subscription, List<AccountRef> accounts) {
    }

    record AccountRef(UUID id) {
    }
}
