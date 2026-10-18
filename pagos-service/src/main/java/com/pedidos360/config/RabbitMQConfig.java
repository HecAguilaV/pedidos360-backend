package com.pedidos360.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.orders:orders.exchange}")
    private String ordersExchange;

    @Value("${rabbitmq.exchange.payments-direct:payments.direct.exchange}")
    private String paymentsDirectExchange;

    @Value("${rabbitmq.queue.payments:order-payments}")
    private String paymentsQueue;

    @Value("${rabbitmq.queue.payment-confirmed:order-payment-confirmed}")
    private String paymentConfirmedQueue;

    @Value("${rabbitmq.routing.order-created:orden.creada}")
    private String orderCreatedRoutingKey;

    @Value("${rabbitmq.routing.payment-processed:pago.procesado}")
    private String paymentProcessedRoutingKey;

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ordersExchange, true, false);
    }

    @Bean
    public DirectExchange paymentsDirectExchange() {
        return new DirectExchange(paymentsDirectExchange, true, false);
    }

    @Bean
    public Queue paymentsQueue() {
        return QueueBuilder.durable(paymentsQueue).build();
    }

    @Bean
    public Queue paymentConfirmedQueue() {
        return QueueBuilder.durable(paymentConfirmedQueue).build();
    }

    @Bean
    public Binding paymentsBinding(Queue paymentsQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(paymentsQueue).to(ordersExchange).with(orderCreatedRoutingKey);
    }

    @Bean
    public Binding paymentConfirmedBinding(Queue paymentConfirmedQueue, DirectExchange paymentsDirectExchange) {
        return BindingBuilder.bind(paymentConfirmedQueue).to(paymentsDirectExchange).with(paymentProcessedRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
