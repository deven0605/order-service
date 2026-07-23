package com.thalicloud.order.dto.response;

public record OrderStatusResponse(
        String orderId,
        String status,
        String rejectionReason,
        String updatedAt
) {}
