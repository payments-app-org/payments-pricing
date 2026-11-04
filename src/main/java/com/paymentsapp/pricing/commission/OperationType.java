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
    // Оплата поставщику через biller: связь, госуслуги, транспорт, игры, зарубежные операторы.
    // Тариф — по группе поставщика.
    BILL
}
