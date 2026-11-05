package com.paymentsapp.pricing.fx;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

// Курс валюты: sell_rate считает сама база из курса ЦБ и наценки
// Справочник меняется только миграциями (поэтому @Immutable: Hibernate не следит за изменениями)
@Entity
@Immutable
@Table(name = "exchange_rates")
public class ExchangeRate {

    @Id
    private String currency;

    private String nameRu;
    private String symbol;
    private BigDecimal cbrRate;
    private BigDecimal markupPercent;

    @Column(insertable = false, updatable = false)
    private BigDecimal sellRate;

    protected ExchangeRate() {
    }

    public ExchangeRate(String currency, String symbol, BigDecimal sellRate) {
        this.currency = currency;
        this.symbol = symbol;
        this.sellRate = sellRate;
    }

    public String getCurrency() { return currency; }
    public String getNameRu() { return nameRu; }
    public String getSymbol() { return symbol; }
    public BigDecimal getSellRate() { return sellRate; }
}
