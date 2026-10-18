package com.pedidos360.config;

import com.pedidos360.model.Pedido;
import com.pedidos360.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initPedidos(PedidoRepository pedidoRepository) {
        return args -> {
            if (pedidoRepository.count() == 0) {
                log.info("Inicializando ordenes de ejemplo en CLP...");
                pedidoRepository.saveAll(List.of(
                    new Pedido(LocalDateTime.now().minusDays(2), "Juan Pérez", "ENTREGADO", new BigDecimal("1529980")),
                    new Pedido(LocalDateTime.now().minusDays(1), "María González", "ENVIADO", new BigDecimal("89990")),
                    new Pedido(LocalDateTime.now().minusHours(3), "Carlos Soto", "PAGADO", new BigDecimal("429980")),
                    new Pedido(LocalDateTime.now(), "Ana Morales", "PENDIENTE", new BigDecimal("1499990"))
                ));
                log.info("4 pedidos iniciales en CLP cargados en la base de datos.");
            } else {
                pedidoRepository.findAll().forEach(pedido -> {
                    boolean changed = false;
                    if ("PAGADA".equalsIgnoreCase(pedido.getEstado())) {
                        pedido.setEstado("PAGADO");
                        changed = true;
                    }
                    if (pedido.getTotal() != null && pedido.getTotal().compareTo(new BigDecimal("5000")) < 0) {
                        pedido.setTotal(pedido.getTotal().multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP));
                        changed = true;
                    }
                    if (changed) {
                        pedidoRepository.save(pedido);
                    }
                });
            }
        };
    }
}
