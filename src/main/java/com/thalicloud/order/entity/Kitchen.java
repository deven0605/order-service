package com.thalicloud.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Read-only view of vendor-service's kitchens table — only what's needed to
 * resolve the customer app's kitchenId to the vendorId that vendor_orders is
 * keyed on. Vendor-service owns the full schema.
 */
@Entity
@Table(name = "kitchens")
@Getter
@NoArgsConstructor
public class Kitchen {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "kitchen_name", nullable = false, length = 150)
    private String kitchenName;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
