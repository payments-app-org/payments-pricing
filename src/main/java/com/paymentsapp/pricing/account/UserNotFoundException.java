package com.paymentsapp.pricing.account;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super("Пользователь не найден: " + userId);
    }
}
