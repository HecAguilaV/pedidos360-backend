package com.pedidos360;

import com.pedidos360.event.OrdenCreadaEvent;
import com.pedidos360.listener.CartOrderEventListener;
import com.pedidos360.repository.CartItemRepository;
import com.pedidos360.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CarritoServiceApplicationTests {

    @Test
    void shouldClearCartByCustomerEmailAndDefaultUserWhenOrderCreatedEventArrives() {
        CartItemRepository repository = mock(CartItemRepository.class);
        CartOrderEventListener listener = new CartOrderEventListener(repository);

        OrdenCreadaEvent event = new OrdenCreadaEvent(
                88L,
                "Cliente Cloud",
                "cliente@pedidos360.cl",
                new BigDecimal("499.99"),
                "PAGADA",
                List.of(),
                LocalDateTime.now()
        );

        listener.onOrderCreated(event);

        verify(repository, times(1)).deleteByUserId("cliente@pedidos360.cl");
        verify(repository, times(1)).deleteByUserId("Cliente Cloud");
        verify(repository, times(1)).deleteByUserId("default-user");
    }

    @Test
    void shouldResolvePreferredUsernameFromJwtInCurrentUserService() {
        CurrentUserService service = new CurrentUserService();
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("preferred_username", "admin@pedidos360.cl", "oid", "uuid-123")
        );
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

        assertEquals("admin@pedidos360.cl", service.resolveUserId(auth, null));
    }
}
