package com.thalicloud.order.dto.request;

import com.thalicloud.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

// Body for PATCH /api/orders/internal/{orderId}/status — see InternalOrderController.
public record InternalUpdateOrderStatusRequest(
        @NotNull(message = "status is required")
        OrderStatus status
) {}
