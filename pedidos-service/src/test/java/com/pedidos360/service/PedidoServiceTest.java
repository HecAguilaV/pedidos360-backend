package com.pedidos360.service;

import com.pedidos360.config.RabbitMQConfig;
import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.model.Pedido;
import com.pedidos360.model.PedidoItem;
import com.pedidos360.repository.PedidoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        Pedido incoming = new Pedido("Empresa Demo", "CONFIRMADO", new BigDecimal("859.98"));
        incoming.setItems(List.of(
                new PedidoItem("PROD-002", "Monitor 4K UltraWide", 2, new BigDecimal("429.99"))
        ));
        when(repository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(42L);
            return p;
        });

        TransactionSynchronizationManager.initSynchronization();

        service.create(incoming);

        verify(publisher, never()).publishOrderCreated(any());

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        ArgumentCaptor<OrdenCreadaEvent> captor = ArgumentCaptor.forClass(OrdenCreadaEvent.class);
        verify(publisher, times(1)).publishOrderCreated(captor.capture());
        OrdenCreadaEvent published = captor.getValue();
        assertEquals(1, published.items().size());
        assertEquals("PROD-002", published.items().get(0).productCode());
        assertEquals("Monitor 4K UltraWide", published.items().get(0).productName());
        assertEquals(2, published.items().get(0).quantity());
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

