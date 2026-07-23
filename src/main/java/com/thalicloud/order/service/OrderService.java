package com.thalicloud.order.service;

import com.thalicloud.order.dto.request.OrderFilterRequest;
import com.thalicloud.order.dto.request.PlaceOrderRequest;
import com.thalicloud.order.dto.request.UpdateOrderStatusRequest;
import com.thalicloud.order.dto.response.OrderResponse;
import com.thalicloud.order.dto.response.OrderStatusResponse;
import com.thalicloud.order.dto.response.PagedOrdersResponse;
import com.thalicloud.order.dto.response.PlaceOrderResponse;
import com.thalicloud.order.dto.response.UpdateOrderStatusResponse;
import com.thalicloud.order.enums.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    List<OrderResponse> getRecentOrders(UUID vendorId, int limit);

    PagedOrdersResponse getOrders(UUID vendorId, OrderFilterRequest filter);

    UpdateOrderStatusResponse updateStatus(UUID vendorId, String orderId, UpdateOrderStatusRequest request);

    PlaceOrderResponse placeOrder(UUID customerId, PlaceOrderRequest request);

    // Vendor accepts a PENDING order — advances it to KITCHEN_ACCEPTED and
    // (best-effort) notifies delivery-service to dispatch a partner.
    UpdateOrderStatusResponse acceptOrder(UUID vendorId, String orderId);

    // Vendor rejects a PENDING order with a reason — terminal, no further transitions.
    UpdateOrderStatusResponse rejectOrder(UUID vendorId, String orderId, String reason);

    // System-driven transition, called by delivery-service after a pickup/delivery
    // OTP match (see InternalOrderController) — not scoped to a vendor.
    void updateStatusInternal(String orderId, OrderStatus newStatus);

    // Customer-facing status poll for the mobile app's order tracking screen.
    OrderStatusResponse getStatusForCustomer(UUID customerId, String orderId);
}
