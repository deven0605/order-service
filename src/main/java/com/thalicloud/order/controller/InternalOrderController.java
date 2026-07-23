package com.thalicloud.order.controller;

import com.thalicloud.order.dto.request.InternalUpdateOrderStatusRequest;
import com.thalicloud.order.dto.response.ApiError;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thalicloud.order.service.OrderService;

// The seam delivery-service calls after a pickup/delivery OTP match (see
// DeliveryAssignmentServiceImpl.verifyPickup/verifyDelivery there) to flip the
// order to DISPATCHED/DELIVERED automatically. Protected by a shared header
// key rather than a customer/vendor JWT — same pattern and same Phase-1
// placeholder spirit as delivery-service's InternalAssignmentController.
@RestController
@RequestMapping("/api/orders/internal")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @Value("${internal.dispatch-key}")
    private String dispatchKey;

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiError> updateStatus(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @PathVariable String orderId,
            @Valid @RequestBody InternalUpdateOrderStatusRequest request) {

        if (key == null || !key.equals(dispatchKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiError.of("UNAUTHORIZED", "Invalid or missing X-Internal-Key"));
        }

        orderService.updateStatusInternal(orderId, request.status());
        return ResponseEntity.ok(null);
    }
}
