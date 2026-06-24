package com.thalicloud.order.controller;

import com.thalicloud.order.dto.request.OrderFilterRequest;
import com.thalicloud.order.dto.request.UpdateOrderStatusRequest;
import com.thalicloud.order.dto.response.ApiResponse;
import com.thalicloud.order.dto.response.OrderResponse;
import com.thalicloud.order.dto.response.PagedOrdersResponse;
import com.thalicloud.order.dto.response.UpdateOrderStatusResponse;
import com.thalicloud.order.entity.Vendor;
import com.thalicloud.order.enums.OrderStatus;
import com.thalicloud.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    /**
     * GET /api/orders/recent?limit=5
     * Returns the most recent N orders for the dashboard widget.
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getRecentOrders(
            @AuthenticationPrincipal Vendor vendor,
            @RequestParam(defaultValue = "5") @Min(1) @Max(10) int limit) {

        List<OrderResponse> body = orderService.getRecentOrders(vendor.getId(), limit);
        return ResponseEntity.ok(ApiResponse.of(body));
    }

    /**
     * GET /api/orders
     * Returns a paginated, filterable, sortable list of orders for the Orders page.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedOrdersResponse>> getOrders(
            @AuthenticationPrincipal Vendor vendor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1")  @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        OrderFilterRequest filter = new OrderFilterRequest(
                date, status, search, page, pageSize, sortBy, sortOrder);

        PagedOrdersResponse body = orderService.getOrders(vendor.getId(), filter);
        return ResponseEntity.ok(ApiResponse.of(body));
    }

    /**
     * PATCH /api/orders/{orderId}/status
     * Advances the order through its lifecycle: Pending → Preparing → Ready → Dispatched → Delivered.
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<UpdateOrderStatusResponse>> updateStatus(
            @AuthenticationPrincipal Vendor vendor,
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        UpdateOrderStatusResponse body = orderService.updateStatus(vendor.getId(), orderId, request);
        return ResponseEntity.ok(ApiResponse.of(body));
    }
}
