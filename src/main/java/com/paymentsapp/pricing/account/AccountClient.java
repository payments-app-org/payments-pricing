package com.paymentsapp.pricing.account;

import com.paymentsapp.pricing.commission.Subscription;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// REST-клиент сервиса account: подписка и счета пользователя, принадлежность карты нашему банку
@Component
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(RestClient.Builder builder, @Value("${account.url}") String accountUrl) {
        this.restClient = builder.baseUrl(accountUrl).build();
    }

    public AccountUser getUser(UUID userId) {
        return restClient.get()
                .uri("/api/users/{userId}", userId)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                    throw new UserNotFoundException(userId);
                })
                .body(AccountUser.class);
    }

    // Карта выпущена нашим банком (есть у нас в account)
    public boolean isOurCard(String cardNumber) {
        return restClient.get()
                .uri("/api/internal/recipients?cardNumber={cardNumber}", cardNumber)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                })
                .toBodilessEntity()
                .getStatusCode().is2xxSuccessful();
    }

    public record AccountUser(UUID id, Subscription subscription, List<AccountRef> accounts) {

        public AccountRef account(UUID accountId) {
            return accounts.stream().filter(a -> a.id().equals(accountId)).findFirst()
                    .orElseThrow(() -> new AccountNotOwnedException(accountId));
        }
    }

    public record AccountRef(UUID id, String type) {

        public boolean isWallet() {
            return "WALLET".equals(type);
        }

        // Привязанная карта другого банка
        public boolean isExternal() {
            return "EXTERNAL_CARD".equals(type);
        }
    }
}
