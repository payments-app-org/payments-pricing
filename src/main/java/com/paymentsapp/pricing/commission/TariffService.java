package com.paymentsapp.pricing.commission;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

// Условия счёта для экрана «Лимиты и условия»: тарифы пользователя по разделам,
// в каждом — «с этого счёта» (outgoing) и «на этот счёт» (incoming)
@Service
public class TariffService {

    private static final Map<OperationType, String> SECTIONS = new LinkedHashMap<>();

    static {
        SECTIONS.put(OperationType.OWN_TRANSFER, "Переводы между счетами");
        SECTIONS.put(OperationType.ABROAD, "Переводы за рубеж");
        SECTIONS.put(OperationType.SBP_PHONE, "Переводы по номеру телефона");
        SECTIONS.put(OperationType.CARD2CARD, "Переводы по номеру карты");
        SECTIONS.put(OperationType.QR, "Оплата по QR");
    }

    private final CommissionRuleRepository rules;

    public TariffService(CommissionRuleRepository rules) {
        this.rules = rules;
    }

    public List<Section> tariffs(Subscription subscription, Party account) {
        List<CommissionRule> all = rules.findBySubscriptionOrderBySortOrder(subscription);
        List<Section> sections = new ArrayList<>();
        SECTIONS.forEach((op, title) -> {
            List<Row> outgoing = all.stream()
                    .filter(r -> r.getOperationType() == op && r.getSourceType() == account)
                    .map(Row::of).toList();
            List<Row> incoming = all.stream()
                    .filter(r -> r.getOperationType() == op && r.getDestinationType() == account && r.getSourceType() != account)
                    .map(Row::of).toList();
            if (!outgoing.isEmpty() || !incoming.isEmpty()) {
                sections.add(new Section(op, title, outgoing, incoming));
            }
        });
        return sections;
    }

    public record Section(OperationType operationType, String title, List<Row> outgoing, List<Row> incoming) {
    }

    public record Row(String label, Party sourceType, Party destinationType, BigDecimal percent, BigDecimal minFee,
                      BigDecimal fixedFee) {

        static Row of(CommissionRule r) {
            return new Row(r.getLabel(), r.getSourceType(), r.getDestinationType(), r.getPercent(), r.getMinFee(),
                    r.getFixedFee());
        }
    }
}
