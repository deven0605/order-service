package com.thalicloud.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Only the meal type's catalog id and the requested quantity/add-ons are
 * trusted from the client — its name and price are always re-resolved
 * server-side from meal-plan-service's meal_types table.
 */
public record PlaceOrderItemRequest(
        @NotBlank(message = "mealTypeId is required")
        String mealTypeId,

        @Min(value = 1, message = "quantity must be at least 1")
        int quantity,

        @NotNull(message = "addOns is required (use an empty list if none)")
        @Valid
        List<PlaceOrderAddOnRequest> addOns
) {}
