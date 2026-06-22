package com.thalicloud.order.dto.response;

public record UpdateOrderStatusResponse(
        String orderId,
        String status,
        String updatedAt
) {}
