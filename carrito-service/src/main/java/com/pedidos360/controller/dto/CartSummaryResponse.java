package com.pedidos360.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartSummaryResponse(
    List<CartItemResponse> items,
    int totalItems,
    BigDecimal totalAmount
) {
    public static CartSummaryResponse from(List<CartItemResponse> items) {
        int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal totalAmount = items.stream()
            .map(CartItemResponse::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartSummaryResponse(items, totalItems, totalAmount);
    }
}
