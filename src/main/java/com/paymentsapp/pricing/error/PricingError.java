package com.paymentsapp.pricing.error;

import com.paymentsapp.platform.error.ErrorCode;
import org.springframework.http.HttpStatus;

// Отказы pricing: фронтенд показывает текст под суммой вместо комиссии
public enum PricingError implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Пользователь не найден"),
    ACCOUNT_NOT_OWNED(HttpStatus.FORBIDDEN, "Счёт не принадлежит пользователю"),
    OPERATION_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT, "Такой перевод недоступен"),
    ABROAD_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT, "С этого счёта перевод за рубеж недоступен", "OPERATION_NOT_AVAILABLE"),
    PROVIDER_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT, "Поставщик не найден"),
    PROVIDER_REQUIRED(HttpStatus.BAD_REQUEST, "Не выбран поставщик"),
    RECIPIENT_CARD_REQUIRED(HttpStatus.BAD_REQUEST, "Не указан номер карты получателя"),
    DESTINATION_REQUIRED(HttpStatus.BAD_REQUEST, "Не выбран счёт зачисления"),
    FX_AMOUNT_ONE_OF(HttpStatus.BAD_REQUEST, "Укажите сумму списания или сумму к получению"),
    CURRENCY_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "Валюта %s не поддерживается"),
    AMOUNT_TOO_SMALL(HttpStatus.BAD_REQUEST, "Слишком маленькая сумма"),
    ACCOUNT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Сервис счетов недоступен, попробуйте позже"),
    BILLER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Сервис поставщиков недоступен, попробуйте позже");

    private final HttpStatus status;
    private final String template;
    private final String code;

    PricingError(HttpStatus status, String template) {
        this(status, template, null);
    }

    PricingError(HttpStatus status, String template, String code) {
        this.status = status;
        this.template = template;
        this.code = code;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String template() {
        return template;
    }

    @Override
    public String code() {
        return code != null ? code : name();
    }
}
