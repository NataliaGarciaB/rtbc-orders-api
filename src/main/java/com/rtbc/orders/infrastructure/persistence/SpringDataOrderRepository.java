package com.rtbc.orders.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOrderRepository extends JpaRepository<OrderEntity, UUID> {
}
