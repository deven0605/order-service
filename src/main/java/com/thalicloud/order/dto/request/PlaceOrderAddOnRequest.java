package com.thalicloud.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Only the add-on's catalog id and the requested quantity are trusted from the
 * client — its name and price are always re-resolved server-side from
 * meal-plan-service's add_ons table.
 */
public record PlaceOrderAddOnRequest(
        @NotBlank(message = "addOnId is required")
        String addOnId,

        @Min(value = 1, message = "add-on quantity must be at least 1")
        int quantity
) {}
