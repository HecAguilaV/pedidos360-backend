package com.pedidos360.controller;

import com.pedidos360.controller.dto.ManualNotificationRequest;
import com.pedidos360.controller.dto.NotificationResponse;
import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class NotificationController {

    private final EmailService emailService;

    public NotificationController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/api/v1/notificaciones/enviar")
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

        String destinatario = emailService.sanitizeParam(request.destinatario());
        boolean exito = emailService.sendDirectEmail(
                destinatario,
                asunto,
                cliente,
                request.orderId() != null ? request.orderId() : 1001L,
                total,
                items
        );

        String mensaje = exito
                ? "Notificacion enviada correctamente a " + destinatario
                : "Fallo al enviar la notificacion";

        HttpStatus status = exito ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(new NotificationResponse(
                exito,
                mensaje,
                destinatario,
                asunto,
                LocalDateTime.now()
        ));
    }

    @PostMapping("/api/email/enviar")
    public ResponseEntity<NotificationResponse> enviarEmailPorFormulario(
            @RequestParam("to") String to,
            @RequestParam(value = "subject", defaultValue = "Notificación Pedidos360") String subject,
            @RequestParam(value = "body", defaultValue = "") String body) {

        String destinatario = emailService.sanitizeParam(to);
        String asuntoLimpio = emailService.sanitizeParam(subject);
        String cuerpoLimpio = emailService.sanitizeParam(body);

        if (destinatario.isBlank()) {
            return ResponseEntity.badRequest().body(new NotificationResponse(
                    false,
                    "El parametro 'to' es obligatorio",
                    "",
                    asuntoLimpio,
                    LocalDateTime.now()
            ));
        }

        boolean exito = emailService.sendCustomEmail(destinatario, asuntoLimpio, cuerpoLimpio);
        String mensaje = exito
                ? "¡Correo enviado exitosamente a " + destinatario + "!"
                : "Fallo al enviar el correo a " + destinatario;

        HttpStatus status = exito ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(new NotificationResponse(
                exito,
                mensaje,
                destinatario,
                asuntoLimpio,
                LocalDateTime.now()
        ));
    }

    @GetMapping("/api/v1/notificaciones/status")
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
