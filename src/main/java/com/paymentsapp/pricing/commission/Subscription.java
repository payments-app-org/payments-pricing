package com.paymentsapp.pricing.commission;

// Своя копия перечня подписок: сервисы не делят код, договариваются через API
public enum Subscription {
    NONE,
    // «МТС Premium» — развлекательная подписка: банковские тарифы как без подписки
    MTS_PREMIUM,
    // Пакет услуг «МТС Банк Premium»: бесплатные переводы по СБП до 1,5 млн ₽ в месяц
    BANK_PREMIUM
}
