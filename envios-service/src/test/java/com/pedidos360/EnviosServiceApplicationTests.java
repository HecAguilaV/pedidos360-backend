package com.pedidos360;

import com.pedidos360.domain.Envio;
import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.listener.ShippingOrderEventListener;
import com.pedidos360.repository.EnvioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EnviosServiceApplicationTests {

    @Test
    void shouldGenerateCanonicalTrackingNumberInTrack2026Format() {
        EnvioRepository repository = mock(EnvioRepository.class);
        when(repository.findByOrderId(7L)).thenReturn(Optional.empty());
        when(repository.save(any(Envio.class))).thenAnswer(inv -> inv.getArgument(0));

        ShippingOrderEventListener listener = new ShippingOrderEventListener(repository);
        OrdenCreadaEvent event = new OrdenCreadaEvent(
                7L,
                "Empresa Logistica",
                "logistica@pedidos360.cl",
                new BigDecimal("1200.00"),
                "CONFIRMADO",
                List.of(),
                LocalDateTime.now()
        );

        listener.onOrderCreated(event);

        ArgumentCaptor<Envio> captor = ArgumentCaptor.forClass(Envio.class);
        verify(repository, times(1)).save(captor.capture());
        assertEquals("TRACK-2026-00007", captor.getValue().getNumeroGuia());
        assertEquals("PREPARANDO_PAQUETE", captor.getValue().getEstado());
    }

    @Test
    void shouldSkipDuplicateShippingOrderEvent() {
        EnvioRepository repository = mock(EnvioRepository.class);
        Envio existing = new Envio();
        existing.setOrderId(7L);
        existing.setNumeroGuia("TRACK-2026-00007");
        when(repository.findByOrderId(7L)).thenReturn(Optional.of(existing));

        ShippingOrderEventListener listener = new ShippingOrderEventListener(repository);
        OrdenCreadaEvent event = new OrdenCreadaEvent(
                7L,
                "Empresa Logistica",
                "logistica@pedidos360.cl",
                new BigDecimal("1200.00"),
                "CONFIRMADO",
                List.of(),
                LocalDateTime.now()
        );

        listener.onOrderCreated(event);

        verify(repository, never()).save(any());
    }
}
