package com.pedidos360.service;

import com.pedidos360.config.RabbitMQConfig;
import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.model.Pedido;
import com.pedidos360.repository.PedidoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PedidoServiceTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldDeferOrderCreatedEventUntilAfterCommitWhenTransactionIsActive() {
        PedidoRepository repository = mock(PedidoRepository.class);
        OrderEventPublisher publisher = mock(OrderEventPublisher.class);
        PedidoService service = new PedidoService(repository, publisher);

        Pedido incoming = new Pedido("Empresa Demo", "CONFIRMADO", new BigDecimal("250.00"));
        Pedido saved = new Pedido("Empresa Demo", "CONFIRMADO", new BigDecimal("250.00"));
        saved.setId(42L);
        when(repository.save(any(Pedido.class))).thenReturn(saved);

        TransactionSynchronizationManager.initSynchronization();

        service.create(incoming);

        verify(publisher, never()).publishOrderCreated(any());

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        verify(publisher, times(1)).publishOrderCreated(any(OrdenCreadaEvent.class));
    }

    @Test
    void shouldPropagateExceptionWhenRabbitMqPublishFails() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        doThrow(new AmqpException("Broker unreachable"))
                .when(rabbitTemplate)
                .convertAndSend(eq(RabbitMQConfig.ORDERS_EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY_ORDER_CREATED), any(OrdenCreadaEvent.class));

        OrderEventPublisher publisher = new OrderEventPublisher(rabbitTemplate);
        OrdenCreadaEvent event = new OrdenCreadaEvent(
                99L,
                "Cliente Test",
                "test@pedidos360.cl",
                new BigDecimal("100.00"),
                "CONFIRMADO",
                List.of(),
                LocalDateTime.now()
        );

        assertThrows(IllegalStateException.class, () -> publisher.publishOrderCreated(event));
    }

    @Test
    void shouldNotThrowInAfterCommitWhenPublishFailsSoCommittedOrderDoesNotReturn500() {
        PedidoRepository repository = mock(PedidoRepository.class);
        OrderEventPublisher publisher = mock(OrderEventPublisher.class);
        doThrow(new IllegalStateException("RabbitMQ temporarily unavailable"))
                .when(publisher).publishOrderCreated(any(OrdenCreadaEvent.class));
        PedidoService service = new PedidoService(repository, publisher);

        Pedido incoming = new Pedido("Empresa Demo", "CONFIRMADO", new BigDecimal("250.00"));
        Pedido saved = new Pedido("Empresa Demo", "CONFIRMADO", new BigDecimal("250.00"));
        saved.setId(43L);
        when(repository.save(any(Pedido.class))).thenReturn(saved);

        TransactionSynchronizationManager.initSynchronization();

        service.create(incoming);

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        verify(publisher, times(1)).publishOrderCreated(any(OrdenCreadaEvent.class));
    }
}

