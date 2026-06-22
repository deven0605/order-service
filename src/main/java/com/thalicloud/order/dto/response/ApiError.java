package com.thalicloud.order.dto.response;

public record ApiError(ApiErrorDetail error) {

    public record ApiErrorDetail(String code, String message) {}

    public static ApiError of(String code, String message) {
        return new ApiError(new ApiErrorDetail(code, message));
    }
}
