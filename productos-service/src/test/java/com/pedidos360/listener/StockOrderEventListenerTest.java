package com.pedidos360.listener;

import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.model.ProcessedOrderStockEvent;
import com.pedidos360.model.Producto;
import com.pedidos360.repository.ProcessedOrderStockEventRepository;
import com.pedidos360.repository.ProductoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StockOrderEventListenerTest {

    @Test
    void shouldDecrementStockAndRecordProcessedOrderOnFirstDelivery() {
        ProductoRepository productoRepository = mock(ProductoRepository.class);
        ProcessedOrderStockEventRepository processedOrderRepository = mock(ProcessedOrderStockEventRepository.class);

        Producto producto = new Producto("PROD-002", "Monitor 4K", "Monitor", new BigDecimal("429.99"), 10);
        when(processedOrderRepository.existsByOrderId(500L)).thenReturn(false);
        when(productoRepository.findByCodigo("PROD-002")).thenReturn(Optional.of(producto));

        StockOrderEventListener listener = new StockOrderEventListener(productoRepository, processedOrderRepository);
        OrdenCreadaEvent event = new OrdenCreadaEvent(
                500L,
                "Cliente Demo",
                "demo@pedidos360.cl",
                new BigDecimal("859.98"),
                "CONFIRMADO",
                List.of(new OrdenCreadaEvent.ItemOrdenEvent("PROD-002", "Monitor 4K", 2, new BigDecimal("429.99"))),
                LocalDateTime.now()
        );

        listener.onOrderCreated(event);

        assertEquals(8, producto.getStock());
        verify(productoRepository, times(1)).save(producto);
        verify(processedOrderRepository, times(1)).save(any(ProcessedOrderStockEvent.class));
    }

    @Test
    void shouldIgnoreDuplicateOrderCreatedEventAndNotDecrementStockTwice() {
        ProductoRepository productoRepository = mock(ProductoRepository.class);
        ProcessedOrderStockEventRepository processedOrderRepository = mock(ProcessedOrderStockEventRepository.class);

        when(processedOrderRepository.existsByOrderId(500L)).thenReturn(true);

        StockOrderEventListener listener = new StockOrderEventListener(productoRepository, processedOrderRepository);
        OrdenCreadaEvent event = new OrdenCreadaEvent(
                500L,
                "Cliente Demo",
                "demo@pedidos360.cl",
                new BigDecimal("859.98"),
                "CONFIRMADO",
                List.of(new OrdenCreadaEvent.ItemOrdenEvent("PROD-002", "Monitor 4K", 2, new BigDecimal("429.99"))),
                LocalDateTime.now()
        );

        listener.onOrderCreated(event);

        verify(productoRepository, never()).save(any());
        verify(processedOrderRepository, never()).save(any());
    }
}
