package com.thalicloud.order.service.impl;

import com.thalicloud.order.dto.response.DashboardSummaryResponse;
import com.thalicloud.order.repository.MealPlanRepository;
import com.thalicloud.order.repository.OrderRepository;
import com.thalicloud.order.repository.RatingRepository;
import com.thalicloud.order.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository    orderRepository;
    private final MealPlanRepository mealPlanRepository;
    private final RatingRepository   ratingRepository;

    @Override
    public DashboardSummaryResponse getSummary(UUID vendorId) {
        LocalDate today     = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        LocalDateTime startOfToday     = today.atStartOfDay();
        LocalDateTime startOfTomorrow  = today.plusDays(1).atStartOfDay();
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();

        // Orders today and delta
        long countToday     = orderRepository.countByVendorIdAndDateRange(vendorId, startOfToday, startOfTomorrow);
        long countYesterday = orderRepository.countByVendorIdAndDateRange(vendorId, startOfYesterday, startOfToday);
        int  delta          = (int) (countToday - countYesterday);

        // Revenue today
        Long rawRevenue    = orderRepository.sumAmountByVendorIdAndDateRange(vendorId, startOfToday, startOfTomorrow);
        long revenueToday  = rawRevenue != null ? rawRevenue : 0L;

        // Active meal plan
        DashboardSummaryResponse.ActiveMealPlan activeMealPlan = mealPlanRepository
                .findFirstByVendorIdAndActiveTrueAndDeletedAtIsNull(vendorId)
                .map(plan -> new DashboardSummaryResponse.ActiveMealPlan(
                        plan.getId().toString(),
                        plan.getName(),
                        (int) Math.max(0L, ChronoUnit.DAYS.between(today, plan.getEndDate()))
                ))
                .orElse(null);

        // Average rating
        Object[] aggregates   = ratingRepository.findAggregatesByVendorId(vendorId);
        double   avgValue     = aggregates[0] != null ? ((Number) aggregates[0]).doubleValue() : 0.0;
        int      reviewCount  = aggregates[1] != null ? ((Number) aggregates[1]).intValue()    : 0;
        double   roundedRating = Math.round(avgValue * 10.0) / 10.0;

        return new DashboardSummaryResponse(
                new DashboardSummaryResponse.OrdersToday((int) countToday, delta),
                new DashboardSummaryResponse.RevenueToday(revenueToday, (int) countToday),
                activeMealPlan,
                new DashboardSummaryResponse.AvgRating(roundedRating, reviewCount)
        );
    }
}
