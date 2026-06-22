package com.thalicloud.order.service.impl;

import com.thalicloud.order.dto.request.OrderFilterRequest;
import com.thalicloud.order.dto.request.UpdateOrderStatusRequest;
import com.thalicloud.order.dto.response.OrderResponse;
import com.thalicloud.order.dto.response.PagedOrdersResponse;
import com.thalicloud.order.dto.response.UpdateOrderStatusResponse;
import com.thalicloud.order.entity.Order;
import com.thalicloud.order.enums.OrderStatus;
import com.thalicloud.order.exception.InvalidStatusTransitionException;
import com.thalicloud.order.exception.ResourceNotFoundException;
import com.thalicloud.order.repository.OrderRepository;
import com.thalicloud.order.service.OrderService;
import com.thalicloud.order.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final Map<String, String> SORT_FIELD_MAP = Map.of(
            "createdAt",    "createdAt",
            "amount",       "amountInPaise",
            "status",       "status",
            "customerName", "customerName"
    );

    private final OrderRepository orderRepository;

    @Override
    public List<OrderResponse> getRecentOrders(UUID vendorId, int limit) {
        return orderRepository.findRecentByVendorId(vendorId, Pageable.ofSize(limit))
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Override
    public PagedOrdersResponse getOrders(UUID vendorId, OrderFilterRequest filter) {
        Specification<Order> spec = Specification
                .where(OrderSpecification.belongsToVendor(vendorId))
                .and(OrderSpecification.notDeleted())
                .and(OrderSpecification.onDate(filter.date() != null ? filter.date() : LocalDate.now()));

        if (filter.status() != null) {
            spec = spec.and(OrderSpecification.withStatus(filter.status()));
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            spec = spec.and(OrderSpecification.searchTerm(filter.search().trim()));
        }

        String entityField = SORT_FIELD_MAP.getOrDefault(filter.sortBy(), "createdAt");
        Sort sort = "asc".equalsIgnoreCase(filter.sortOrder())
                ? Sort.by(entityField).ascending()
                : Sort.by(entityField).descending();

        PageRequest pageRequest = PageRequest.of(filter.page() - 1, filter.pageSize(), sort);
        Page<Order> page = orderRepository.findAll(spec, pageRequest);

        List<OrderResponse> orders = page.getContent().stream()
                .map(this::toOrderResponse)
                .toList();

        int totalPages = (int) Math.ceil((double) page.getTotalElements() / filter.pageSize());

        return new PagedOrdersResponse(
                orders,
                new PagedOrdersResponse.PaginationInfo(
                        filter.page(),
                        filter.pageSize(),
                        page.getTotalElements(),
                        Math.max(totalPages, 1)
                )
        );
    }

    @Override
    @Transactional
    public UpdateOrderStatusResponse updateStatus(UUID vendorId, String orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository
                .findByOrderDisplayIdAndVendorIdAndDeletedAtIsNull(orderId, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ORDER_NOT_FOUND", "Order not found: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus     = request.status();

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition order " + orderId + " from " + currentStatus.getDisplayName()
                    + " to " + newStatus.getDisplayName());
        }

        order.advanceStatus(newStatus);
        orderRepository.save(order);

        return new UpdateOrderStatusResponse(
                order.getOrderDisplayId(),
                newStatus.getDisplayName(),
                order.getUpdatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OrderResponse toOrderResponse(Order o) {
        return new OrderResponse(
                o.getOrderDisplayId(),
                o.getCustomerName(),
                o.getMealType(),
                o.getAmountInPaise(),
                o.getStatus().getDisplayName(),
                o.getCreatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
    }

}
