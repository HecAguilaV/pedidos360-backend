package com.pedidos360.repository;

import com.pedidos360.model.ProcessedOrderStockEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedOrderStockEventRepository extends JpaRepository<ProcessedOrderStockEvent, Long> {
    boolean existsByOrderId(Long orderId);
}
