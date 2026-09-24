package com.pedidos360.controller.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
    boolean exito,
    String mensaje,
    String destinatario,
    String asunto,
    LocalDateTime fechaEnvio
) {}
