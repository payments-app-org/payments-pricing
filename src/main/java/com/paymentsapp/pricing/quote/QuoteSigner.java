package com.paymentsapp.pricing.quote;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

// Токен котировки: base64url(json) + "." + base64url(HMAC-SHA256). Секрет общий с сервисом payment.
@Component
public class QuoteSigner {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    private final JsonMapper json;
    private final byte[] secret;

    public QuoteSigner(JsonMapper json, QuoteProperties properties) {
        this.json = json;
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
    }

    public String sign(Quote quote) {
        String body = B64.encodeToString(json.writeValueAsBytes(quote));
        return body + "." + B64.encodeToString(hmac(body));
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
