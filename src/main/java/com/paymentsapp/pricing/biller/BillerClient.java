package com.paymentsapp.pricing.biller;

import static com.paymentsapp.pricing.error.PricingError.BILLER_UNAVAILABLE;

import com.paymentsapp.platform.error.DomainException;
import com.paymentsapp.platform.resilience.ResilientCalls;
import com.paymentsapp.pricing.client.Backends;
import com.paymentsapp.pricing.client.BillerApi;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

// biller: к какой группе относится поставщик (от группы зависит тариф оплаты BILL). Справочник — в кэше Redis;
// «поставщик не найден» (Optional.empty) не кэшируем. Сбой biller → PricingError.BILLER_UNAVAILABLE.
@Component
public class BillerClient {

    private final BillerApi api;
    private final ResilientCalls calls;

    public BillerClient(BillerApi api, ResilientCalls calls) {
        this.api = api;
        this.calls = calls;
    }

    @Cacheable(value = Backends.PROVIDER_GROUP_CACHE, unless = "#result == null")
    public Optional<String> providerGroup(String providerCode) {
        try {
            return calls.call(Backends.BILLER, () -> {
                try {
                    return Optional.ofNullable(api.provider(providerCode)).map(d -> d.provider().groupCode());
                } catch (HttpClientErrorException.NotFound e) {
                    return Optional.empty();
                }
            });
        } catch (RestClientException e) {
            throw DomainException.withCause(BILLER_UNAVAILABLE, e);
        }
    }
}
