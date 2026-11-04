package com.paymentsapp.pricing.biller;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

// REST-клиент biller: к какой группе относится поставщик (от группы зависит тариф оплаты BILL)
@Component
public class BillerClient {

    private final RestClient rest;

    public BillerClient(RestClient.Builder builder, @Value("${biller.url}") String url) {
        this.rest = builder.baseUrl(url).build();
    }

    public Optional<String> providerGroup(String providerCode) {
        try {
            ProviderDetails d = rest.get().uri("/api/biller/providers/{code}", providerCode).retrieve().body(ProviderDetails.class);
            return Optional.ofNullable(d).map(x -> x.provider().groupCode());
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    record ProviderDetails(Provider provider) {
    }

    record Provider(String code, String groupCode) {
    }
}
