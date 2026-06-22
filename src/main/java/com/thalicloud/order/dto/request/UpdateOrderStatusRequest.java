package com.thalicloud.order.dto.request;

import com.thalicloud.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "status is required")
        OrderStatus status
) {}
