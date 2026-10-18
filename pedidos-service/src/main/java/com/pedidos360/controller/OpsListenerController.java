package com.pedidos360.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/pedidos/ops/listener")
public class OpsListenerController {

    private static final Logger log = LoggerFactory.getLogger(OpsListenerController.class);

    private final RabbitListenerEndpointRegistry registry;

    @Value("${spring.application.name:pedidos-service}")
    private String serviceName;

    public OpsListenerController(RabbitListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean anyRunning = registry.getListenerContainers().stream().anyMatch(MessageListenerContainer::isRunning);
        List<Map<String, Object>> containers = registry.getListenerContainers().stream().map(c -> {
            Map<String, Object> info = new HashMap<>();
            info.put("running", c.isRunning());
            return info;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("service", serviceName);
        response.put("running", anyRunning);
        response.put("containerCount", registry.getListenerContainers().size());
        response.put("containers", containers);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        log.info("Pausando listeners AMQP en servicio: {}", serviceName);
        registry.stop();
        Map<String, Object> response = new HashMap<>();
        response.put("service", serviceName);
        response.put("status", "PAUSED");
        response.put("running", false);
        response.put("message", "Consumidor AMQP pausado exitosamente");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        log.info("Reanudando listeners AMQP en servicio: {}", serviceName);
        registry.start();
        Map<String, Object> response = new HashMap<>();
        response.put("service", serviceName);
        response.put("status", "RUNNING");
        response.put("running", true);
        response.put("message", "Consumidor AMQP reanudado exitosamente");
        return ResponseEntity.ok(response);
    }
}
