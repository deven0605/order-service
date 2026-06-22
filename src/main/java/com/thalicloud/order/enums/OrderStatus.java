package com.thalicloud.order.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {

    PENDING("Pending"),
    PREPARING("Preparing"),
    READY("Ready"),
    DELIVERED("Delivered");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static OrderStatus fromString(String value) {
        for (OrderStatus s : values()) {
            if (s.displayName.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid order status: " + value);
    }

    /**
     * Returns the single valid next status, or null if already terminal.
     * Enforces the one-direction lifecycle: Pending → Preparing → Ready → Delivered.
     */
    public OrderStatus next() {
        return switch (this) {
            case PENDING   -> PREPARING;
            case PREPARING -> READY;
            case READY     -> DELIVERED;
            case DELIVERED -> null;
        };
    }

    public boolean canTransitionTo(OrderStatus target) {
        return target != null && target == this.next();
    }
}
