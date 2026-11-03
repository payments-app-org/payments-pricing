package com.paymentsapp.pricing.commission;

// Для такой пары «откуда → куда» тарифа нет — операция не поддерживается
public class OperationNotAvailableException extends RuntimeException {

    public OperationNotAvailableException(String message) {
        super(message);
    }
}
