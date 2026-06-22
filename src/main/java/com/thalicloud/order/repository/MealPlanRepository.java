package com.thalicloud.order.repository;

import com.thalicloud.order.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, UUID> {

    Optional<MealPlan> findFirstByVendorIdAndActiveTrueAndDeletedAtIsNull(UUID vendorId);
}
