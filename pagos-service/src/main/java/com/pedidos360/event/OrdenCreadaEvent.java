package com.pedidos360.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenCreadaEvent(
        Long orderId,
        String cliente,
        BigDecimal total,
        String estado,
        LocalDateTime fecha,
        List<ItemOrdenEvent> items
) implements Serializable {

    public record ItemOrdenEvent(
            String codigoProducto,
            String nombreProducto,
            Integer cantidad,
            BigDecimal precioUnitario
    ) implements Serializable {}
}
