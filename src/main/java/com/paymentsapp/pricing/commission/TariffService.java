package com.paymentsapp.pricing.commission;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

// Условия счёта для экрана «Лимиты и условия»: тарифы пользователя по разделам,
// в каждом — «с этого счёта» (outgoing) и «на этот счёт» (incoming)
@Service
public class TariffService {

    private final CommissionRuleRepository rules;

    public TariffService(CommissionRuleRepository rules) {
        this.rules = rules;
    }

    public List<Section> tariffs(Subscription subscription, Party account) {
        List<CommissionRule> all = rules.findBySubscriptionOrderBySortOrder(subscription);
        List<Section> sections = new ArrayList<>();
        for (TariffSection section : TariffSection.values()) {
            OperationType op = section.operationType();
            List<Row> outgoing = all.stream()
                    .filter(r -> r.getOperationType() == op && r.getSourceType() == account)
                    .map(Row::of).toList();
            List<Row> incoming = all.stream()
                    .filter(r -> r.getOperationType() == op && r.getDestinationType() == account && r.getSourceType() != account)
                    .map(Row::of).toList();
            if (!outgoing.isEmpty() || !incoming.isEmpty()) {
                sections.add(new Section(op, section.title(), outgoing, incoming));
            }
        }
        return sections;
    }

    public record Section(OperationType operationType, String title, List<Row> outgoing, List<Row> incoming) {
    }

    public record Row(String label, Party sourceType, Party destinationType, BigDecimal percent, BigDecimal minFee,
                      BigDecimal fixedFee, BigDecimal maxFee, BigDecimal freeMonthlyAmount) {

        static Row of(CommissionRule r) {
            return new Row(r.getLabel(), r.getSourceType(), r.getDestinationType(), r.getPercent(), r.getMinFee(),
                    r.getFixedFee(), r.getMaxFee(), r.getFreeMonthlyAmount());
        }
    }
}
