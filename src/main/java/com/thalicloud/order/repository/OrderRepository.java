package com.thalicloud.order.repository;

import com.thalicloud.order.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderDisplayIdAndVendorIdAndDeletedAtIsNull(String orderDisplayId, UUID vendorId);

    Optional<Order> findByOrderDisplayIdAndCustomerIdAndDeletedAtIsNull(String orderDisplayId, UUID customerId);

    // Unscoped lookup — needed by the internal status-callback endpoint, which
    // delivery-service calls knowing only the orderId (no vendorId in hand).
    Optional<Order> findByOrderDisplayIdAndDeletedAtIsNull(String orderDisplayId);

    boolean existsByOrderDisplayId(String orderDisplayId);

    @Query("""
            SELECT o FROM VendorOrder o
            WHERE o.vendorId = :vendorId
              AND o.deletedAt IS NULL
            ORDER BY o.createdAt DESC
            """)
    List<Order> findRecentByVendorId(@Param("vendorId") UUID vendorId, Pageable pageable);

    @Query("""
            SELECT COUNT(o) FROM VendorOrder o
            WHERE o.vendorId    = :vendorId
              AND o.createdAt  >= :startOfDay
              AND o.createdAt   < :endOfDay
              AND o.deletedAt  IS NULL
            """)
    long countByVendorIdAndDateRange(
            @Param("vendorId")   UUID vendorId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay")   LocalDateTime endOfDay
    );

    @Query("""
            SELECT SUM(o.amountInPaise) FROM VendorOrder o
            WHERE o.vendorId    = :vendorId
              AND o.createdAt  >= :startOfDay
              AND o.createdAt   < :endOfDay
              AND o.deletedAt  IS NULL
            """)
    Long sumAmountByVendorIdAndDateRange(
            @Param("vendorId")   UUID vendorId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay")   LocalDateTime endOfDay
    );
}
