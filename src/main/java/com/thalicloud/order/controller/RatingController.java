package com.thalicloud.order.controller;

import com.thalicloud.order.dto.response.ApiResponse;
import com.thalicloud.order.dto.response.RatingAggregateResponse;
import com.thalicloud.order.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingRepository ratingRepository;

    /**
     * GET /api/orders/ratings/aggregate?vendorIds=id1,id2,...
     * Batch rating rollup consumed by vendor-service to enrich kitchen listing
     * cards (Home/Search). Public — this is a service-to-service call with no
     * customer/vendor token attached (see SecurityConfig.PUBLIC_ENDPOINTS).
     */
    @GetMapping("/aggregate")
    public ResponseEntity<ApiResponse<List<RatingAggregateResponse>>> getAggregates(
            @RequestParam List<UUID> vendorIds) {

        if (vendorIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.of(List.of()));
        }

        // AVG()/COUNT() come back as generic Number (H2/Hibernate don't guarantee
        // BigDecimal/Long here) — see DashboardServiceImpl's identical single-vendor
        // aggregate for the same defensive cast.
        List<Object[]> rows = ratingRepository.findAggregatesByVendorIds(vendorIds);
        List<RatingAggregateResponse> body = rows.stream()
                .map(row -> {
                    UUID vendorId = (UUID) row[0];
                    double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                    long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                    BigDecimal rounded = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
                    return new RatingAggregateResponse(vendorId, rounded, count);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.of(body));
    }
}
