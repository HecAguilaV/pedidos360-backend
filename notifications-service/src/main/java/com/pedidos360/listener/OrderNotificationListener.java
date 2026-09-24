package com.pedidos360.listener;

import com.pedidos360.config.RabbitMQConfig;
import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationListener.class);
    private final EmailService emailService;

    public OrderNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE)
    public void onOrderCreated(OrdenCreadaEvent event) {
        log.info("==> [RabbitMQ Listener] Mensaje de orden recibido en 'order-notifications'. Orden ID: {}, Cliente: {}, Total: {}",
                event.orderId(), event.cliente(), event.total());

        boolean sent = emailService.sendOrderReceiptEmail(event);
        if (sent) {
            log.info("Notificacion por correo procesada exitosamente para la orden ID: {}", event.orderId());
        } else {
            log.warn("No se pudo completar el envio de correo para la orden ID: {}", event.orderId());
        }
    }
}
