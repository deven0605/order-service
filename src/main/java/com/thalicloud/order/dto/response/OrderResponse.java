package com.thalicloud.order.dto.response;

public record OrderResponse(
        String orderId,
        String customerName,
        String mealType,
        long amountInPaise,
        String status,
        String createdAt,
        String rejectionReason
) {}
