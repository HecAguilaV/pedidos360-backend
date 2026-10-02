package com.pedidos360.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProcesarPagoRequest(
        @NotNull(message = "El ID de orden es obligatorio")
        Long orderId,

        @NotBlank(message = "El cliente es obligatorio")
        String cliente,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser positivo")
        BigDecimal monto,

        String metodoPago
) {}
