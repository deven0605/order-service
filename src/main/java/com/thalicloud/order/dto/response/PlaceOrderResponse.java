package com.thalicloud.order.dto.response;

public record PlaceOrderResponse(
        String id,
        String status,
        String paymentMethod,
        long grandTotal,
        String placedAt
) {}
