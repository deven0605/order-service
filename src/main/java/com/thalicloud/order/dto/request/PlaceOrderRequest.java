package com.thalicloud.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.thalicloud.order.enums.PaymentMethod;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(
        @NotNull(message = "kitchenId is required")
        UUID kitchenId,

        @NotEmpty(message = "items must not be empty")
        @Valid
        List<PlaceOrderItemRequest> items,

        @NotNull(message = "deliveryAddress is required")
        @Valid
        DeliveryAddressRequest deliveryAddress,

        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod,

        String couponCode,

        // coupon-service doesn't exist yet (see the mobile app's couponApi.ts mock) — there is
        // nowhere to independently validate a coupon server-side. Rather than silently ignoring
        // it and charging the customer more than the app showed them, we accept the client's
        // computed discount but only when couponCode is present, and clamp it to the recomputed
        // subtotal in OrderServiceImpl so the worst case is a free order, never a negative total.
        @Min(value = 0, message = "discount cannot be negative")
        Integer discount
) {}
