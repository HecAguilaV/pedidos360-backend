package com.pedidos360.controller.dto;

import com.pedidos360.domain.CartItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartItemResponse(
    Long id,
    String userId,
    String productCode,
    String productName,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal subtotal,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CartItemResponse from(CartItem item) {
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
            item.getId(),
            item.getUserId(),
            item.getProductCode(),
            item.getProductName(),
            item.getUnitPrice(),
            item.getQuantity(),
            subtotal,
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }
}
