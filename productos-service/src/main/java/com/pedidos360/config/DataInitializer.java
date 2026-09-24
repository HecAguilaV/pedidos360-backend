package com.pedidos360.config;

import com.pedidos360.model.Producto;
import com.pedidos360.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initProductos(ProductoRepository productoRepository) {
        return args -> {
            if (productoRepository.count() == 0) {
                log.info("Inicializando datos de prueba para productos...");
                productoRepository.saveAll(List.of(
                    new Producto("PROD-001", "Laptop Pro 16", "Laptop de alto rendimiento 32GB RAM", new BigDecimal("1499.99"), 15),
                    new Producto("PROD-002", "Mouse Inalámbrico", "Mouse ergonómico con sensor óptico", new BigDecimal("29.99"), 50),
                    new Producto("PROD-003", "Teclado Mecánico", "Teclado mecánico RGB switches red", new BigDecimal("89.99"), 30),
                    new Producto("PROD-004", "Monitor 27 UHD", "Monitor 4K IPS 144Hz", new BigDecimal("399.99"), 20)
                ));
                log.info("4 productos iniciales cargados en la base de datos.");
            }
        };
    }
}
