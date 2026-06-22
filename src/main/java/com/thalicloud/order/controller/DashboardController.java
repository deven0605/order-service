package com.thalicloud.order.controller;

import com.thalicloud.order.dto.response.ApiResponse;
import com.thalicloud.order.dto.response.DashboardSummaryResponse;
import com.thalicloud.order.entity.Vendor;
import com.thalicloud.order.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard/summary
     * Returns the four stat cards for the Dashboard: orders today, revenue today,
     * active meal plan, and average rating.
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @AuthenticationPrincipal Vendor vendor) {

        DashboardSummaryResponse body = dashboardService.getSummary(vendor.getId());
        return ResponseEntity.ok(ApiResponse.of(body));
    }
}
