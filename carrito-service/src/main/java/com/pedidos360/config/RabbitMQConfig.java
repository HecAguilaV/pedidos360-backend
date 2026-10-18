package com.pedidos360.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDERS_EXCHANGE = "orders.exchange";
    public static final String CART_CLEAR_QUEUE = "cart-clear";
    public static final String ROUTING_KEY_PATTERN = "orden.*";

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE, true, false);
    }

    @Bean
    public Queue cartClearQueue() {
        return new Queue(CART_CLEAR_QUEUE, true);
    }

    @Bean
    public Binding cartClearBinding(Queue cartClearQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(cartClearQueue).to(ordersExchange).with(ROUTING_KEY_PATTERN);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
