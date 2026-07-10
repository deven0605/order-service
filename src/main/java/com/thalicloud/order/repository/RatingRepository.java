package com.thalicloud.order.repository;

import com.thalicloud.order.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {

    @Query("""
            SELECT AVG(r.value), COUNT(r) FROM Rating r
            WHERE r.vendorId = :vendorId
              AND r.deletedAt IS NULL
            """)
    Object[] findAggregatesByVendorId(@Param("vendorId") UUID vendorId);

    @Query("""
            SELECT r.vendorId, AVG(r.value), COUNT(r) FROM Rating r
            WHERE r.vendorId IN :vendorIds
              AND r.deletedAt IS NULL
            GROUP BY r.vendorId
            """)
    List<Object[]> findAggregatesByVendorIds(@Param("vendorIds") List<UUID> vendorIds);
}
