package com.pedidos360.controller.dto;

import com.pedidos360.event.OrdenCreadaEvent;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record ManualNotificationRequest(
    @NotBlank(message = "El destinatario es obligatorio")
    @Email(message = "El formato de correo es invalido")
    String destinatario,

    String asunto,
    String cliente,
    Long orderId,
    BigDecimal total,
    List<OrdenCreadaEvent.ItemOrdenEvent> items
) {}
