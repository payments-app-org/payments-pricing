package com.paymentsapp.pricing.web;

import com.paymentsapp.pricing.commission.Commission;
import com.paymentsapp.pricing.account.AccountClient;
import com.paymentsapp.pricing.commission.CommissionService;
import com.paymentsapp.pricing.commission.Party;
import com.paymentsapp.pricing.commission.Subscription;
import com.paymentsapp.pricing.commission.TariffService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
public class CommissionController {

    private final CommissionService commissionService;
    private final TariffService tariffs;
    private final AccountClient accountClient;

    public CommissionController(CommissionService commissionService, TariffService tariffs, AccountClient accountClient) {
        this.commissionService = commissionService;
        this.tariffs = tariffs;
        this.accountClient = accountClient;
    }

    @PostMapping("/commission")
    public Commission calculate(@Valid @RequestBody CommissionRequest request) {
        return commissionService.calculate(request.userId(), request.sourceAccountId(), request.operationType(),
                request.amount(), request.recipientCardNumber(), request.destinationAccountId());
    }

    // Комиссии для экрана «Лимиты и условия счёта» — по подписке пользователя.
    // account=WALLET — условия «Моего кошелька», OUR_CARD — карты.
    @GetMapping("/tariffs")
    public Tariffs tariffs(@RequestParam UUID userId, @RequestParam(defaultValue = "WALLET") Party account) {
        Subscription subscription = accountClient.getUser(userId).subscription();
        return new Tariffs(subscription, tariffs.tariffs(subscription, account));
    }

    public record Tariffs(Subscription subscription, List<TariffService.Section> sections) {
    }
}
