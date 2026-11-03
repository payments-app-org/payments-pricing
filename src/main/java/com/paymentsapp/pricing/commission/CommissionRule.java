package com.paymentsapp.pricing.commission;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

// Тариф: процент комиссии для пары «вид операции × подписка»
@Entity
@Table(name = "commission_rules")
public class CommissionRule {

    @Id
    private Integer id;

    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    private Subscription subscription;

    private BigDecimal percent;

    protected CommissionRule() {
    }

    public CommissionRule(Integer id, OperationType operationType, Subscription subscription, BigDecimal percent) {
        this.id = id;
        this.operationType = operationType;
        this.subscription = subscription;
        this.percent = percent;
    }

    public Integer getId() { return id; }
    public OperationType getOperationType() { return operationType; }
    public Subscription getSubscription() { return subscription; }
    public BigDecimal getPercent() { return percent; }
}
