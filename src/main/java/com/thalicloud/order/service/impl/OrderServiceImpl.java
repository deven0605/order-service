package com.thalicloud.order.service.impl;

import com.thalicloud.order.dto.request.OrderFilterRequest;
import com.thalicloud.order.dto.request.PlaceOrderAddOnRequest;
import com.thalicloud.order.dto.request.PlaceOrderItemRequest;
import com.thalicloud.order.dto.request.PlaceOrderRequest;
import com.thalicloud.order.dto.request.UpdateOrderStatusRequest;
import com.thalicloud.order.dto.response.OrderResponse;
import com.thalicloud.order.dto.response.PagedOrdersResponse;
import com.thalicloud.order.dto.response.PlaceOrderResponse;
import com.thalicloud.order.dto.response.UpdateOrderStatusResponse;
import com.thalicloud.order.entity.AddOn;
import com.thalicloud.order.entity.Customer;
import com.thalicloud.order.entity.Kitchen;
import com.thalicloud.order.entity.MealType;
import com.thalicloud.order.entity.Order;
import com.thalicloud.order.entity.OrderItem;
import com.thalicloud.order.entity.OrderItemAddOn;
import com.thalicloud.order.enums.OrderStatus;
import com.thalicloud.order.enums.PaymentMethod;
import com.thalicloud.order.exception.InvalidOrderRequestException;
import com.thalicloud.order.exception.InvalidStatusTransitionException;
import com.thalicloud.order.exception.ResourceNotFoundException;
import com.thalicloud.order.repository.AddOnRepository;
import com.thalicloud.order.repository.CustomerRepository;
import com.thalicloud.order.repository.KitchenRepository;
import com.thalicloud.order.repository.MealTypeRepository;
import com.thalicloud.order.repository.OrderRepository;
import com.thalicloud.order.service.OrderService;
import com.thalicloud.order.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import java.util.stream.Collectors;

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

    // Matches the mobile app's utils/cartTotals.ts (DELIVERY_CHARGE / GST_RATE) —
    // duplicated here because order-service is the source of truth for pricing at
    // order-placement time and must not trust anything the client computed.
    private static final long DELIVERY_CHARGE_IN_PAISE = 3000; // ₹30
    private static final double TAX_RATE = 0.05;               // 5% GST

    private final OrderRepository   orderRepository;
    private final CustomerRepository customerRepository;
    private final KitchenRepository  kitchenRepository;
    private final MealTypeRepository mealTypeRepository;
    private final AddOnRepository    addOnRepository;

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

    @Override
    @Transactional
    public PlaceOrderResponse placeOrder(UUID customerId, PlaceOrderRequest request) {
        if (request.paymentMethod() != PaymentMethod.COD) {
            throw new InvalidOrderRequestException(
                    "PAYMENT_METHOD_NOT_SUPPORTED", "Only Cash on Delivery is available right now.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));

        Kitchen kitchen = kitchenRepository.findById(request.kitchenId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "KITCHEN_NOT_FOUND", "Kitchen not found: " + request.kitchenId()));
        if (!kitchen.isActive()) {
            throw new InvalidOrderRequestException(
                    "KITCHEN_UNAVAILABLE", "This kitchen isn't accepting orders right now.");
        }

        // Re-price every item/add-on from the catalog — a client-submitted price is never trusted.
        List<PricedItem> pricedItems = request.items().stream().map(this::priceItem).toList();

        long subtotalInPaise = pricedItems.stream().mapToLong(PricedItem::lineTotalInPaise).sum();
        long deliveryChargeInPaise = subtotalInPaise > 0 ? DELIVERY_CHARGE_IN_PAISE : 0;
        long subtotalRupees = subtotalInPaise / 100;
        long taxInPaise = (long) Math.ceil(subtotalRupees * TAX_RATE) * 100;
        long discountInPaise = resolveDiscountInPaise(request, subtotalInPaise);
        long grandTotalInPaise = Math.max(0, subtotalInPaise + deliveryChargeInPaise + taxInPaise - discountInPaise);

        String customerName = (customer.getName() != null && !customer.getName().isBlank())
                ? customer.getName() : customer.getPhone();
        String couponCode = (request.couponCode() != null && !request.couponCode().isBlank())
                ? request.couponCode().trim().toUpperCase() : null;

        Order order = Order.place(
                generateOrderDisplayId(),
                kitchen.getVendorId(),
                kitchen.getId(),
                customer.getId(),
                customerName,
                summarizeItems(pricedItems),
                request.paymentMethod(),
                couponCode,
                request.deliveryAddress().label(),
                request.deliveryAddress().fullAddress(),
                request.deliveryAddress().latitude(),
                request.deliveryAddress().longitude(),
                subtotalInPaise,
                deliveryChargeInPaise,
                taxInPaise,
                discountInPaise,
                grandTotalInPaise
        );

        for (PricedItem pricedItem : pricedItems) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .mealTypeId(pricedItem.mealType().getId())
                    .name(pricedItem.mealType().getName())
                    .basePriceInPaise(pricedItem.mealType().getPrice() * 100L)
                    .quantity(pricedItem.quantity())
                    .lineTotalInPaise(pricedItem.lineTotalInPaise())
                    .build();

            for (PricedAddOn pricedAddOn : pricedItem.addOns()) {
                orderItem.addAddOn(OrderItemAddOn.builder()
                        .orderItem(orderItem)
                        .addOnId(pricedAddOn.addOn().getId())
                        .name(pricedAddOn.addOn().getName())
                        .priceInPaise(pricedAddOn.addOn().getPrice() * 100L)
                        .quantity(pricedAddOn.quantity())
                        .build());
            }

            order.addItem(orderItem);
        }

        orderRepository.save(order);

        return new PlaceOrderResponse(
                order.getOrderDisplayId(),
                order.getStatus().name(),
                order.getPaymentMethod().name(),
                grandTotalInPaise / 100,
                order.getCreatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
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

    private PricedItem priceItem(PlaceOrderItemRequest itemRequest) {
        MealType mealType = mealTypeRepository.findById(itemRequest.mealTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MEAL_TYPE_NOT_FOUND", "Meal type not found: " + itemRequest.mealTypeId()));

        List<PricedAddOn> pricedAddOns = itemRequest.addOns().stream().map(this::priceAddOn).toList();

        long addOnsTotalInPaise = pricedAddOns.stream()
                .mapToLong(a -> a.addOn().getPrice() * 100L * a.quantity())
                .sum();
        long basePriceInPaise = mealType.getPrice() * 100L;
        long lineTotalInPaise = (basePriceInPaise + addOnsTotalInPaise) * itemRequest.quantity();

        return new PricedItem(mealType, itemRequest.quantity(), pricedAddOns, lineTotalInPaise);
    }

    private PricedAddOn priceAddOn(PlaceOrderAddOnRequest addOnRequest) {
        AddOn addOn = addOnRepository.findById(addOnRequest.addOnId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ADD_ON_NOT_FOUND", "Add-on not found: " + addOnRequest.addOnId()));
        if (!addOn.isActive()) {
            throw new InvalidOrderRequestException(
                    "ADD_ON_UNAVAILABLE", "Add-on no longer available: " + addOn.getName());
        }
        return new PricedAddOn(addOn, addOnRequest.quantity());
    }

    private long resolveDiscountInPaise(PlaceOrderRequest request, long subtotalInPaise) {
        boolean hasCoupon = request.couponCode() != null && !request.couponCode().isBlank();
        if (!hasCoupon || request.discount() == null) {
            return 0;
        }
        long requestedDiscountInPaise = request.discount() * 100L;
        return Math.max(0, Math.min(requestedDiscountInPaise, subtotalInPaise));
    }

    private String summarizeItems(List<PricedItem> items) {
        if (items.size() == 1) {
            return items.get(0).mealType().getName() + " x" + items.get(0).quantity();
        }
        return items.stream()
                .map(i -> i.mealType().getName().replaceAll("(?i)\\s*Thali$", ""))
                .collect(Collectors.joining(" + "));
    }

    private String generateOrderDisplayId() {
        long sequence = orderRepository.count() + 1;
        String candidate = "ORD-%03d".formatted(sequence);
        while (orderRepository.existsByOrderDisplayId(candidate)) {
            sequence++;
            candidate = "ORD-%03d".formatted(sequence);
        }
        return candidate;
    }

    private record PricedAddOn(AddOn addOn, int quantity) {}

    private record PricedItem(MealType mealType, int quantity, List<PricedAddOn> addOns, long lineTotalInPaise) {}

}
