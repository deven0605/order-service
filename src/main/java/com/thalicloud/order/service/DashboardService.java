package com.thalicloud.order.service;

import com.thalicloud.order.dto.response.DashboardSummaryResponse;

import java.util.UUID;

public interface DashboardService {

    DashboardSummaryResponse getSummary(UUID vendorId);
}
