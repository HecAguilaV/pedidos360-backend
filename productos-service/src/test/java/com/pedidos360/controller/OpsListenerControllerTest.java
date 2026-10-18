package com.pedidos360.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpsListenerControllerTest {

    private RabbitListenerEndpointRegistry registry;
    private MessageListenerContainer container;
    private OpsListenerController controller;

    @BeforeEach
    void setUp() {
        registry = mock(RabbitListenerEndpointRegistry.class);
        container = mock(MessageListenerContainer.class);
        controller = new OpsListenerController(registry);
    }

    @Test
    void shouldReturnRunningStatusWhenContainersAreActive() {
        when(container.isRunning()).thenReturn(true);
        when(registry.getListenerContainers()).thenReturn(List.of(container));

        ResponseEntity<Map<String, Object>> response = controller.getStatus();

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("running"));
        assertEquals(1, body.get("containerCount"));
    }

    @Test
    void shouldStopRegistryAndReturnPausedStatus() {
        ResponseEntity<Map<String, Object>> response = controller.stop();

        verify(registry, times(1)).stop();
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("PAUSED", body.get("status"));
        assertEquals(false, body.get("running"));
    }

    @Test
    void shouldStartRegistryAndReturnRunningStatus() {
        ResponseEntity<Map<String, Object>> response = controller.start();

        verify(registry, times(1)).start();
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("RUNNING", body.get("status"));
        assertEquals(true, body.get("running"));
    }
}
