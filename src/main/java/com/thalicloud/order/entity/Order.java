package com.thalicloud.order.entity;

import com.thalicloud.order.enums.OrderStatus;
import com.thalicloud.order.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "ORDER" is an SQL/JPQL reserved word — the JPQL entity name is "VendorOrder"
 * and the table is "vendor_orders" to avoid parser conflicts.
 */
@Entity(name = "VendorOrder")
@Table(name = "vendor_orders", indexes = {
        @Index(name = "idx_order_vendor_id",      columnList = "vendor_id"),
        @Index(name = "idx_order_display_id",     columnList = "order_display_id"),
        @Index(name = "idx_order_vendor_created", columnList = "vendor_id,created_at"),
        @Index(name = "idx_order_customer_id",    columnList = "customer_id")
})
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Human-readable identifier shown in the UI (e.g. "ORD-008").
     * Assigned by OrderServiceImpl.placeOrder — see generateOrderDisplayId().
     */
    @Column(name = "order_display_id", nullable = false, unique = true, length = 20)
    private String orderDisplayId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    // The customer app's kitchenId (vendor-service's kitchens.id) — kept alongside
    // vendorId (the FK vendor-dashboard queries filter on) so a future customer-order-
    // history endpoint can look orders up the same way the customer app already does.
    //
    // Nullable — see vendor-service's Kitchen entity for why (ddl-auto: update can't
    // retrofit NOT NULL columns onto vendor_orders, which already has seeded rows from
    // before order-placement existed). Order.place() always sets a real value below;
    // only pre-existing dashboard-seeded rows will ever read back null here.
    @Column(name = "kitchen_id")
    private UUID kitchenId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    // Denormalized summary shown in the vendor dashboard's single-line order rows
    // (e.g. "Standard Veg Thali x1"). Full fidelity lives in `items`.
    @Column(name = "meal_type", nullable = false, length = 100)
    private String mealType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "coupon_code", length = 30)
    private String couponCode;

    @Column(name = "delivery_label", length = 30)
    private String deliveryLabel;

    @Column(name = "delivery_full_address", length = 500)
    private String deliveryFullAddress;

    @Column(name = "delivery_latitude")
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude")
    private Double deliveryLongitude;

    @Column(name = "subtotal_in_paise")
    private Long subtotalInPaise;

    @Column(name = "delivery_charge_in_paise")
    private Long deliveryChargeInPaise;

    @Column(name = "tax_in_paise")
    private Long taxInPaise;

    @Column(name = "discount_in_paise")
    private Long discountInPaise;

    // Grand total — kept as the pre-existing column name/type so the vendor
    // dashboard's amount queries (DashboardServiceImpl) don't need to change.
    @Column(name = "amount_in_paise", nullable = false)
    private Long amountInPaise;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<OrderItem> items = new ArrayList<>();

    // ── Domain methods ────────────────────────────────────────────────────────

    /**
     * Creates a new order in PENDING status. Money fields are paise; callers
     * (OrderServiceImpl) must derive them from the meal-plan catalog, never
     * from client-submitted prices.
     */
    public static Order place(
            String orderDisplayId,
            UUID vendorId,
            UUID kitchenId,
            UUID customerId,
            String customerName,
            String mealTypeSummary,
            PaymentMethod paymentMethod,
            String couponCode,
            String deliveryLabel,
            String deliveryFullAddress,
            double deliveryLatitude,
            double deliveryLongitude,
            long subtotalInPaise,
            long deliveryChargeInPaise,
            long taxInPaise,
            long discountInPaise,
            long grandTotalInPaise
    ) {
        LocalDateTime now = LocalDateTime.now();

        Order order = new Order();
        order.orderDisplayId = orderDisplayId;
        order.vendorId = vendorId;
        order.kitchenId = kitchenId;
        order.customerId = customerId;
        order.customerName = customerName;
        order.mealType = mealTypeSummary;
        order.paymentMethod = paymentMethod;
        order.couponCode = couponCode;
        order.deliveryLabel = deliveryLabel;
        order.deliveryFullAddress = deliveryFullAddress;
        order.deliveryLatitude = deliveryLatitude;
        order.deliveryLongitude = deliveryLongitude;
        order.subtotalInPaise = subtotalInPaise;
        order.deliveryChargeInPaise = deliveryChargeInPaise;
        order.taxInPaise = taxInPaise;
        order.discountInPaise = discountInPaise;
        order.amountInPaise = grandTotalInPaise;
        order.status = OrderStatus.PENDING;
        order.createdAt = now;
        order.updatedAt = now;
        return order;
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void advanceStatus(OrderStatus newStatus) {
        this.status    = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
