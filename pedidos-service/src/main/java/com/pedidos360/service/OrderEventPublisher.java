package com.pedidos360.service;

import com.pedidos360.config.RabbitMQConfig;
import com.pedidos360.event.OrdenCreadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(OrdenCreadaEvent event) {
        try {
            log.info("Publicando evento 'orden.creada' para orden ID: {} a exchange: {}", event.orderId(), RabbitMQConfig.ORDERS_EXCHANGE);
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDERS_EXCHANGE, RabbitMQConfig.ROUTING_KEY_ORDER_CREATED, event);
            log.info("Evento 'orden.creada' publicado exitosamente para orden ID: {}", event.orderId());
        } catch (Exception e) {
            log.error("Error al publicar evento 'orden.creada' a RabbitMQ para orden ID: {}: {}", event.orderId(), e.getMessage());
        }
    }
}
