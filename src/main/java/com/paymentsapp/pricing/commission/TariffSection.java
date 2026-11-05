package com.paymentsapp.pricing.commission;

// Разделы экрана «Лимиты и условия счёта» — в порядке показа
enum TariffSection {
    OWN_TRANSFER(OperationType.OWN_TRANSFER, "Переводы между счетами"),
    ABROAD(OperationType.ABROAD, "Переводы за рубеж"),
    SBP_PHONE(OperationType.SBP_PHONE, "Переводы по номеру телефона"),
    CARD2CARD(OperationType.CARD2CARD, "Переводы по номеру карты"),
    QR(OperationType.QR, "Оплата по QR");

    private final OperationType operationType;
    private final String title;

    TariffSection(OperationType operationType, String title) {
        this.operationType = operationType;
        this.title = title;
    }

    OperationType operationType() {
        return operationType;
    }

    String title() {
        return title;
    }
}
