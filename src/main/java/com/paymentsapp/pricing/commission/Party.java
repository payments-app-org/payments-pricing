package com.paymentsapp.pricing.commission;

// Откуда списываем и куда зачисляем — от этого зависит тариф
public enum Party {
    WALLET,             // «Мой кошелёк»
    OUR_CARD,           // карта нашего банка
    PARTNER_CARD,       // карта партнёрского сервиса
    OTHER_BANK_CARD,    // карта другого банка
    OPERATOR_ACCOUNT,   // лицевой счёт мобильного оператора
    SBP,                // перевод через СБП
    MOBILE,             // оплата сотовой связи
    ABROAD,             // перевод за рубеж
    QR                  // оплата по QR
}
