package com.thalicloud.order.specification;

import com.thalicloud.order.entity.Order;
import com.thalicloud.order.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class OrderSpecification {

    private OrderSpecification() {}

    public static Specification<Order> belongsToVendor(UUID vendorId) {
        return (root, query, cb) -> cb.equal(root.get("vendorId"), vendorId);
    }

    public static Specification<Order> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Order> onDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.plusDays(1).atStartOfDay();
        return (root, query, cb) ->
                cb.and(
                    cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                    cb.lessThan(root.get("createdAt"), end)
                );
    }

    public static Specification<Order> withStatus(OrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Case-insensitive partial match on customerName or orderDisplayId.
     */
    public static Specification<Order> searchTerm(String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("customerName")), pattern),
                    cb.like(cb.lower(root.get("orderDisplayId")), pattern)
                );
    }
}
