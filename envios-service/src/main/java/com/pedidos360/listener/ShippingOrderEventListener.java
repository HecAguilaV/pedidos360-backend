package com.pedidos360.listener;

import com.pedidos360.config.RabbitMQConfig;
import com.pedidos360.domain.Envio;
import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.repository.EnvioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ShippingOrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(ShippingOrderEventListener.class);
    private final EnvioRepository envioRepository;

    public ShippingOrderEventListener(EnvioRepository envioRepository) {
        this.envioRepository = envioRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.SHIPPING_QUEUE)
    @Transactional
    public void onOrderCreated(OrdenCreadaEvent event) {
        log.info("==> [RabbitMQ Listener] Evento 'orden.creada' recibido en 'shipping-dispatch'. Orden ID: {}", event.orderId());

        if (envioRepository.findByOrderId(event.orderId()).isPresent()) {
            log.info("La orden ID: {} ya cuenta con guia de despacho generada previamente.", event.orderId());
            return;
        }

        String trackingNumber = "TRACK-" + String.format("%05d", event.orderId()) + "-" + (System.currentTimeMillis() % 1000);
        LocalDateTime despacho = LocalDateTime.now();
        LocalDateTime entregaEstimada = despacho.plusDays(2);

        Envio envio = new Envio(
                event.orderId(),
                trackingNumber,
                event.cliente(),
                event.clienteEmail(),
                "Av. Providencia 1234, Santiago, Chile",
                "PREPARANDO_PAQUETE",
                "Pedidos360 Express",
                despacho,
                entregaEstimada
        );

        Envio guardado = envioRepository.save(envio);

        log.info("GUIA DE DESPACHO GENERADA EXITOSAMENTE [Orden #{}]: Guia = '{}', Destinatario = '{}', Estado = '{}', Entrega Estimada = {}",
                guardado.getOrderId(), guardado.getNumeroGuia(), guardado.getDestinatario(), guardado.getEstado(), guardado.getFechaEstimadaEntrega());
    }
}
