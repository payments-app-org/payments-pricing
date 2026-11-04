package com.paymentsapp.pricing.commission;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;

// Тариф: комиссия для «вид операции × откуда × куда × подписка».
// Комиссия = min(max(сумма × percent / 100, minFee) + fixedFee, maxFee).
// freeMonthlyAmount — сколько в месяц можно без комиссии: комиссию берём только с превышения
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
    // null — без верхней границы
    private BigDecimal maxFee;
    // null — бесплатного лимита нет
    private BigDecimal freeMonthlyAmount;
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

    public CommissionRule(Integer id, OperationType operationType, Party sourceType, Party destinationType,
                          Subscription subscription, BigDecimal percent, BigDecimal minFee, BigDecimal fixedFee,
                          BigDecimal maxFee, BigDecimal freeMonthlyAmount) {
        this(id, operationType, sourceType, destinationType, subscription, percent, minFee, fixedFee);
        this.maxFee = maxFee;
        this.freeMonthlyAmount = freeMonthlyAmount;
    }

    public BigDecimal commissionFor(BigDecimal amount) {
        return commissionFor(amount, BigDecimal.ZERO);
    }

    // usedThisMonth — сколько клиент уже перевёл этим видом операций с начала месяца
    public BigDecimal commissionFor(BigDecimal amount, BigDecimal usedThisMonth) {
        BigDecimal charged = amount.subtract(freeLeft(usedThisMonth)).max(BigDecimal.ZERO);
        if (charged.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal byPercent = charged.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal fee = byPercent.max(minFee).add(fixedFee);
        if (maxFee != null) {
            fee = fee.min(maxFee);
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    // Сколько ещё можно перевести без комиссии в этом месяце; без бесплатного лимита — 0
    public BigDecimal freeLeft(BigDecimal usedThisMonth) {
        if (freeMonthlyAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal used = usedThisMonth == null ? BigDecimal.ZERO : usedThisMonth;
        return freeMonthlyAmount.subtract(used).max(BigDecimal.ZERO);
    }

    public Integer getId() { return id; }
    public OperationType getOperationType() { return operationType; }
    public Party getSourceType() { return sourceType; }
    public Party getDestinationType() { return destinationType; }
    public Subscription getSubscription() { return subscription; }
    public BigDecimal getPercent() { return percent; }
    public BigDecimal getMinFee() { return minFee; }
    public BigDecimal getFixedFee() { return fixedFee; }
    public BigDecimal getMaxFee() { return maxFee; }
    public BigDecimal getFreeMonthlyAmount() { return freeMonthlyAmount; }
    public String getLabel() { return label; }
    public int getSortOrder() { return sortOrder; }
}
