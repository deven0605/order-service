package com.thalicloud.order.exception;

import lombok.Getter;

@Getter
public class InvalidOrderRequestException extends RuntimeException {

    private final String code;

    public InvalidOrderRequestException(String code, String message) {
        super(message);
        this.code = code;
    }
}
