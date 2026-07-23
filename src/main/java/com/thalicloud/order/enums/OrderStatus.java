package com.thalicloud.order.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {

    PENDING("Pending"),
    KITCHEN_ACCEPTED("Kitchen Accepted"),
    PREPARING("Preparing"),
    READY("Ready"),
    DISPATCHED("DISPATCHED"),
    DELIVERED("Delivered"),
    REJECTED("Rejected");

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
     * Returns the single valid "forward" next status, or null if already terminal.
     * Enforces the one-direction lifecycle: Pending → Kitchen Accepted → Preparing →
     * Ready → Dispatched → Delivered. Rejected is a separate terminal branch off
     * Pending only (see canTransitionTo) — a vendor rejection isn't a "next" step.
     */
    public OrderStatus next() {
        return switch (this) {
            case PENDING          -> KITCHEN_ACCEPTED;
            case KITCHEN_ACCEPTED -> PREPARING;
            case PREPARING        -> READY;
            case READY            -> DISPATCHED;
            case DISPATCHED       -> DELIVERED;
            case DELIVERED, REJECTED -> null;
        };
    }

    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        if (this == PENDING && target == REJECTED) {
            return true;
        }
        return target == this.next();
    }
}
