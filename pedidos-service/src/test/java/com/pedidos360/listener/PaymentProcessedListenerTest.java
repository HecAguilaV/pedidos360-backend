package com.pedidos360.listener;

import com.pedidos360.event.PagoProcesadoEvent;
import com.pedidos360.model.Pedido;
import com.pedidos360.repository.PedidoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PaymentProcessedListenerTest {

    @Test
    void shouldUpdateOrderStatusToPagadoWhenPaymentIsApprovedViaP2P() {
        PedidoRepository repository = mock(PedidoRepository.class);
        Pedido pedido = new Pedido("Empresa Demo", "CONFIRMADO", new BigDecimal("500.00"));
        pedido.setId(77L);
        when(repository.findById(77L)).thenReturn(Optional.of(pedido));

        PaymentProcessedListener listener = new PaymentProcessedListener(repository);
        PagoProcesadoEvent event = new PagoProcesadoEvent(
                10L,
                77L,
                "TX-PAY-77-123456",
                "Empresa Demo",
                new BigDecimal("500.00"),
                "APROBADO",
                "AUTH-1234ABCD",
                LocalDateTime.now()
        );

        listener.handlePaymentProcessed(event);

        assertEquals("PAGADO", pedido.getEstado());
        verify(repository, times(1)).save(pedido);
    }

    @Test
    void shouldSkipSaveWhenOrderIsAlreadyPagado() {
        PedidoRepository repository = mock(PedidoRepository.class);
        Pedido pedido = new Pedido("Empresa Demo", "PAGADO", new BigDecimal("500.00"));
        pedido.setId(78L);
        when(repository.findById(78L)).thenReturn(Optional.of(pedido));

        PaymentProcessedListener listener = new PaymentProcessedListener(repository);
        PagoProcesadoEvent event = new PagoProcesadoEvent(
                11L,
                78L,
                "TX-PAY-78-123456",
                "Empresa Demo",
                new BigDecimal("500.00"),
                "APROBADO",
                "AUTH-5678EFGH",
                LocalDateTime.now()
        );

        listener.handlePaymentProcessed(event);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldIgnoreWhenOrderDoesNotExist() {
        PedidoRepository repository = mock(PedidoRepository.class);
        when(repository.findById(999L)).thenReturn(Optional.empty());

        PaymentProcessedListener listener = new PaymentProcessedListener(repository);
        PagoProcesadoEvent event = new PagoProcesadoEvent(
                12L,
                999L,
                "TX-PAY-999-123456",
                "Empresa Inexistente",
                new BigDecimal("100.00"),
                "APROBADO",
                "AUTH-0000",
                LocalDateTime.now()
        );

        listener.handlePaymentProcessed(event);

        verify(repository, never()).save(any());
    }
}
