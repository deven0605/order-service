package com.thalicloud.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Read-only view of meal-plan-service's meal_types table — the canonical
 * catalog price is looked up from here at order-placement time so a client
 * can never submit its own item price. Meal-plan-service owns the full schema.
 */
@Entity
@Table(name = "meal_types")
@Getter
@NoArgsConstructor
public class MealType {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private int price;
}
