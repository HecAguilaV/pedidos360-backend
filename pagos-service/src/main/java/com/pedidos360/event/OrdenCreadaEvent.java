package com.pedidos360.event;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenCreadaEvent(
        Long orderId,
        String cliente,
        String clienteEmail,
        BigDecimal total,
        String estado,
        List<ItemOrdenEvent> items,
        LocalDateTime fecha
) implements Serializable {

    public OrdenCreadaEvent(
            Long orderId,
            String cliente,
            BigDecimal total,
            String estado,
            LocalDateTime fecha,
            List<ItemOrdenEvent> items
    ) {
        this(orderId, cliente, "cliente@pedidos360.cl", total, estado, items, fecha);
    }

    public record ItemOrdenEvent(
            @JsonAlias({"codigoProducto", "productId"}) String productCode,
            @JsonAlias({"nombreProducto", "nombre"}) String productName,
            @JsonAlias({"cantidad"}) Integer quantity,
            @JsonAlias({"precioUnitario", "precio"}) BigDecimal unitPrice
    ) implements Serializable {}
}

