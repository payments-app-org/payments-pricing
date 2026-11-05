package com.paymentsapp.pricing.quote;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// secret — подпись котировок (общий с payment); ttl — сколько живёт котировка: за это время нужно нажать «Перевести»
@ConfigurationProperties("quote")
public record QuoteProperties(String secret, Duration ttl) {
}
