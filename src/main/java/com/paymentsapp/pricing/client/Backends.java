package com.paymentsapp.pricing.client;

// Имена соседей: группы HTTP-клиентов (spring.http.serviceclient.*), экземпляры Resilience4j и кэши
public final class Backends {

    public static final String ACCOUNT = "account";
    public static final String BILLER = "biller";
    // Кэши (Redis): профиль со счетами и подпиской — на каждое изменение суммы в форме, группа поставщика — для тарифа BILL
    public static final String USER_CACHE = "account-user";
    public static final String PROVIDER_GROUP_CACHE = "provider-group";

    private Backends() {
    }
}
