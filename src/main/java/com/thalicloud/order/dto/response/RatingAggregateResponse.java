package com.thalicloud.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RatingAggregateResponse(
        UUID vendorId,
        BigDecimal avgRating,
        long ratingCount
) {}
