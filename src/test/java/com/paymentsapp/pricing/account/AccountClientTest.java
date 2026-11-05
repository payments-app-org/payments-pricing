package com.paymentsapp.pricing.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paymentsapp.platform.error.DomainException;
import com.paymentsapp.platform.resilience.ResilientCalls;
import com.paymentsapp.platform.resilience.ServiceUnavailableException;
import com.paymentsapp.pricing.account.AccountClient.AccountKind;
import com.paymentsapp.pricing.account.AccountClient.AccountRef;
import com.paymentsapp.pricing.account.AccountClient.AccountUser;
import com.paymentsapp.pricing.client.AccountApi;
import com.paymentsapp.pricing.commission.Subscription;
import com.paymentsapp.pricing.error.PricingError;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountClientTest {

    private final AccountDirectory directory = mock(AccountDirectory.class);
    private final AccountClient client = new AccountClient(directory, mock(AccountApi.class), mock(ResilientCalls.class));
    private final UUID userId = UUID.randomUUID();
    private final UUID walletId = UUID.randomUUID();
    private final UUID newCardId = UUID.randomUUID();

    private AccountUser user(UUID... accounts) {
        return new AccountUser(userId, Subscription.NONE,
                Arrays.stream(accounts).map(id -> new AccountRef(id, AccountKind.WALLET)).toList());
    }

    @Test
    void cachedProfileIsEnoughWhenAccountIsThere() {
        when(directory.user(userId)).thenReturn(user(walletId));

        assertThat(client.getUserWithAccount(userId, walletId).account(walletId).isWallet()).isTrue();
        verify(directory, never()).fresh(userId);
    }

    @Test
    void justLinkedCardRefreshesCachedProfile() {
        when(directory.user(userId)).thenReturn(user(walletId));
        when(directory.fresh(userId)).thenReturn(user(walletId, newCardId));

        AccountUser result = client.getUserWithAccount(userId, newCardId);

        assertThat(result.accounts()).hasSize(2);
        verify(directory).fresh(userId);
    }

    @Test
    void foreignAccountIsRejectedAfterRefresh() {
        when(directory.user(userId)).thenReturn(user(walletId));
        when(directory.fresh(userId)).thenReturn(user(walletId));

        assertThatThrownBy(() -> client.getUserWithAccount(userId, newCardId))
                .satisfies(e -> assertThat(((DomainException) e).getCode()).isEqualTo(PricingError.ACCOUNT_NOT_OWNED));
    }

    @Test
    void accountOutageBecomesDomainError() {
        when(directory.user(userId)).thenThrow(new ServiceUnavailableException("account", new RuntimeException("open")));

        assertThatThrownBy(() -> client.getUser(userId))
                .satisfies(e -> assertThat(((DomainException) e).getCode()).isEqualTo(PricingError.ACCOUNT_UNAVAILABLE));
    }
}
