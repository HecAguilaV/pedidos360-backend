package com.pedidos360.config;

import com.pedidos360.model.Pedido;
import com.pedidos360.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initPedidos(PedidoRepository pedidoRepository) {
        return args -> {
            if (pedidoRepository.count() == 0) {
                log.info("Inicializando datos de prueba para pedidos...");
                pedidoRepository.saveAll(List.of(
                    new Pedido(LocalDateTime.now().minusDays(2), "Juan Pérez", "ENTREGADO", new BigDecimal("1529.98")),
                    new Pedido(LocalDateTime.now().minusDays(1), "María González", "ENVIADO", new BigDecimal("89.99")),
                    new Pedido(LocalDateTime.now().minusHours(3), "Carlos Soto", "CONFIRMADO", new BigDecimal("429.98")),
                    new Pedido(LocalDateTime.now(), "Ana Morales", "PENDIENTE", new BigDecimal("1499.99"))
                ));
                log.info("4 pedidos iniciales cargados en la base de datos.");
            }
        };
    }
}
