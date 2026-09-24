package com.pedidos360.controller;

import com.pedidos360.controller.dto.AddCartItemRequest;
import com.pedidos360.controller.dto.CartItemResponse;
import com.pedidos360.controller.dto.CartSummaryResponse;
import com.pedidos360.controller.dto.UpdateCartItemRequest;
import com.pedidos360.domain.CartItem;
import com.pedidos360.repository.CartItemRepository;
import com.pedidos360.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/carrito")
public class CartController {

    private final CartItemRepository repository;
    private final CurrentUserService currentUserService;

    public CartController(CartItemRepository repository, CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<CartSummaryResponse> getCart(Authentication authentication, HttpServletRequest request) {
        String userId = currentUserService.resolveUserId(authentication, request);
        List<CartItemResponse> items = repository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(CartItemResponse::from)
            .toList();

        return ResponseEntity.ok(CartSummaryResponse.from(items));
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addItem(
        @Valid @RequestBody AddCartItemRequest body,
        Authentication authentication,
        HttpServletRequest request
    ) {
        String userId = currentUserService.resolveUserId(authentication, request);

        CartItem item = repository.findByUserIdAndProductCode(userId, body.productCode())
            .map(existing -> {
                existing.setQuantity(existing.getQuantity() + body.quantity());
                existing.setUnitPrice(body.unitPrice());
                existing.setProductName(body.productName());
                return repository.save(existing);
            })
            .orElseGet(() -> {
                CartItem newItem = new CartItem(
                    userId,
                    body.productCode(),
                    body.productName(),
                    body.unitPrice(),
                    body.quantity()
                );
                return repository.save(newItem);
            });

        return ResponseEntity.status(HttpStatus.CREATED).body(CartItemResponse.from(item));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<CartItemResponse> updateQuantity(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCartItemRequest body,
        Authentication authentication,
        HttpServletRequest request
    ) {
        String userId = currentUserService.resolveUserId(authentication, request);

        CartItem item = repository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item no encontrado en el carrito"));

        item.setQuantity(body.quantity());
        CartItem saved = repository.save(item);

        return ResponseEntity.ok(CartItemResponse.from(saved));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(
        @PathVariable Long id,
        Authentication authentication,
        HttpServletRequest request
    ) {
        String userId = currentUserService.resolveUserId(authentication, request);

        CartItem item = repository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item no encontrado en el carrito"));

        repository.delete(item);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> clearCart(Authentication authentication, HttpServletRequest request) {
        String userId = currentUserService.resolveUserId(authentication, request);
        repository.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
