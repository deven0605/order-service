package com.thalicloud.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeliveryAddressRequest(
        @NotBlank(message = "deliveryAddress.label is required")
        String label,

        @NotBlank(message = "deliveryAddress.fullAddress is required")
        String fullAddress,

        @NotNull(message = "deliveryAddress.latitude is required")
        Double latitude,

        @NotNull(message = "deliveryAddress.longitude is required")
        Double longitude
) {}
