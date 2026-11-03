package com.paymentsapp.pricing.web;

import com.paymentsapp.pricing.account.AccountNotOwnedException;
import com.paymentsapp.pricing.account.UserNotFoundException;
import com.paymentsapp.pricing.commission.OperationNotAvailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail userNotFound(UserNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AccountNotOwnedException.class)
    ProblemDetail accountNotOwned(AccountNotOwnedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(OperationNotAvailableException.class)
    ProblemDetail notAvailable(OperationNotAvailableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // Сервис account недоступен или ответил ошибкой
    @ExceptionHandler(RestClientException.class)
    ProblemDetail accountUnavailable(RestClientException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Сервис account недоступен");
    }
}
