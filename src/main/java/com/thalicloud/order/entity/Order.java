package com.thalicloud.order.entity;

import com.thalicloud.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * "ORDER" is an SQL/JPQL reserved word — the JPQL entity name is "VendorOrder"
 * and the table is "vendor_orders" to avoid parser conflicts.
 */
@Entity(name = "VendorOrder")
@Table(name = "vendor_orders", indexes = {
        @Index(name = "idx_order_vendor_id",      columnList = "vendor_id"),
        @Index(name = "idx_order_display_id",     columnList = "order_display_id"),
        @Index(name = "idx_order_vendor_created", columnList = "vendor_id,created_at")
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
     * Set by the order-creation flow; this service only reads and searches by it.
     */
    @Column(name = "order_display_id", nullable = false, unique = true, length = 20)
    private String orderDisplayId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "meal_type", nullable = false, length = 100)
    private String mealType;

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

    // ── Domain methods ────────────────────────────────────────────────────────

    public void advanceStatus(OrderStatus newStatus) {
        this.status    = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
