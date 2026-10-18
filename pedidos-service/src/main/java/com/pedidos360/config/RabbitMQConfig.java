package com.pedidos360.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDERS_EXCHANGE = "orders.exchange";
    public static final String ROUTING_KEY_ORDER_CREATED = "orden.creada";

    public static final String PAYMENTS_DIRECT_EXCHANGE = "payments.direct.exchange";
    public static final String QUEUE_ORDER_PAYMENT_CONFIRMED = "order-payment-confirmed";
    public static final String ROUTING_KEY_PAYMENT_PROCESSED = "pago.procesado";

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange paymentsDirectExchange() {
        return new DirectExchange(PAYMENTS_DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderPaymentConfirmedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_PAYMENT_CONFIRMED).build();
    }

    @Bean
    public Binding orderPaymentConfirmedBinding(Queue orderPaymentConfirmedQueue, DirectExchange paymentsDirectExchange) {
        return BindingBuilder.bind(orderPaymentConfirmedQueue).to(paymentsDirectExchange).with(ROUTING_KEY_PAYMENT_PROCESSED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
