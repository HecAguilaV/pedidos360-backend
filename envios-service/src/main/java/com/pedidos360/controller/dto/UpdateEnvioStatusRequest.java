package com.pedidos360.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateEnvioStatusRequest(
    @NotBlank(message = "El nuevo estado es obligatorio")
    String estado
) {}
