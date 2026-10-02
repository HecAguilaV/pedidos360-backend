package com.pedidos360.listener;

import com.pedidos360.domain.Pago;
import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.event.PagoProcesadoEvent;
import com.pedidos360.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class OrderPaymentListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentListener.class);

    private final PagoRepository pagoRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.orders:orders.exchange}")
    private String ordersExchange;

    @Value("${rabbitmq.routing.payment-processed:pago.procesado}")
    private String paymentProcessedRoutingKey;

    public OrderPaymentListener(PagoRepository pagoRepository, RabbitTemplate rabbitTemplate) {
        this.pagoRepository = pagoRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "${rabbitmq.queue.payments:order-payments}")
    @Transactional
    public void procesarPagoOrden(OrdenCreadaEvent event) {
        log.info("[RABBITMQ] Recibido evento 'orden.creada' para cobro de Orden #{} - Cliente: {} - Total: ${}",
                event.orderId(), event.cliente(), event.total());

        // Verificar idempotencia: si ya existe pago para esta orden, no duplicar
        if (pagoRepository.findByOrderId(event.orderId()).isPresent()) {
            log.warn("[PAGOS] La orden #{} ya tiene un registro de pago asociado. Omitiendo duplicado.", event.orderId());
            return;
        }

        String transaccionId = "TX-PAY-" + event.orderId() + "-" + (System.currentTimeMillis() % 1000000);
        String codigoAuth = "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Pago pago = new Pago(
                event.orderId(),
                transaccionId,
                event.cliente(),
                event.total(),
                "LINEA_CREDITO_B2B",
                "APROBADO",
                codigoAuth,
                LocalDateTime.now()
        );

        Pago pagoGuardado = pagoRepository.save(pago);
        log.info("[PAGOS] Pago liquidado y registrado exitosamente: ID #{} - Transacción: {} - Auth: {}",
                pagoGuardado.getId(), transaccionId, codigoAuth);

        // Emitir evento de dominio: pago.procesado
        try {
            PagoProcesadoEvent pagoEvent = new PagoProcesadoEvent(
                    pagoGuardado.getId(),
                    pagoGuardado.getOrderId(),
                    pagoGuardado.getTransaccionId(),
                    pagoGuardado.getCliente(),
                    pagoGuardado.getMonto(),
                    pagoGuardado.getEstado(),
                    pagoGuardado.getCodigoAutorizacion(),
                    pagoGuardado.getFechaPago()
            );

            rabbitTemplate.convertAndSend(ordersExchange, paymentProcessedRoutingKey, pagoEvent);
            log.info("[RABBITMQ] Publicado evento '{}' en exchange '{}' para Orden #{}",
                    paymentProcessedRoutingKey, ordersExchange, event.orderId());
        } catch (Exception e) {
            log.error("[RABBITMQ] Error al emitir evento pago.procesado: {}", e.getMessage());
        }
    }
}
