package com.paymentsapp.pricing.commission;

public enum OperationType {
    SBP_PHONE,
    CARD2CARD,
    OWN_TRANSFER,
    ABROAD,
    QR,
    // На «Мой кошелёк» другого клиента по номеру телефона
    WALLET_TRANSFER,
    // По реквизитам юрлицу: в другой банк / на счёт в нашем банке
    REQUISITES,
    REQUISITES_OUR,
    // Госуслуги по УИН
    UIN_PAYMENT,
    // Мобильная связь любого оператора
    MOBILE_PAYMENT
}
