package com.pedidos360.repository;

import com.pedidos360.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<CartItem> findByUserIdAndProductCode(String userId, String productCode);

    Optional<CartItem> findByIdAndUserId(Long id, String userId);

    void deleteByUserId(String userId);
}
