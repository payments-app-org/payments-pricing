package com.paymentsapp.pricing.commission;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;

// Тариф: комиссия для «вид операции × откуда × куда × подписка».
// Комиссия = max(сумма × percent / 100, minFee) + fixedFee
@Entity
@Table(name = "commission_rules")
public class CommissionRule {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Id
    private Integer id;

    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    private Party sourceType;

    @Enumerated(EnumType.STRING)
    private Party destinationType;

    @Enumerated(EnumType.STRING)
    private Subscription subscription;

    private BigDecimal percent;
    private BigDecimal minFee;
    private BigDecimal fixedFee;
    private String label;
    private int sortOrder;

    protected CommissionRule() {
    }

    public CommissionRule(Integer id, OperationType operationType, Party sourceType, Party destinationType,
                          Subscription subscription, BigDecimal percent, BigDecimal minFee, BigDecimal fixedFee) {
        this.id = id;
        this.operationType = operationType;
        this.sourceType = sourceType;
        this.destinationType = destinationType;
        this.subscription = subscription;
        this.percent = percent;
        this.minFee = minFee;
        this.fixedFee = fixedFee;
        this.label = "";
    }

    public BigDecimal commissionFor(BigDecimal amount) {
        BigDecimal byPercent = amount.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return byPercent.max(minFee).add(fixedFee).setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getId() { return id; }
    public OperationType getOperationType() { return operationType; }
    public Party getSourceType() { return sourceType; }
    public Party getDestinationType() { return destinationType; }
    public Subscription getSubscription() { return subscription; }
    public BigDecimal getPercent() { return percent; }
    public BigDecimal getMinFee() { return minFee; }
    public BigDecimal getFixedFee() { return fixedFee; }
    public String getLabel() { return label; }
    public int getSortOrder() { return sortOrder; }
}
