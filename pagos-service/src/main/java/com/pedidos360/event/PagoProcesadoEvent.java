package com.pedidos360.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoProcesadoEvent(
        Long pagoId,
        Long orderId,
        String transaccionId,
        String cliente,
        BigDecimal monto,
        String estado,
        String codigoAutorizacion,
        LocalDateTime fechaPago
) implements Serializable {}
