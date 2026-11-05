package com.paymentsapp.pricing.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/api/biller")
public interface BillerApi {

    @GetExchange("/providers/{code}")
    ProviderDetails provider(@PathVariable String code);

    record ProviderDetails(Provider provider) {
    }

    record Provider(String code, String groupCode) {
    }
}
