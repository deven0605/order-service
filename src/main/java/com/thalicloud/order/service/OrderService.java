package com.thalicloud.order.service;

import com.thalicloud.order.dto.request.OrderFilterRequest;
import com.thalicloud.order.dto.request.PlaceOrderRequest;
import com.thalicloud.order.dto.request.UpdateOrderStatusRequest;
import com.thalicloud.order.dto.response.OrderResponse;
import com.thalicloud.order.dto.response.PagedOrdersResponse;
import com.thalicloud.order.dto.response.PlaceOrderResponse;
import com.thalicloud.order.dto.response.UpdateOrderStatusResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    List<OrderResponse> getRecentOrders(UUID vendorId, int limit);

    PagedOrdersResponse getOrders(UUID vendorId, OrderFilterRequest filter);

    UpdateOrderStatusResponse updateStatus(UUID vendorId, String orderId, UpdateOrderStatusRequest request);

    PlaceOrderResponse placeOrder(UUID customerId, PlaceOrderRequest request);
}
