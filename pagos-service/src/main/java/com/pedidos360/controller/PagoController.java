package com.pedidos360.controller;

import com.pedidos360.controller.dto.ProcesarPagoRequest;
import com.pedidos360.domain.Pago;
import com.pedidos360.event.PagoProcesadoEvent;
import com.pedidos360.repository.PagoRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController {

    private static final Logger log = LoggerFactory.getLogger(PagoController.class);

    private final PagoRepository pagoRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.orders:orders.exchange}")
    private String ordersExchange = "orders.exchange";

    @Value("${rabbitmq.exchange.payments-direct:payments.direct.exchange}")
    private String paymentsDirectExchange = "payments.direct.exchange";

    @Value("${rabbitmq.routing.payment-processed:pago.procesado}")
    private String paymentProcessedRoutingKey = "pago.procesado";

    public PagoController(PagoRepository pagoRepository, RabbitTemplate rabbitTemplate) {
        this.pagoRepository = pagoRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping
    public ResponseEntity<List<Pago>> listarPagos() {
        return ResponseEntity.ok(pagoRepository.findAllByOrderByFechaPagoDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pago> obtenerPagoPorId(@PathVariable Long id) {
        return pagoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/orden/{orderId}")
    public ResponseEntity<Pago> obtenerPagoPorOrderId(@PathVariable Long orderId) {
        return pagoRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/procesar")
    @Transactional
    public ResponseEntity<Pago> procesarPagoManual(@Valid @RequestBody ProcesarPagoRequest request) {
        var existente = pagoRepository.findByOrderId(request.orderId());
        if (existente.isPresent()) {
            log.info("[PAGOS] Solicitud manual idempotente: la orden #{} ya posee pago registrado.", request.orderId());
            return ResponseEntity.ok(existente.get());
        }

        String metodo = (request.metodoPago() != null && !request.metodoPago().isBlank())
                ? request.metodoPago()
                : "LINEA_CREDITO_B2B";

        String transaccionId = "TX-PAY-" + request.orderId() + "-" + (System.currentTimeMillis() % 1000000);
        String codigoAuth = "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Pago pago = new Pago(
                request.orderId(),
                transaccionId,
                request.cliente(),
                request.monto(),
                metodo,
                "APROBADO",
                codigoAuth,
                LocalDateTime.now()
        );

        Pago pagoGuardado = pagoRepository.save(pago);

        try {
            PagoProcesadoEvent event = new PagoProcesadoEvent(
                    pagoGuardado.getId(),
                    pagoGuardado.getOrderId(),
                    pagoGuardado.getTransaccionId(),
                    pagoGuardado.getCliente(),
                    pagoGuardado.getMonto(),
                    pagoGuardado.getEstado(),
                    pagoGuardado.getCodigoAutorizacion(),
                    pagoGuardado.getFechaPago()
            );
            rabbitTemplate.convertAndSend(paymentsDirectExchange, paymentProcessedRoutingKey, event);
            log.info("[RABBITMQ P2P] Publicado evento '{}' en DirectExchange '{}' para Orden #{}",
                    paymentProcessedRoutingKey, paymentsDirectExchange, pagoGuardado.getOrderId());
        } catch (Exception e) {
            log.error("[RABBITMQ P2P] Error al publicar evento '{}' para Orden #{}: {}",
                    paymentProcessedRoutingKey, pagoGuardado.getOrderId(), e.getMessage(), e);
            throw new IllegalStateException("Failed to publish pago.procesado event for order ID " + pagoGuardado.getOrderId(), e);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(pagoGuardado);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "servicio", "pagos-service",
                "puerto", 8086,
                "estado", "OPERATIVO",
                "colaRabbitMQ", "order-payments",
                "exchange", ordersExchange,
                "exchangeP2P", paymentsDirectExchange,
                "colaP2P", "order-payment-confirmed",
                "timestamp", LocalDateTime.now()
        ));
    }
}
