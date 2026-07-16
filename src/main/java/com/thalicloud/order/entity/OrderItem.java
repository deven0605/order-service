package com.thalicloud.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vendor_order_items")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // meal-plan-service's meal-type catalog id (e.g. "standard-veg-thali")
    @Column(name = "meal_type_id", nullable = false, length = 50)
    private String mealTypeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // Catalog price at the time of order, in paise — never trusts the client's submitted price.
    @Column(name = "base_price_in_paise", nullable = false)
    private long basePriceInPaise;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "line_total_in_paise", nullable = false)
    private long lineTotalInPaise;

    @Builder.Default
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<OrderItemAddOn> addOns = new ArrayList<>();

    public void addAddOn(OrderItemAddOn addOn) {
        addOns.add(addOn);
    }
}
