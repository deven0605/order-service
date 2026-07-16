package com.thalicloud.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "vendor_order_item_addons")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderItemAddOn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    // meal-plan-service's add-on catalog id (e.g. "extra-roti")
    @Column(name = "add_on_id", nullable = false, length = 50)
    private String addOnId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // Catalog price at the time of order, in paise — never trusts the client's submitted price.
    @Column(name = "price_in_paise", nullable = false)
    private long priceInPaise;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
