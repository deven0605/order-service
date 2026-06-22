package com.thalicloud.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record DashboardSummaryResponse(
        OrdersToday ordersToday,
        RevenueToday revenueToday,
        @JsonInclude(JsonInclude.Include.ALWAYS) ActiveMealPlan activeMealPlan,
        AvgRating avgRating
) {

    public record OrdersToday(int count, int deltaFromYesterday) {}

    public record RevenueToday(long amountInPaise, int orderCount) {}

    public record ActiveMealPlan(String planId, String name, int daysRemaining) {}

    public record AvgRating(double value, int reviewCount) {}
}
