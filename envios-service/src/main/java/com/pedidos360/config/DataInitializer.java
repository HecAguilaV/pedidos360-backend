package com.pedidos360.config;

import com.pedidos360.domain.Envio;
import com.pedidos360.repository.EnvioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initEnvios(EnvioRepository envioRepository) {
        return args -> {
            if (envioRepository.count() == 0) {
                log.info("Inicializando envíos de prueba para el panel de Despachador...");
                LocalDateTime now = LocalDateTime.now();
                envioRepository.saveAll(List.of(
                    new Envio(101L, "TRACK-2026-00101", "DataCenter Tech Chile SPA", "contacto@techchile.cl", "Av. Providencia 1234, Santiago", "PREPARANDO_PAQUETE", "Chilexpress", now, now.plusDays(2)),
                    new Envio(102L, "TRACK-2026-00102", "Héctor Águila", "he.aguila@duocuc.cl", "Av. Vitacura 5000, Las Condes", "PEDIDO_CONFIRMADO", "Starken", now, now.plusDays(3)),
                    new Envio(103L, "TRACK-2026-00103", "Soluciones Cloud Austral", "infra@cloudaustral.cl", "Camino El Alba 880, Las Condes", "EN_TRANSITO", "Blue Express", now.minusDays(1), now.plusDays(1))
                ));
                log.info("3 envíos iniciales cargados en la base de datos.");
            } else {
                envioRepository.findAll().forEach(envio -> {
                    if (envio.getNumeroGuia() != null && !envio.getNumeroGuia().startsWith("TRACK-2026-") && envio.getOrderId() != null) {
                        envio.setNumeroGuia(String.format("TRACK-2026-%05d", envio.getOrderId()));
                        envioRepository.save(envio);
                    }
                });
            }
        };
    }
}

