package com.paymentsapp.pricing.account;

import java.util.UUID;

public class AccountNotOwnedException extends RuntimeException {

    public AccountNotOwnedException(UUID accountId) {
        super("Счёт не принадлежит пользователю: " + accountId);
    }
}
