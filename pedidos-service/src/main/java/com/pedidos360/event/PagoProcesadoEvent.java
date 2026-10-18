package com.pedidos360.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PagoProcesadoEvent(
        Long pagoId,
        Long orderId,
        String transaccionId,
        String cliente,
        BigDecimal monto,
        String estado,
        String codigoAutorizacion,
        LocalDateTime fechaPago
) implements Serializable {
}
