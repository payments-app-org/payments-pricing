package com.paymentsapp.pricing.client;

import com.paymentsapp.pricing.account.AccountClient.AccountUser;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface AccountApi {

    @GetExchange("/api/users/{userId}")
    AccountUser user(@PathVariable UUID userId);

    // 200 — карта нашего банка, 404 — чужая
    @GetExchange("/api/internal/recipients")
    void recipientByCard(@RequestParam("cardNumber") String cardNumber);

    @GetExchange("/api/users/{userId}/operation-usage")
    OperationUsage operationUsage(@PathVariable UUID userId, @RequestParam("operationType") String operationType);

    record OperationUsage(String operationType, BigDecimal month) {
    }
}
