package com.pedidos360;

import com.pedidos360.domain.Pago;
import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.event.PagoProcesadoEvent;
import com.pedidos360.listener.OrderPaymentListener;
import com.pedidos360.repository.PagoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PagosServiceApplicationTests {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldPublishPaymentProcessedAfterTransactionCommit() {
        PagoRepository repository = mock(PagoRepository.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(repository.findByOrderId(101L)).thenReturn(Optional.empty());
        when(repository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPaymentListener listener = new OrderPaymentListener(repository, rabbitTemplate);
        OrdenCreadaEvent event = new OrdenCreadaEvent(
                101L,
                "Cliente B2B",
                new BigDecimal("500.00"),
                "CONFIRMADO",
                LocalDateTime.now(),
                List.of()
        );

        TransactionSynchronizationManager.initSynchronization();

        listener.procesarPagoOrden(event);

        verify(rabbitTemplate, never()).convertAndSend(any(), any(), any(PagoProcesadoEvent.class));

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        verify(rabbitTemplate, times(1)).convertAndSend(any(), any(), any(PagoProcesadoEvent.class));
    }

    @Test
    void shouldPropagateExceptionWhenPaymentProcessedPublishFails() {
        PagoRepository repository = mock(PagoRepository.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(repository.findByOrderId(102L)).thenReturn(Optional.empty());
        when(repository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new AmqpException("Broker down"))
                .when(rabbitTemplate)
                .convertAndSend(any(), any(), any(PagoProcesadoEvent.class));

        OrderPaymentListener listener = new OrderPaymentListener(repository, rabbitTemplate);
        OrdenCreadaEvent event = new OrdenCreadaEvent(
                102L,
                "Cliente B2B",
                new BigDecimal("500.00"),
                "CONFIRMADO",
                LocalDateTime.now(),
                List.of()
        );

        assertThrows(IllegalStateException.class, () -> listener.procesarPagoOrden(event));
    }
}
