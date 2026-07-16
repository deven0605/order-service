package com.thalicloud.order.repository;

import com.thalicloud.order.entity.Kitchen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KitchenRepository extends JpaRepository<Kitchen, UUID> {
}
