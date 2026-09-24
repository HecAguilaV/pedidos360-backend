package com.pedidos360.controller;

import com.pedidos360.controller.dto.ManualNotificationRequest;
import com.pedidos360.controller.dto.NotificationResponse;
import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificationController {

    private final EmailService emailService;

    public NotificationController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/enviar")
    public ResponseEntity<NotificationResponse> enviarNotificacionDirecta(
            @Valid @RequestBody ManualNotificationRequest request) {

        String asunto = (request.asunto() != null && !request.asunto().isBlank())
                ? request.asunto()
                : "Comprobante de Compra Pedidos360 #" + (request.orderId() != null ? request.orderId() : 1001L);

        String cliente = (request.cliente() != null && !request.cliente().isBlank())
                ? request.cliente()
                : "Cliente Estimado";

        BigDecimal total = (request.total() != null) ? request.total() : new BigDecimal("1499.99");
        List<OrdenCreadaEvent.ItemOrdenEvent> items = (request.items() != null && !request.items().isEmpty())
                ? request.items()
                : List.of(new OrdenCreadaEvent.ItemOrdenEvent("PROD-001", "Laptop Pro 16", 1, total));

        boolean exito = emailService.sendDirectEmail(
                request.destinatario(),
                asunto,
                cliente,
                request.orderId() != null ? request.orderId() : 1001L,
                total,
                items
        );

        String mensaje = exito
                ? "Notificacion enviada correctamente a " + request.destinatario()
                : "Fallo al enviar la notificacion";

        return ResponseEntity.ok(new NotificationResponse(
                exito,
                mensaje,
                request.destinatario(),
                asunto,
                LocalDateTime.now()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "servicio", "notifications-service",
                "estado", "OPERATIVO",
                "colaRabbitMQ", "order-notifications",
                "exchange", "orders.exchange",
                "timestamp", LocalDateTime.now()
        ));
    }
}
