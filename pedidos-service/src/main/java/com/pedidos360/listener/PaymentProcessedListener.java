package com.pedidos360.listener;

import com.pedidos360.config.RabbitMQConfig;
import com.pedidos360.event.PagoProcesadoEvent;
import com.pedidos360.model.Pedido;
import com.pedidos360.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentProcessedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessedListener.class);

    private final PedidoRepository pedidoRepository;

    public PaymentProcessedListener(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_PAYMENT_CONFIRMED)
    @Transactional
    public void handlePaymentProcessed(PagoProcesadoEvent event) {
        if (event == null || event.orderId() == null) {
            log.warn("[RABBITMQ P2P] Evento 'pago.procesado' recibido sin orderId válido. Ignorando.");
            return;
        }

        log.info("[RABBITMQ P2P] Recibido evento punto-a-punto 'pago.procesado' desde '{}' en cola '{}' para Orden #{} (Estado pago: {}, Tx: {})",
                RabbitMQConfig.PAYMENTS_DIRECT_EXCHANGE,
                RabbitMQConfig.QUEUE_ORDER_PAYMENT_CONFIRMED,
                event.orderId(),
                event.estado(),
                event.transaccionId());

        Pedido pedido = pedidoRepository.findById(event.orderId()).orElse(null);
        if (pedido == null) {
            log.warn("[RABBITMQ P2P] Pedido #{} no encontrado al procesar confirmación de pago. Omitiendo.", event.orderId());
            return;
        }

        String nuevoEstado = "APROBADO".equalsIgnoreCase(event.estado()) ? "PAGADO" : "PAGO_RECHAZADO";
        if (nuevoEstado.equalsIgnoreCase(pedido.getEstado())) {
            log.info("[RABBITMQ P2P] Pedido #{} ya se encuentra en estado {}. Omitiendo duplicado.",
                    pedido.getId(), nuevoEstado);
            return;
        }

        pedido.setEstado(nuevoEstado);
        pedidoRepository.save(pedido);

        log.info("[RABBITMQ P2P] Pedido #{} actualizado exitosamente a estado '{}' tras confirmación P2P (Auth: {})",
                pedido.getId(), nuevoEstado, event.codigoAutorizacion());
    }
}
