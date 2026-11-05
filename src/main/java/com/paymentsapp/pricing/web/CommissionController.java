package com.paymentsapp.pricing.web;

import com.paymentsapp.pricing.account.AccountClient;
import com.paymentsapp.pricing.commission.Commission;
import com.paymentsapp.pricing.commission.CommissionService;
import com.paymentsapp.pricing.commission.Party;
import com.paymentsapp.pricing.commission.Subscription;
import com.paymentsapp.pricing.commission.TariffService;
import com.paymentsapp.pricing.fx.FxQuote;
import com.paymentsapp.pricing.fx.FxService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
public class CommissionController {

    private final CommissionService commissionService;
    private final TariffService tariffs;
    private final AccountClient accountClient;
    private final FxService fx;

    public CommissionController(CommissionService commissionService, TariffService tariffs, AccountClient accountClient,
                                FxService fx) {
        this.commissionService = commissionService;
        this.tariffs = tariffs;
        this.accountClient = accountClient;
        this.fx = fx;
    }

    // Перевод за рубеж: пересчёт по курсу в обе стороны — по сумме списания или по сумме к получению
    @PostMapping("/fx-quote")
    public FxQuote fxQuote(@Valid @RequestBody FxQuoteRequest req) {
        return fx.quote(req.userId(), req.sourceAccountId(), req.currency(), req.amountRub(), req.receiveAmount());
    }

    // Сумма — в рублях (amountRub) или к получению (receiveAmount); что из них задано, проверяет FxService
    public record FxQuoteRequest(@NotNull UUID userId, @NotNull UUID sourceAccountId,
                                 @NotNull @Pattern(regexp = "[A-Z]{3}", message = "код валюты — три заглавные латинские буквы") String currency,
                                 @Positive @Digits(integer = 15, fraction = 2) BigDecimal amountRub,
                                 @Positive @Digits(integer = 15, fraction = 2) BigDecimal receiveAmount) {
    }

    @PostMapping("/commission")
    public Commission calculate(@Valid @RequestBody CommissionRequest request) {
        return commissionService.calculate(request.userId(), request.sourceAccountId(), request.operationType(),
                request.amount(), request.recipientCardNumber(), request.destinationAccountId(), request.providerCode());
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
