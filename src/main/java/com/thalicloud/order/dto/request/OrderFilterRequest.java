package com.thalicloud.order.dto.request;

import com.thalicloud.order.enums.OrderStatus;

import java.time.LocalDate;

public record OrderFilterRequest(
        LocalDate date,
        OrderStatus status,
        String search,
        int page,
        int pageSize,
        String sortBy,
        String sortOrder
) {}
