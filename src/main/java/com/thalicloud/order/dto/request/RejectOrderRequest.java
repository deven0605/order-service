package com.thalicloud.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectOrderRequest(
        @NotBlank(message = "reason is required")
        @Size(max = 255, message = "reason must be at most 255 characters")
        String reason
) {}
