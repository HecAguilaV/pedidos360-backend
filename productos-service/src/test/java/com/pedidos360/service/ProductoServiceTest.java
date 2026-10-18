package com.pedidos360.service;

import com.pedidos360.model.Producto;
import com.pedidos360.repository.ProductoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductoServiceTest {

    @Test
    void shouldAutoGenerateCanonicalProductCodeWhenBlank() {
        ProductoRepository repository = mock(ProductoRepository.class);
        when(repository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(5L);
            }
            return p;
        });

        ProductoService service = new ProductoService(repository);
        Producto input = new Producto("SSD NVMe 2TB", "PCIe 4.0", new BigDecimal("179.99"), 25);

        Producto created = service.create(input);

        assertEquals(5L, created.getId());
        assertEquals("PROD-005", created.getCodigo());
        verify(repository, times(2)).save(any(Producto.class));
    }

    @Test
    void shouldPreserveExplicitProductCodeWhenProvided() {
        ProductoRepository repository = mock(ProductoRepository.class);
        when(repository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setId(9L);
            return p;
        });

        ProductoService service = new ProductoService(repository);
        Producto input = new Producto("PROD-CUSTOM", "Router WiFi 7", "Tri-band", new BigDecimal("249.99"), 10);

        Producto created = service.create(input);

        assertEquals("PROD-CUSTOM", created.getCodigo());
        verify(repository, times(1)).save(any(Producto.class));
    }

    @Test
    void shouldDeleteOnlyWhenProductExists() {
        ProductoRepository repository = mock(ProductoRepository.class);
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.existsById(99L)).thenReturn(false);

        ProductoService service = new ProductoService(repository);

        assertTrue(service.deleteById(1L));
        assertFalse(service.deleteById(99L));
        verify(repository, times(1)).deleteById(1L);
        verify(repository, never()).deleteById(99L);
    }
}
