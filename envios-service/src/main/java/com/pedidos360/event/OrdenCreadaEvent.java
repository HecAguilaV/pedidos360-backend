package com.pedidos360.event;

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

    public record ItemOrdenEvent(
        String productCode,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
    ) implements Serializable {}
}
