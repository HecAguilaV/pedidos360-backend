package com.pedidos360.listener;

import com.pedidos360.config.RabbitMQConfig;
import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.repository.CartItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class CartOrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(CartOrderEventListener.class);
    private final CartItemRepository cartItemRepository;

    public CartOrderEventListener(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.CART_CLEAR_QUEUE)
    @Transactional
    public void onOrderCreated(OrdenCreadaEvent event) {
        log.info("==> [RabbitMQ Consumer] Evento 'orden.creada' recibido en 'cart-clear'. Orden ID: {}, ClienteEmail: {}",
                event.orderId(), event.clienteEmail());

        if (StringUtils.hasText(event.clienteEmail())) {
            cartItemRepository.deleteByUserId(event.clienteEmail().trim());
        }
        if (StringUtils.hasText(event.cliente())) {
            cartItemRepository.deleteByUserId(event.cliente().trim());
        }
        cartItemRepository.deleteByUserId("default-user");

        log.info("CARRITO VACIADO AUTOMATICAMENTE POR EVENTO [Orden #{}]: cliente='{}', email='{}'",
                event.orderId(), event.cliente(), event.clienteEmail());
    }
}
